-- Pickle database bootstrap, reconciled from the deployed schema.
-- For a NEW Supabase database only. Existing deployments use supabase/migrations.
-- auth.users and Supabase roles must already exist.
begin;
set local search_path = public;

create table public.polls (
id uuid not null default gen_random_uuid(),
question text not null,
creator_id uuid not null,
allow_suggestions boolean not null default false,
require_approval boolean not null default true,
show_results_before_voting boolean not null default false,
allow_vote_change boolean not null default true,
status text not null default 'OPEN'::text,
created_at timestamp with time zone not null default now(),
closed_at timestamp with time zone,
winner_option_id uuid,
expected_voters integer,
constraint polls_creator_id_fkey FOREIGN KEY (creator_id) REFERENCES auth.users(id),
constraint polls_pkey PRIMARY KEY (id),
constraint polls_status_check CHECK ((status = ANY (ARRAY['OPEN'::text, 'CLOSED'::text])))
);
alter table public.polls enable row level security;

create table public.poll_options (
id uuid not null default gen_random_uuid(),
poll_id uuid not null,
text text not null,
status text not null default 'APPROVED'::text,
vote_count integer not null default 0,
suggested_by uuid,
created_at timestamp with time zone not null default now(),
constraint poll_options_pkey PRIMARY KEY (id),
constraint poll_options_poll_id_fkey FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE,
constraint poll_options_status_check CHECK ((status = ANY (ARRAY['APPROVED'::text, 'PENDING'::text, 'REJECTED'::text]))),
constraint poll_options_suggested_by_fkey FOREIGN KEY (suggested_by) REFERENCES auth.users(id)
);
alter table public.poll_options enable row level security;

create table public.votes (
id uuid not null default gen_random_uuid(),
poll_id uuid not null,
voter_id uuid not null,
option_id uuid not null,
created_at timestamp with time zone not null default now(),
constraint votes_option_id_fkey FOREIGN KEY (option_id) REFERENCES poll_options(id),
constraint votes_pkey PRIMARY KEY (id),
constraint votes_poll_id_fkey FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE,
constraint votes_poll_id_voter_id_key UNIQUE (poll_id, voter_id),
constraint votes_voter_id_fkey FOREIGN KEY (voter_id) REFERENCES auth.users(id)
);
alter table public.votes enable row level security;

create table public.notifications (
id uuid not null default gen_random_uuid(),
user_id uuid not null,
type text not null,
poll_id uuid not null,
poll_question text not null,
detail text,
created_at timestamp with time zone not null default now(),
is_read boolean not null default false,
constraint notifications_pkey PRIMARY KEY (id),
constraint notifications_poll_id_fkey FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE,
constraint notifications_type_check CHECK ((type = ANY (ARRAY['POLL_CREATED'::text, 'POLL_CLOSED'::text, 'NEW_VOTES'::text, 'SUGGESTION_RECEIVED'::text, 'SUGGESTION_APPROVED'::text, 'SUGGESTION_REJECTED'::text]))),
constraint notifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id)
);
alter table public.notifications enable row level security;

create table public.poll_creation_requests (
creator_id uuid not null,
request_id uuid not null,
poll_id uuid not null,
constraint poll_creation_requests_creator_id_fkey FOREIGN KEY (creator_id) REFERENCES auth.users(id),
constraint poll_creation_requests_pkey PRIMARY KEY (creator_id, request_id),
constraint poll_creation_requests_poll_id_fkey FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE
);
alter table public.poll_creation_requests enable row level security;

CREATE INDEX idx_votes_poll_id ON public.votes USING btree (poll_id);
CREATE INDEX idx_votes_voter_id ON public.votes USING btree (voter_id);
CREATE INDEX idx_poll_options_poll_id ON public.poll_options USING btree (poll_id);
CREATE INDEX idx_notifications_user_id ON public.notifications USING btree (user_id);

CREATE OR REPLACE FUNCTION public.create_poll(p_question text, p_options text[], p_allow_suggestions boolean, p_require_approval boolean, p_show_results_before_voting boolean, p_allow_vote_change boolean, p_deadline_seconds integer, p_expected_voters integer, p_request_id uuid)
 RETURNS uuid
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  v_user uuid := auth.uid();
  v_poll uuid;
  v_options text[];
begin
  if v_user is null then raise exception 'Authentication required' using errcode = '42501'; end if;
  if p_request_id is null then raise exception 'Request ID required'; end if;
  -- Serialize retries before inserting anything. Hash collisions merely serialize unrelated requests.
  perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(v_user::text || ':' || p_request_id::text, 0));
  select poll_id into v_poll from public.poll_creation_requests where creator_id = v_user and request_id = p_request_id;
  if v_poll is not null then return v_poll; end if;
  if p_question is null or pg_catalog.btrim(p_question) = '' then raise exception 'Question required'; end if;
  if p_options is null or pg_catalog.cardinality(p_options) > 10 then raise exception 'Two to ten options required'; end if;
  select pg_catalog.array_agg(text order by ordinal) into v_options from (
    select distinct on (pg_catalog.lower(pg_catalog.btrim(text))) pg_catalog.btrim(text) as text, ordinal
    from pg_catalog.unnest(p_options) with ordinality as entries(text, ordinal)
    where text is not null and pg_catalog.btrim(text) <> ''
    order by pg_catalog.lower(pg_catalog.btrim(text)), ordinal
  ) cleaned;
  if coalesce(pg_catalog.cardinality(v_options), 0) < 2 then raise exception 'Two distinct options required'; end if;
  if p_expected_voters is not null and p_expected_voters <= 0 then raise exception 'Expected voters must be positive'; end if;
  if p_deadline_seconds is not null and p_deadline_seconds not in (3600, 86400, 259200, 604800) then raise exception 'Invalid deadline'; end if;
  if p_allow_suggestions is null or p_require_approval is null or p_show_results_before_voting is null or p_allow_vote_change is null then raise exception 'Settings required'; end if;
  insert into public.polls(question, creator_id, allow_suggestions, require_approval, show_results_before_voting, allow_vote_change, closed_at, expected_voters)
  values (pg_catalog.btrim(p_question), v_user, p_allow_suggestions, p_require_approval, p_show_results_before_voting, p_allow_vote_change,
    case when p_deadline_seconds is not null then pg_catalog.clock_timestamp() + pg_catalog.make_interval(secs => p_deadline_seconds) end, p_expected_voters)
  returning id into v_poll;
  insert into public.poll_options(poll_id, text, status, vote_count)
  select v_poll, text, 'APPROVED', 0 from pg_catalog.unnest(v_options) as texts(text);
  insert into public.poll_creation_requests values (v_user, p_request_id, v_poll);
  -- Match Android's creation notification without duplicating it on retries.
  insert into public.notifications(user_id, type, poll_id, poll_question)
  values (v_user, 'POLL_CREATED', v_poll, pg_catalog.btrim(p_question));
  return v_poll;
end;
$function$;

CREATE OR REPLACE FUNCTION public.pickle_finalize_poll(p_poll_id uuid, p_closed_at timestamp with time zone)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare v_winner uuid; v_max integer; v_count integer;
begin
  select max(vote_count) into v_max from public.poll_options where poll_id = p_poll_id and status = 'APPROVED';
  if coalesce(v_max, 0) > 0 then
    select count(*) into v_count from public.poll_options where poll_id = p_poll_id and status = 'APPROVED' and vote_count = v_max;
    if v_count = 1 then
      select id into v_winner from public.poll_options where poll_id = p_poll_id and status = 'APPROVED' and vote_count = v_max;
    end if;
  end if;
  update public.polls set status = 'CLOSED', closed_at = p_closed_at, winner_option_id = v_winner where id = p_poll_id;
end;
$function$;

CREATE OR REPLACE FUNCTION public.pickle_guard_option()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare v_poll public.polls%rowtype;
begin
  if TG_OP = 'UPDATE' then
    if new.poll_id <> old.poll_id then raise exception 'Cannot move options between polls'; end if;
    if new.status = old.status and new.text = old.text and new.suggested_by is not distinct from old.suggested_by then return new; end if;
  end if;
  select * into v_poll from public.polls where id = new.poll_id for update;
  if v_poll.status <> 'OPEN' or v_poll.closed_at <= pg_catalog.clock_timestamp() then raise exception 'Poll is not open'; end if;
  if TG_OP = 'UPDATE' and v_poll.creator_id is distinct from auth.uid() then raise exception 'Not authorized' using errcode = '42501'; end if;
  return new;
end;
$function$;

CREATE OR REPLACE FUNCTION public.expire_poll(p_poll_id uuid)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare v_poll public.polls%rowtype;
begin
  if auth.uid() is null then raise exception 'Authentication required' using errcode = '42501'; end if;
  select * into v_poll from public.polls where id = p_poll_id for update;
  if v_poll.status = 'OPEN' and v_poll.closed_at <= pg_catalog.clock_timestamp() then
    perform public.pickle_finalize_poll(p_poll_id, v_poll.closed_at);
  end if;
end;
$function$;

CREATE OR REPLACE FUNCTION public.close_poll(p_poll_id uuid)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare v_poll public.polls%rowtype;
begin
  if auth.uid() is null then raise exception 'Authentication required' using errcode = '42501'; end if;
  select * into v_poll from public.polls where id = p_poll_id for update;
  if v_poll.id is null or v_poll.creator_id <> auth.uid() or v_poll.status <> 'OPEN' then
    raise exception 'Not authorized or poll not open' using errcode = '42501';
  end if;
  perform public.pickle_finalize_poll(p_poll_id, case when v_poll.closed_at <= pg_catalog.clock_timestamp() then v_poll.closed_at else pg_catalog.clock_timestamp() end);
end;
$function$;

CREATE OR REPLACE FUNCTION public.cast_vote(p_poll_id uuid, p_option_id uuid)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare v_poll public.polls%rowtype; v_old uuid; v_user uuid := auth.uid();
begin
  if v_user is null then raise exception 'Authentication required' using errcode = '42501'; end if;
  select * into v_poll from public.polls where id = p_poll_id for update;
  if v_poll.id is null or v_poll.status <> 'OPEN' or v_poll.closed_at <= pg_catalog.clock_timestamp() then
    -- Do not finalize then raise: raising would roll back finalization. Readers call expire_poll.
    raise exception 'Poll is not open';
  end if;
  if not exists (select 1 from public.poll_options where id = p_option_id and poll_id = p_poll_id and status = 'APPROVED') then
    raise exception 'Option not found or not approved';
  end if;
  select option_id into v_old from public.votes where poll_id = p_poll_id and voter_id = v_user;
  if v_old is not null then
    if not v_poll.allow_vote_change then raise exception 'Vote change not allowed'; end if;
    if v_old = p_option_id then return; end if;
    update public.poll_options set vote_count = greatest(vote_count - 1, 0) where id = v_old;
    update public.votes set option_id = p_option_id, created_at = pg_catalog.clock_timestamp() where poll_id = p_poll_id and voter_id = v_user;
  else
    insert into public.votes(poll_id, voter_id, option_id) values(p_poll_id, v_user, p_option_id);
  end if;
  update public.poll_options set vote_count = vote_count + 1 where id = p_option_id;
end;
$function$;

CREATE OR REPLACE FUNCTION public.moderate_option(p_poll_id uuid, p_option_id uuid, p_approve boolean)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare v_poll public.polls%rowtype;
begin
  if auth.uid() is null then raise exception 'Authentication required' using errcode = '42501'; end if;
  select * into v_poll from public.polls where id = p_poll_id for update;
  if v_poll.id is null or v_poll.creator_id <> auth.uid() then raise exception 'Not authorized' using errcode = '42501'; end if;
  if v_poll.status <> 'OPEN' or v_poll.closed_at <= pg_catalog.clock_timestamp() then raise exception 'Poll is not open'; end if;
  if p_approve is null then raise exception 'Decision required'; end if;
  update public.poll_options set status = case when p_approve then 'APPROVED' else 'REJECTED' end
  where id = p_option_id and poll_id = p_poll_id and status = 'PENDING';
  if not found then raise exception 'Suggestion unavailable'; end if;
end;
$function$;

CREATE TRIGGER pickle_guard_option BEFORE INSERT OR UPDATE ON public.poll_options FOR EACH ROW EXECUTE FUNCTION pickle_guard_option();

create policy "votes_delete" on public.votes for DELETE to authenticated using ((voter_id = auth.uid()));
create policy "votes_update" on public.votes for UPDATE to authenticated using ((voter_id = auth.uid()));
create policy "votes_insert" on public.votes for INSERT to authenticated with check ((voter_id = auth.uid()));
create policy "votes_select" on public.votes for SELECT to authenticated using ((voter_id = auth.uid()));
create policy "polls_update" on public.polls for UPDATE to authenticated using ((creator_id = auth.uid())) with check ((creator_id = auth.uid()));
create policy "polls_insert" on public.polls for INSERT to authenticated with check ((creator_id = auth.uid()));
create policy "polls_select" on public.polls for SELECT to authenticated using (true);
create policy "poll_options_update" on public.poll_options for UPDATE to authenticated using ((EXISTS ( SELECT 1
   FROM polls p
  WHERE ((p.id = poll_options.poll_id) AND (p.creator_id = auth.uid()))))) with check ((EXISTS ( SELECT 1
   FROM polls p
  WHERE ((p.id = poll_options.poll_id) AND (p.creator_id = auth.uid())))));
create policy "poll_options_select" on public.poll_options for SELECT to authenticated using (true);
create policy "notifications_delete" on public.notifications for DELETE to authenticated using ((user_id = auth.uid()));
create policy "notifications_update" on public.notifications for UPDATE to authenticated using ((user_id = auth.uid()));
create policy "notifications_insert" on public.notifications for INSERT to authenticated with check ((user_id = auth.uid()));
create policy "notifications_select" on public.notifications for SELECT to authenticated using ((user_id = auth.uid()));

grant select, insert, update, delete on public.polls, public.poll_options, public.notifications to anon, authenticated;
grant select on public.votes to anon, authenticated;
revoke insert, update, delete on public.votes from anon, authenticated;
revoke all on public.poll_creation_requests from anon, authenticated;
revoke all on function public.create_poll(p_question text, p_options text[], p_allow_suggestions boolean, p_require_approval boolean, p_show_results_before_voting boolean, p_allow_vote_change boolean, p_deadline_seconds integer, p_expected_voters integer, p_request_id uuid) from public, anon, authenticated;
grant execute on function public.create_poll(p_question text, p_options text[], p_allow_suggestions boolean, p_require_approval boolean, p_show_results_before_voting boolean, p_allow_vote_change boolean, p_deadline_seconds integer, p_expected_voters integer, p_request_id uuid) to service_role, authenticated;
revoke all on function public.pickle_finalize_poll(p_poll_id uuid, p_closed_at timestamp with time zone) from public, anon, authenticated;
grant execute on function public.pickle_finalize_poll(p_poll_id uuid, p_closed_at timestamp with time zone) to service_role;
revoke all on function public.pickle_guard_option() from public, anon, authenticated;
grant execute on function public.pickle_guard_option() to service_role;
revoke all on function public.expire_poll(p_poll_id uuid) from public, anon, authenticated;
grant execute on function public.expire_poll(p_poll_id uuid) to service_role, authenticated;
revoke all on function public.close_poll(p_poll_id uuid) from public, anon, authenticated;
grant execute on function public.close_poll(p_poll_id uuid) to service_role, authenticated;
revoke all on function public.cast_vote(p_poll_id uuid, p_option_id uuid) from public, anon, authenticated;
grant execute on function public.cast_vote(p_poll_id uuid, p_option_id uuid) to service_role, authenticated;
revoke all on function public.moderate_option(p_poll_id uuid, p_option_id uuid, p_approve boolean) from public, anon, authenticated;
grant execute on function public.moderate_option(p_poll_id uuid, p_option_id uuid, p_approve boolean) to service_role, authenticated;

drop policy if exists poll_options_insert on public.poll_options;
create policy poll_options_insert on public.poll_options for insert to authenticated
with check (
  poll_options.vote_count = 0
  and pg_catalog.btrim(poll_options.text) <> ''
  and exists (
    select 1 from public.polls p
    where p.id = poll_options.poll_id and p.status = 'OPEN'
      and (p.closed_at is null or p.closed_at > pg_catalog.clock_timestamp())
      and (
        (p.creator_id = (select auth.uid())
          and poll_options.status = 'APPROVED'
          and (poll_options.suggested_by is null or poll_options.suggested_by = (select auth.uid())))
        or (p.allow_suggestions
          and poll_options.suggested_by = (select auth.uid())
          and poll_options.status = case when p.require_approval then 'PENDING' else 'APPROVED' end)
      )
  )
);
revoke truncate, trigger, references on public.polls, public.poll_options, public.votes, public.notifications, public.poll_creation_requests from anon, authenticated;


-- 20261015101848_templates_and_multi_choice
-- Additive template and approval-ballot support. Existing single-choice RPCs stay available.
alter table public.polls add column voting_mode text not null default 'SINGLE' check (voting_mode in ('SINGLE','MULTIPLE'));
alter table public.polls add column voter_count integer not null default 0 check (voter_count >= 0);
alter table public.poll_options add column position integer not null default 2147483647;
with ranked as (select id, row_number() over(partition by poll_id order by created_at,id)-1 as pos from public.poll_options)
update public.poll_options o set position=r.pos from ranked r where o.id=r.id;
update public.polls p set voter_count=(select count(*) from public.votes v where v.poll_id=p.id);

create table public.poll_templates (
 id uuid primary key default gen_random_uuid(),
 owner_id uuid not null references auth.users(id),
 source_poll_id uuid not null unique references public.polls(id),
 name text not null check (btrim(name) <> ''),
 icon text not null check (icon in ('film','pizza','date','place','game','driver')),
 question text not null,
 option_texts text[] not null,
 allow_suggestions boolean not null,
 require_approval boolean not null,
 show_results_before_voting boolean not null,
 allow_vote_change boolean not null,
 voting_mode text not null check (voting_mode in ('SINGLE','MULTIPLE')),
 schedule_weekday integer check (schedule_weekday between 1 and 7),
 schedule_time text,
 timezone text not null,
 rounds_count integer not null default 1,
 created_at timestamptz not null default now(),
 updated_at timestamptz not null default now(),
 check ((schedule_weekday is null) = (schedule_time is null))
);
create index poll_templates_owner on public.poll_templates(owner_id,updated_at desc);
alter table public.polls add column template_id uuid references public.poll_templates(id);
alter table public.polls add column template_round integer;
create unique index polls_template_round on public.polls(template_id,template_round) where template_id is not null;

create table public.poll_ballots (
 poll_id uuid not null references public.polls(id) on delete cascade,
 voter_id uuid not null references auth.users(id),
 option_ids uuid[] not null check (cardinality(option_ids) between 1 and 10),
 updated_at timestamptz not null default now(),
 primary key(poll_id,voter_id)
);
create index poll_ballots_voter on public.poll_ballots(voter_id);
alter table public.poll_templates enable row level security;
alter table public.poll_ballots enable row level security;
create policy templates_owner_read on public.poll_templates for select to authenticated using(owner_id=(select auth.uid()));
create policy ballots_owner_read on public.poll_ballots for select to authenticated using(voter_id=(select auth.uid()));
revoke all on public.poll_templates, public.poll_ballots from anon,authenticated;
grant select on public.poll_templates, public.poll_ballots to authenticated;
grant all on public.poll_templates, public.poll_ballots to service_role;
-- New contract fields must only be changed by the transactional RPCs.
revoke insert on public.polls from authenticated,anon;
grant insert(question,creator_id,allow_suggestions,require_approval,show_results_before_voting,allow_vote_change,status,closed_at,winner_option_id,expected_voters) on public.polls to authenticated;
revoke update on public.polls from authenticated,anon;
grant update(question,allow_suggestions,require_approval,show_results_before_voting,allow_vote_change,status,closed_at,winner_option_id,expected_voters) on public.polls to authenticated;

-- Client writers cannot forge aggregate counts or canonical option order.
revoke insert,update on public.poll_options from anon,authenticated;
grant insert(id,poll_id,text,status,suggested_by,created_at) on public.poll_options to authenticated;
grant update(text,status,suggested_by) on public.poll_options to authenticated;

create function public.save_poll_template(p_poll_id uuid,p_name text,p_icon text,p_weekday integer,p_local_time text,p_timezone text)
returns uuid language plpgsql security definer set search_path='' as $$
declare v_poll public.polls%rowtype; v_id uuid; v_options text[];
begin
 if auth.uid() is null then raise exception 'Authentication required' using errcode='42501'; end if;
 select * into v_poll from public.polls where id=p_poll_id for update;
 if v_poll.id is null or v_poll.creator_id<>auth.uid() then raise exception 'Not authorized' using errcode='42501'; end if;
 if p_name is null or btrim(p_name)='' or length(p_name)>80 then raise exception 'Template name required'; end if;
 if p_icon is null or p_icon not in ('film','pizza','date','place','game','driver') then raise exception 'Invalid icon'; end if;
 if not exists(select 1 from pg_catalog.pg_timezone_names where name=p_timezone) then raise exception 'Invalid timezone'; end if;
 if (p_weekday is null)<>(p_local_time is null) or p_weekday not between 1 and 7 then raise exception 'Invalid schedule'; end if;
 if p_local_time is not null then perform p_local_time::time; end if;
 select array_agg(text order by position,created_at,id) into v_options from public.poll_options where poll_id=p_poll_id and status='APPROVED';
 if coalesce(cardinality(v_options),0)<2 then raise exception 'Two approved options required'; end if;
 v_id:=v_poll.template_id;
 if v_id is null then select id into v_id from public.poll_templates where source_poll_id=p_poll_id; end if;
 if v_id is not null then
   update public.poll_templates set name=btrim(p_name),icon=p_icon,schedule_weekday=p_weekday,schedule_time=p_local_time,timezone=p_timezone,updated_at=now() where id=v_id and owner_id=auth.uid();
   if not found then raise exception 'Not authorized' using errcode='42501'; end if;
   return v_id;
 end if;
 insert into public.poll_templates(owner_id,source_poll_id,name,icon,question,option_texts,allow_suggestions,require_approval,show_results_before_voting,allow_vote_change,voting_mode,schedule_weekday,schedule_time,timezone)
 values(auth.uid(),p_poll_id,btrim(p_name),p_icon,v_poll.question,v_options,v_poll.allow_suggestions,v_poll.require_approval,v_poll.show_results_before_voting,v_poll.allow_vote_change,v_poll.voting_mode,p_weekday,p_local_time,p_timezone) returning id into v_id;
 update public.polls set template_id=v_id,template_round=1 where id=p_poll_id;
 return v_id;
end $$;

create function public.create_poll_v2(p_question text,p_options text[],p_allow_suggestions boolean,p_require_approval boolean,p_show_results_before_voting boolean,p_allow_vote_change boolean,p_closes_at timestamptz,p_expected_voters integer,p_request_id uuid,p_voting_mode text,p_template_id uuid default null)
returns uuid language plpgsql security definer set search_path='' as $$
declare v_poll uuid; v_round integer;
begin
 if auth.uid() is null then raise exception 'Authentication required' using errcode='42501'; end if;
 if p_request_id is null then raise exception 'Request ID required'; end if;
 perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(auth.uid()::text||':'||p_request_id::text,0));
 select poll_id into v_poll from public.poll_creation_requests where creator_id=auth.uid() and request_id=p_request_id;
 if v_poll is not null then return v_poll; end if;
 if p_voting_mode is null or p_voting_mode not in ('SINGLE','MULTIPLE') then raise exception 'Invalid voting mode'; end if;
 if p_closes_at is not null and p_closes_at<=clock_timestamp() then raise exception 'Deadline must be in the future'; end if;
 if p_template_id is not null then
  update public.poll_templates set rounds_count=rounds_count+1,updated_at=now() where id=p_template_id and owner_id=auth.uid() returning rounds_count into v_round;
  if not found then raise exception 'Not authorized' using errcode='42501'; end if;
 end if;
 v_poll:=public.create_poll(p_question,p_options,p_allow_suggestions,p_require_approval,p_show_results_before_voting,p_allow_vote_change,null,p_expected_voters,p_request_id);
 update public.polls set voting_mode=p_voting_mode,closed_at=p_closes_at,template_id=p_template_id,template_round=v_round where id=v_poll;
 update public.poll_options o set position=x.ordinal-1 from (select lower(btrim(text)) as text,min(ordinal) as ordinal from unnest(p_options) with ordinality as entries(text,ordinal) group by lower(btrim(text))) x
 where o.poll_id=v_poll and lower(o.text)=x.text;
 return v_poll;
end $$;

CREATE OR REPLACE FUNCTION public.cast_vote(p_poll_id uuid, p_option_id uuid)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare v_poll public.polls%rowtype; v_old uuid; v_user uuid := auth.uid();
begin
  if v_user is null then raise exception 'Authentication required' using errcode = '42501'; end if;
  select * into v_poll from public.polls where id = p_poll_id for update;
  if v_poll.id is null or v_poll.status <> 'OPEN' or v_poll.closed_at <= pg_catalog.clock_timestamp() then
    -- Do not finalize then raise: raising would roll back finalization. Readers call expire_poll.
    raise exception 'Poll is not open';
  end if;
  if v_poll.voting_mode <> 'SINGLE' then raise exception 'Use cast_ballot for multiple choice'; end if;
  if not exists (select 1 from public.poll_options where id = p_option_id and poll_id = p_poll_id and status = 'APPROVED') then
    raise exception 'Option not found or not approved';
  end if;
  select option_id into v_old from public.votes where poll_id = p_poll_id and voter_id = v_user;
  if v_old is not null then
    if v_old = p_option_id then return; end if;
    if not v_poll.allow_vote_change then raise exception 'Vote change not allowed'; end if;
    update public.poll_options set vote_count = greatest(vote_count - 1, 0) where id = v_old;
    update public.votes set option_id = p_option_id, created_at = pg_catalog.clock_timestamp() where poll_id = p_poll_id and voter_id = v_user;
  else
    insert into public.votes(poll_id, voter_id, option_id) values(p_poll_id, v_user, p_option_id);
    update public.polls set voter_count=voter_count+1 where id=p_poll_id;
  end if;
  update public.poll_options set vote_count = vote_count + 1 where id = p_option_id;
end;
$function$;

create function public.cast_ballot(p_poll_id uuid,p_option_ids uuid[])
returns void language plpgsql security definer set search_path='' as $$
declare v_poll public.polls%rowtype; v_user uuid:=auth.uid(); v_ids uuid[]; v_old uuid[];
begin
 if v_user is null then raise exception 'Authentication required' using errcode='42501'; end if;
 select * into v_poll from public.polls where id=p_poll_id for update;
 if v_poll.id is null or v_poll.status<>'OPEN' or v_poll.closed_at<=clock_timestamp() then raise exception 'Poll is not open'; end if;
 if p_option_ids is null or cardinality(p_option_ids) not between 1 and 10 then raise exception 'Select at least one option'; end if;
 select array_agg(distinct id order by id) into v_ids from unnest(p_option_ids) as ids(id) where id is not null;
 if cardinality(v_ids) is distinct from cardinality(p_option_ids) then raise exception 'Invalid ballot'; end if;
 if (select count(*) from public.poll_options where poll_id=p_poll_id and status='APPROVED' and id=any(v_ids))<>cardinality(v_ids) then raise exception 'Option not found or not approved'; end if;
 if v_poll.voting_mode='SINGLE' then
   if cardinality(v_ids)<>1 then raise exception 'Choose one option'; end if;
   perform public.cast_vote(p_poll_id,v_ids[1]); return;
 end if;
 select option_ids into v_old from public.poll_ballots where poll_id=p_poll_id and voter_id=v_user;
 if v_old is not null then
  if v_old=v_ids then return; end if;
  if not v_poll.allow_vote_change then raise exception 'Vote change not allowed'; end if;
 else
  update public.polls set voter_count=voter_count+1 where id=p_poll_id;
 end if;
 update public.poll_options set vote_count=greatest(0,vote_count + case when id=any(v_ids) then 1 else 0 end - case when id=any(coalesce(v_old,'{}'::uuid[])) then 1 else 0 end)
 where poll_id=p_poll_id and (id=any(v_ids) or id=any(coalesce(v_old,'{}'::uuid[])));
 insert into public.poll_ballots(poll_id,voter_id,option_ids) values(p_poll_id,v_user,v_ids)
 on conflict(poll_id,voter_id) do update set option_ids=excluded.option_ids,updated_at=clock_timestamp();
end $$;
revoke all on function public.save_poll_template(uuid,text,text,integer,text,text), public.create_poll_v2(text,text[],boolean,boolean,boolean,boolean,timestamptz,integer,uuid,text,uuid), public.cast_ballot(uuid,uuid[]) from public,anon;
grant execute on function public.save_poll_template(uuid,text,text,integer,text,text), public.create_poll_v2(text,text[],boolean,boolean,boolean,boolean,timestamptz,integer,uuid,text,uuid), public.cast_ballot(uuid,uuid[]) to authenticated;


-- 20261015104704_allow_complete_ballot_sets
-- Approved suggestions can extend a live poll beyond the ten-option creation limit.
-- A ballot may contain any nonempty subset of its approved options.
alter table public.poll_ballots drop constraint poll_ballots_option_ids_check;
alter table public.poll_ballots add constraint poll_ballots_option_ids_check check(cardinality(option_ids)>0);
create or replace function public.cast_ballot(p_poll_id uuid,p_option_ids uuid[])
returns void language plpgsql security definer set search_path='' as $$
declare v_poll public.polls%rowtype; v_user uuid:=auth.uid(); v_ids uuid[]; v_old uuid[];
begin
 if v_user is null then raise exception 'Authentication required' using errcode='42501'; end if;
 select * into v_poll from public.polls where id=p_poll_id for update;
 if v_poll.id is null or v_poll.status<>'OPEN' or v_poll.closed_at<=clock_timestamp() then raise exception 'Poll is not open'; end if;
 if p_option_ids is null or cardinality(p_option_ids)<1 then raise exception 'Select at least one option'; end if;
 select array_agg(distinct id order by id) into v_ids from unnest(p_option_ids) as ids(id) where id is not null;
 if cardinality(v_ids) is distinct from cardinality(p_option_ids) then raise exception 'Invalid ballot'; end if;
 if (select count(*) from public.poll_options where poll_id=p_poll_id and status='APPROVED' and id=any(v_ids))<>cardinality(v_ids) then raise exception 'Option not found or not approved'; end if;
 if v_poll.voting_mode='SINGLE' then
   if cardinality(v_ids)<>1 then raise exception 'Choose one option'; end if;
   perform public.cast_vote(p_poll_id,v_ids[1]); return;
 end if;
 select option_ids into v_old from public.poll_ballots where poll_id=p_poll_id and voter_id=v_user;
 if v_old is not null then
  if v_old=v_ids then return; end if;
  if not v_poll.allow_vote_change then raise exception 'Vote change not allowed'; end if;
 else
  update public.polls set voter_count=voter_count+1 where id=p_poll_id;
 end if;
 update public.poll_options set vote_count=greatest(0,vote_count + case when id=any(v_ids) then 1 else 0 end - case when id=any(coalesce(v_old,'{}'::uuid[])) then 1 else 0 end)
 where poll_id=p_poll_id and (id=any(v_ids) or id=any(coalesce(v_old,'{}'::uuid[])));
 insert into public.poll_ballots(poll_id,voter_id,option_ids) values(p_poll_id,v_user,v_ids)
 on conflict(poll_id,voter_id) do update set option_ids=excluded.option_ids,updated_at=clock_timestamp();
end $$;

commit;
