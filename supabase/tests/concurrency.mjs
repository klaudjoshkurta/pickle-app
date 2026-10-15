// Requires psql and an explicitly designated isolated LOCAL Supabase database.
// No migrations are applied. No production host is accepted.
import { spawn } from "node:child_process";
import { randomUUID } from "node:crypto";
import assert from "node:assert/strict";
const connection = process.env.PICKLE_TEST_DATABASE_URL;
if (!connection || process.env.PICKLE_ISOLATED_DATABASE !== "1") {
  throw new Error("Set PICKLE_TEST_DATABASE_URL and PICKLE_ISOLATED_DATABASE=1 for an isolated local Supabase database.");
}
const url = new URL(connection);
if (!["localhost", "127.0.0.1", "[::1]"].includes(url.hostname)) throw new Error("Only a local isolated test database is supported.");
function sql(input) {
  return new Promise((resolve, reject) => {
    const child = spawn(process.env.PSQL_BIN || "psql", ["-X", "-A", "-t", "-v", "ON_ERROR_STOP=1"], {
      env: { ...process.env, PGHOST: url.hostname, PGPORT: url.port || "5432", PGDATABASE: decodeURIComponent(url.pathname.slice(1)), PGUSER: decodeURIComponent(url.username), PGPASSWORD: decodeURIComponent(url.password) },
      stdio: ["pipe", "pipe", "pipe"], windowsHide: true,
    });
    let output = ""; let error = "";
    child.stdout.on("data", (d) => { output += d; });
    child.stderr.on("data", (d) => { error += d; });
    child.on("error", reject);
    child.on("close", (code) => code === 0 ? resolve(output.trim()) : reject(new Error(error)));
    child.stdin.end(input);
  });
}
const owner = randomUUID(); const voter = randomUUID(); const request = randomUUID();
let poll; const a = randomUUID(); const b = randomUUID(); const pending = randomUUID();
const claims = (user) => `set local role authenticated; select set_config('request.jwt.claim.sub','${user}',true);`;
const create = `select public.create_poll('Concurrency test',array['A','B'],false,true,false,true,null,null,'${request}');`;
const uuidFrom = (text) => text.match(/[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}/g)?.at(-1);
try {
  await sql(`insert into auth.users(id) values('${owner}'),('${voter}');`);
  const [first, second] = await Promise.all([
    sql(`begin; ${claims(owner)} ${create} select pg_sleep(0.3); commit;`),
    sql(`begin; ${claims(owner)} ${create} commit;`),
  ]);
  assert.equal(uuidFrom(first), uuidFrom(second), "Concurrent retries created different polls");
  assert.equal(await sql(`select count(*) from public.polls where creator_id='${owner}';`), "1");

  poll=uuidFrom(await sql(`begin; ${claims(owner)}
    insert into public.polls(question,creator_id,allow_suggestions) values('Voting race','${owner}',true) returning id; commit;`));
  assert.ok(poll);
  await sql(`begin; ${claims(owner)}
    insert into public.poll_options(id,poll_id,text) values('${a}','${poll}','A'),('${b}','${poll}','B'); commit;
    begin; ${claims(voter)} insert into public.poll_options(id,poll_id,text,status,suggested_by) values('${pending}','${poll}','C','PENDING','${voter}'); commit;`);
  const whenOpen = (statement) => `do $$ begin ${statement} exception when others then if sqlerrm <> 'Poll is not open' then raise; end if; end; $$;`;
  await Promise.all([
    sql(`begin; ${claims(voter)} ${whenOpen(`perform public.cast_vote('${poll}','${a}');`)} select pg_sleep(0.2); commit;`),
    sql(`begin; ${claims(owner)} ${whenOpen(`perform public.moderate_option('${poll}','${pending}',true);`)} commit;`),
    sql(`begin; ${claims(owner)} select public.close_poll('${poll}'); commit;`),
  ]);
  assert.equal(await sql(`select status from public.polls where id='${poll}';`), "CLOSED");
  assert.equal(await sql(`select sum(vote_count) = (select count(*) from public.votes where poll_id='${poll}') from public.poll_options where poll_id='${poll}';`), "t", "Counts differ from committed votes");
  const before = await sql(`select status from public.poll_options where id='${pending}';`);
  await sql(`begin; ${claims(owner)} ${whenOpen(`perform public.moderate_option('${poll}','${pending}',false);`)} commit;`);
  assert.equal(await sql(`select status from public.poll_options where id='${pending}';`), before, "Option changed after closure");
  const templateId = uuidFrom(await sql(`begin; ${claims(owner)} select public.save_poll_template('${poll}','Race template','film',null,null,'UTC'); commit;`));
  assert.ok(templateId);
  const roundRequest=randomUUID();
  const reuse=`select public.create_poll_v2('Ballot race',array['A','B','C'],false,true,true,true,null,null,'${roundRequest}','MULTIPLE','${templateId}');`;
  const rounds=await Promise.all([
    sql(`begin; ${claims(owner)} ${reuse} select pg_sleep(0.3); commit;`),
    sql(`begin; ${claims(owner)} ${reuse} commit;`),
  ]);
  const multi=uuidFrom(rounds[0]);assert.equal(multi,uuidFrom(rounds[1]));
  assert.equal(await sql(`select rounds_count from public.poll_templates where id='${templateId}';`),'2','Retry incremented history');
  assert.equal(await sql(`select count(*) from public.notifications where poll_id='${multi}' and type='POLL_CREATED';`),'1');
  const choiceIds=(await sql(`select id from public.poll_options where poll_id='${multi}' order by position;`)).split(/\s+/);
  const ballot=(ids)=>`perform public.cast_ballot('${multi}',array[${ids.map(id=>`'${id}'::uuid`).join(',')}]);`;
  await Promise.all([
    sql(`begin; ${claims(voter)} ${whenOpen(ballot(choiceIds.slice(0,2)))} select pg_sleep(0.2); commit;`),
    sql(`begin; ${claims(voter)} ${whenOpen(ballot(choiceIds.slice(1)))} commit;`),
    sql(`begin; ${claims(owner)} select public.close_poll('${multi}'); commit;`),
  ]);
  assert.equal(await sql(`select status from public.polls where id='${multi}';`),'CLOSED');
  assert.equal(await sql(`select voter_count=(select count(*) from public.poll_ballots where poll_id='${multi}') from public.polls where id='${multi}';`),'t');
  assert.equal(await sql(`select coalesce(sum(vote_count),0)=(select coalesce(sum(cardinality(option_ids)),0) from public.poll_ballots where poll_id='${multi}') from public.poll_options where poll_id='${multi}';`),'t','Selections differ from complete ballots');
  console.log('Concurrent creation, template retries, ballot replacement/closing, and moderation/closing checks passed.');
} finally {
  await sql(`update public.polls set template_id=null,template_round=null where creator_id='${owner}'; delete from public.poll_templates where owner_id='${owner}'; delete from public.polls where creator_id='${owner}'; delete from auth.users where id in ('${owner}','${voter}');`);
}
