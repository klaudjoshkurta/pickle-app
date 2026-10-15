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

