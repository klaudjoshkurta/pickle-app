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

commit;
