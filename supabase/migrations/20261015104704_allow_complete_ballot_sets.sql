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
