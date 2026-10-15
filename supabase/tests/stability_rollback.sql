-- These fixtures and all writes are rolled back, including on assertion failure.
begin;
do $test$
declare
  creator uuid := gen_random_uuid();
  voter uuid := gen_random_uuid();
  request_id uuid := gen_random_uuid();
  poll uuid;
  repeated uuid;
  expired uuid;
  option_id uuid;
  option_b uuid;
  pending uuid;
  caught boolean;
begin
  insert into auth.users(id, email) values
    (creator, creator::text || '@pickle-test.invalid'),
    (voter, voter::text || '@pickle-test.invalid');
  execute 'set local role authenticated';
  perform set_config('request.jwt.claim.sub', creator::text, true);
  poll := public.create_poll('Rollback stability test', array['A','B'], true, true, false, true, null, null, request_id);
  repeated := public.create_poll('Rollback stability test', array['A','B'], true, true, false, true, null, null, request_id);
  if repeated <> poll then raise exception 'Retry produced a duplicate poll'; end if;
  if (select count(*) from public.poll_options where poll_id=poll) <> 2 then raise exception 'Incomplete poll'; end if;
  if (select count(*) from public.notifications where poll_id=poll and type='POLL_CREATED') <> 1 then raise exception 'Duplicate creation notification'; end if;
  insert into public.poll_options(poll_id,text,status) values (poll,'Creator option','APPROVED') returning id into option_id;
  caught := false;
  begin
    insert into public.polls(question,creator_id) values ('Forged owner',voter);
  exception when insufficient_privilege then caught := true;
  end;
  if not caught then raise exception 'Forged poll owner accepted'; end if;
  caught := false;
  begin
    perform public.create_poll('Invalid options',array['same','SAME'],true,true,false,true,null,null,gen_random_uuid());
  exception when raise_exception then caught := true;
  end;
  if not caught then raise exception 'Invalid atomic creation accepted'; end if;

  perform set_config('request.jwt.claim.sub', voter::text, true);
  insert into public.poll_options(poll_id,text,status,suggested_by)
    values(poll,'Pending suggestion','PENDING',voter) returning id into pending;
  caught := false;
  begin
    insert into public.poll_options(poll_id,text,status,suggested_by) values(poll,'Self-approved','APPROVED',voter);
  exception when insufficient_privilege then caught := true;
  end;
  if not caught then raise exception 'Self-approved suggestion accepted'; end if;
  caught := false;
  begin
    insert into public.poll_options(poll_id,text,status,suggested_by) values(poll,'Forged author','PENDING',creator);
  exception when insufficient_privilege then caught := true;
  end;
  if not caught then raise exception 'Forged suggestion author accepted'; end if;
  caught := false;
  begin perform public.moderate_option(poll,pending,true);
  exception when insufficient_privilege then caught := true;
  end;
  if not caught then raise exception 'Noncreator moderation accepted'; end if;
  perform public.cast_vote(poll,option_id);
  select id into option_b from public.poll_options where poll_id=poll and text='B';
  perform public.cast_vote(poll,option_b);
  if (select vote_count from public.poll_options where id=option_id) <> 0
    or (select vote_count from public.poll_options where id=option_b) <> 1
    or (select count(*) from public.votes where poll_id=poll) <> 1 then
    raise exception 'Vote change corrupted totals';
  end if;

  perform set_config('request.jwt.claim.sub',creator::text,true);
  perform public.moderate_option(poll,pending,true);
  if (select status from public.poll_options where id=pending) <> 'APPROVED' then raise exception 'Moderation failed'; end if;
  perform public.close_poll(poll);
  if (select winner_option_id from public.polls where id=poll) <> option_b then raise exception 'Wrong winner'; end if;
  caught := false;
  begin
    insert into public.poll_options(poll_id,text,status) values(poll,'Closed insert','APPROVED');
  exception when raise_exception or insufficient_privilege then caught := true;
  end;
  if not caught then raise exception 'Closed poll insert accepted'; end if;

  expired := public.create_poll('Expired test',array['A','B'],true,true,false,true,null,null,gen_random_uuid());
  execute 'reset role';
  update public.polls set closed_at=clock_timestamp()-interval '1 second' where id=expired;
  execute 'set local role authenticated';
  caught := false;
  begin
    insert into public.poll_options(poll_id,text,status) values(expired,'Late option','APPROVED');
  exception when raise_exception or insufficient_privilege then caught := true;
  end;
  if not caught then raise exception 'Expired poll insert accepted'; end if;
  perform public.expire_poll(expired);
  if (select status from public.polls where id=expired) <> 'CLOSED' then raise exception 'Expiration failed'; end if;
  execute 'reset role';
end;
$test$;
rollback;
select 'passed: atomic creation and retry, RLS owner/author/approval checks, vote changes, moderation, closing, expiration; all fixtures rolled back' as validation;

