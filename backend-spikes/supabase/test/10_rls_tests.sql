-- RLS tests. Each block switches to the "authenticated" role with a fake JWT, like a real Supabase request.
\set ON_ERROR_STOP on
\set QUIET on
\pset tuples_only on
\set alice '''11111111-1111-1111-1111-111111111111'''
\set bob   '''22222222-2222-2222-2222-222222222222'''

create or replace function pg_temp.as_user(uid text) returns void language plpgsql as $$
begin
  perform set_config('request.jwt.claims', json_build_object('sub', uid, 'role', 'authenticated')::text, true);
  execute 'set local role authenticated';
end $$;

create or replace function pg_temp.ok(name text) returns void language plpgsql as $$
begin raise notice 'ok - %', name; end $$;

-- Alice creates a habit and an event.
begin;
select pg_temp.as_user(:alice);
insert into public.habits (id, name, measurement, target, unit) values ('water', 'Water', 'QUANTITY', 3, 'L');
insert into public.log_events (id, habit_id, type, value, event_time, zone, created_at, client_mutation_id)
values ('e1', 'water', 'QUANTITY', 1, '2026-10-28T03:00:00Z', 'Asia/Kolkata', now(), 'm-e1');
select pg_temp.ok('owner can insert own habit and event');
commit;

-- Bob sees nothing of Alice's.
begin;
select pg_temp.as_user(:bob);
do $$ begin
  if (select count(*) from public.habits) <> 0 or (select count(*) from public.log_events) <> 0 then
    raise exception 'FAIL: bob can see alice rows';
  end if;
end $$;
select pg_temp.ok('another user sees zero rows (RLS filters silently)');
commit;

-- Bob cannot insert into Alice's account by forging user_id.
begin;
select pg_temp.as_user(:bob);
do $$ begin
  insert into public.log_events (id, user_id, habit_id, type, value, event_time, zone, created_at, client_mutation_id)
  values ('x', '11111111-1111-1111-1111-111111111111', 'water', 'QUANTITY', 1, now(), 'UTC', now(), 'm-x');
  raise exception 'FAIL: forged user_id insert succeeded';
exception when insufficient_privilege then null;
end $$;
select pg_temp.ok('forged user_id insert is refused');
commit;

-- Bob's update/tombstone of Alice's event affects zero rows.
begin;
select pg_temp.as_user(:bob);
do $$ declare n int; begin
  update public.log_events set deleted = true where id = 'e1';
  get diagnostics n = row_count;
  if n <> 0 then raise exception 'FAIL: bob tombstoned alice event'; end if;
end $$;
select pg_temp.ok('another user cannot tombstone the event');
commit;

-- Anonymous role has no table privileges at all.
begin;
set local role anon;
do $$ begin
  perform count(*) from public.log_events;
  raise exception 'FAIL: anon could query';
exception when insufficient_privilege then null;
end $$;
select pg_temp.ok('signed-out (anon) access is refused');
commit;

-- Append-only events.
begin;
select pg_temp.as_user(:alice);
do $$ begin
  update public.log_events set value = 99 where id = 'e1';
  raise exception 'FAIL: event value was edited';
exception when raise_exception then
  if sqlerrm like 'FAIL%' then raise; end if;
end $$;
do $$ declare n int; begin
  delete from public.log_events where id = 'e1';
  get diagnostics n = row_count;
  if n <> 0 then raise exception 'FAIL: hard delete succeeded'; end if;
exception when insufficient_privilege then null;
end $$;
update public.log_events set deleted = true, deleted_at = now() where id = 'e1';
do $$ begin
  if not (select deleted from public.log_events where id = 'e1') then raise exception 'FAIL: tombstone not stored'; end if;
end $$;
select pg_temp.ok('events: edit refused, hard delete refused, tombstone allowed');
commit;

-- Idempotent replay of the same sync mutation.
begin;
select pg_temp.as_user(:alice);
insert into public.log_events (id, habit_id, type, value, event_time, zone, created_at, client_mutation_id)
values ('e2', 'water', 'QUANTITY', 1, now(), 'UTC', now(), 'm-e2')
on conflict (user_id, client_mutation_id) do nothing;
insert into public.log_events (id, habit_id, type, value, event_time, zone, created_at, client_mutation_id)
values ('e2', 'water', 'QUANTITY', 1, now(), 'UTC', now(), 'm-e2')
on conflict (user_id, client_mutation_id) do nothing;
do $$ begin
  if (select count(*) from public.log_events where client_mutation_id = 'm-e2') <> 1 then raise exception 'FAIL: duplicate'; end if;
end $$;
select pg_temp.ok('replayed mutation does not duplicate');
commit;

-- Version conflict on habit settings.
begin;
select pg_temp.as_user(:alice);
update public.habits set target = 2.5, version = 2 where id = 'water';
do $$ begin
  update public.habits set target = 4, version = 2 where id = 'water'; -- stale: based on v1
  raise exception 'FAIL: stale edit accepted';
exception when serialization_failure then null;
end $$;
do $$ begin
  if (select target from public.habits where id = 'water') <> 2.5 then raise exception 'FAIL: wrong target kept'; end if;
end $$;
select pg_temp.ok('stale habit edit is a conflict; newer value kept');
commit;

\echo 'ALL RLS TESTS PASSED'
