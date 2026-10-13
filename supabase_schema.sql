-- Pickle App — Supabase Database Schema
-- Run this in the Supabase SQL Editor to set up the database.

-- ============================================================
-- Tables
-- ============================================================

create table polls (
    id uuid primary key default gen_random_uuid(),
    question text not null,
    creator_id uuid not null references auth.users(id),
    allow_suggestions boolean not null default false,
    require_approval boolean not null default true,
    show_results_before_voting boolean not null default false,
    allow_vote_change boolean not null default true,
    status text not null default 'OPEN' check (status in ('OPEN', 'CLOSED')),
    created_at timestamptz not null default now(),
    closed_at timestamptz,
    winner_option_id uuid,
    expected_voters int
);

create table poll_options (
    id uuid primary key default gen_random_uuid(),
    poll_id uuid not null references polls(id) on delete cascade,
    text text not null,
    status text not null default 'APPROVED' check (status in ('APPROVED', 'PENDING', 'REJECTED')),
    vote_count int not null default 0,
    suggested_by uuid references auth.users(id),
    created_at timestamptz not null default now()
);

create table votes (
    id uuid primary key default gen_random_uuid(),
    poll_id uuid not null references polls(id) on delete cascade,
    voter_id uuid not null references auth.users(id),
    option_id uuid not null references poll_options(id),
    created_at timestamptz not null default now(),
    unique(poll_id, voter_id)
);

create table notifications (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id),
    type text not null check (type in (
        'POLL_CREATED', 'POLL_CLOSED', 'NEW_VOTES',
        'SUGGESTION_RECEIVED', 'SUGGESTION_APPROVED', 'SUGGESTION_REJECTED'
    )),
    poll_id uuid not null references polls(id) on delete cascade,
    poll_question text not null,
    detail text,
    created_at timestamptz not null default now(),
    is_read boolean not null default false
);

-- ============================================================
-- Indexes
-- ============================================================

create index idx_poll_options_poll_id on poll_options(poll_id);
create index idx_votes_poll_id on votes(poll_id);
create index idx_votes_voter_id on votes(voter_id);
create index idx_notifications_user_id on notifications(user_id);

-- ============================================================
-- Row Level Security
-- ============================================================

alter table polls enable row level security;
alter table poll_options enable row level security;
alter table votes enable row level security;
alter table notifications enable row level security;

-- polls: any authenticated user can read; creator can insert/update
create policy "polls_select" on polls for select to authenticated using (true);
create policy "polls_insert" on polls for insert to authenticated with check (creator_id = auth.uid());
create policy "polls_update" on polls for update to authenticated using (creator_id = auth.uid());

-- poll_options: any authenticated user can read; insert if creator or suggestions allowed; creator can update
create policy "poll_options_select" on poll_options for select to authenticated using (true);
create policy "poll_options_insert" on poll_options for insert to authenticated
    with check (
        exists (
            select 1 from polls
            where polls.id = poll_id
              and (polls.creator_id = auth.uid() or polls.allow_suggestions = true)
        )
    );
create policy "poll_options_update" on poll_options for update to authenticated
    using (
        exists (
            select 1 from polls
            where polls.id = poll_id
              and polls.creator_id = auth.uid()
        )
    );

-- votes: users can only see/manage their own votes
create policy "votes_select" on votes for select to authenticated using (voter_id = auth.uid());
create policy "votes_insert" on votes for insert to authenticated with check (voter_id = auth.uid());
create policy "votes_update" on votes for update to authenticated using (voter_id = auth.uid());
create policy "votes_delete" on votes for delete to authenticated using (voter_id = auth.uid());

-- notifications: users can only see/manage their own notifications
create policy "notifications_select" on notifications for select to authenticated using (user_id = auth.uid());
create policy "notifications_insert" on notifications for insert to authenticated with check (user_id = auth.uid());
create policy "notifications_update" on notifications for update to authenticated using (user_id = auth.uid());
create policy "notifications_delete" on notifications for delete to authenticated using (user_id = auth.uid());

-- ============================================================
-- RPC Functions
-- ============================================================

-- Atomically cast or change a vote, updating vote_count on poll_options.
create or replace function cast_vote(p_poll_id uuid, p_option_id uuid)
returns void
language plpgsql
security definer
as $$
declare
    v_voter_id uuid := auth.uid();
    v_old_option_id uuid;
    v_allow_change boolean;
    v_poll_status text;
begin
    -- Check poll is open
    select status, allow_vote_change into v_poll_status, v_allow_change
    from polls where id = p_poll_id;

    if v_poll_status is null or v_poll_status != 'OPEN' then
        raise exception 'Poll is not open';
    end if;

    -- Check option exists and is approved
    if not exists (
        select 1 from poll_options
        where id = p_option_id and poll_id = p_poll_id and status = 'APPROVED'
    ) then
        raise exception 'Option not found or not approved';
    end if;

    -- Check for existing vote
    select option_id into v_old_option_id
    from votes where poll_id = p_poll_id and voter_id = v_voter_id;

    if v_old_option_id is not null then
        if not v_allow_change then
            raise exception 'Vote change not allowed';
        end if;
        if v_old_option_id = p_option_id then
            return; -- already voted for this option
        end if;
        -- Decrement old option
        update poll_options set vote_count = greatest(vote_count - 1, 0)
        where id = v_old_option_id;
        -- Update vote record
        update votes set option_id = p_option_id, created_at = now()
        where poll_id = p_poll_id and voter_id = v_voter_id;
    else
        -- Insert new vote
        insert into votes (poll_id, voter_id, option_id) values (p_poll_id, v_voter_id, p_option_id);
    end if;

    -- Increment new option
    update poll_options set vote_count = vote_count + 1 where id = p_option_id;
end;
$$;

-- Atomically close a poll and determine the winner.
create or replace function close_poll(p_poll_id uuid)
returns void
language plpgsql
security definer
as $$
declare
    v_winner_id uuid;
    v_max_votes int;
    v_tie_count int;
begin
    -- Verify caller is the creator
    if not exists (
        select 1 from polls where id = p_poll_id and creator_id = auth.uid() and status = 'OPEN'
    ) then
        raise exception 'Not authorized or poll not open';
    end if;

    -- Find max vote count among approved options
    select max(vote_count) into v_max_votes
    from poll_options where poll_id = p_poll_id and status = 'APPROVED';

    -- Check for tie
    if v_max_votes is not null and v_max_votes > 0 then
        select count(*) into v_tie_count
        from poll_options
        where poll_id = p_poll_id and status = 'APPROVED' and vote_count = v_max_votes;

        if v_tie_count = 1 then
            select id into v_winner_id
            from poll_options
            where poll_id = p_poll_id and status = 'APPROVED' and vote_count = v_max_votes;
        end if;
    end if;

    update polls set
        status = 'CLOSED',
        closed_at = now(),
        winner_option_id = v_winner_id
    where id = p_poll_id;
end;
$$;
