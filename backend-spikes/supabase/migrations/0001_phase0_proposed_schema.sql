-- Brain Rot 2.0 — Phase 0 proposed Supabase schema (mirrors backend-spikes/firebase/firestore.rules).
-- Tested only against a local PostgreSQL 16 with test/00_supabase_auth_stub.sql.

create table public.habits (
  id          text        not null,
  user_id     uuid        not null default auth.uid(),
  name        text        not null,
  measurement text        not null check (measurement in ('BINARY', 'QUANTITY', 'DURATION')),
  target      double precision not null check (target >= 0),
  unit        text,
  version     integer     not null default 1,
  updated_at  timestamptz not null default now(),
  primary key (user_id, id)
);

create table public.log_events (
  id                 text        not null,
  user_id            uuid        not null default auth.uid(),
  habit_id           text        not null,
  type               text        not null check (type in ('QUANTITY', 'DURATION', 'COMPLETION')),
  value              double precision not null,
  event_time         timestamptz not null,
  zone               text        not null check (length(zone) between 1 and 64),
  created_at         timestamptz not null,
  client_mutation_id text        not null,
  deleted            boolean     not null default false,
  deleted_at         timestamptz,
  primary key (user_id, id),
  unique (user_id, client_mutation_id), -- a replayed sync operation can never create a duplicate
  foreign key (user_id, habit_id) references public.habits (user_id, id)
);
create index log_events_user_time on public.log_events (user_id, event_time);

alter table public.habits enable row level security;
alter table public.log_events enable row level security;

-- Official Supabase pattern: (select auth.uid()) = user_id, granted to the authenticated role only.
create policy habits_owner on public.habits for all to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

create policy events_select on public.log_events for select to authenticated
  using ((select auth.uid()) = user_id);
create policy events_insert on public.log_events for insert to authenticated
  with check ((select auth.uid()) = user_id and deleted = false);
create policy events_tombstone on public.log_events for update to authenticated
  using ((select auth.uid()) = user_id and deleted = false)
  with check ((select auth.uid()) = user_id and deleted = true);
-- No delete policy: hard deletes are refused for everyone except the database owner.

-- Events are append-only: an UPDATE may only set the tombstone fields.
create function public.guard_event_update() returns trigger language plpgsql as $$
begin
  if (new.id, new.user_id, new.habit_id, new.type, new.value, new.event_time, new.zone, new.created_at,
      new.client_mutation_id)
     is distinct from
     (old.id, old.user_id, old.habit_id, old.type, old.value, old.event_time, old.zone, old.created_at,
      old.client_mutation_id) then
    raise exception 'log events are append-only; only deleted/deleted_at may change' using errcode = 'P0001';
  end if;
  return new;
end $$;
create trigger log_events_append_only before update on public.log_events
  for each row execute function public.guard_event_update();

-- Optimistic concurrency for editable settings: a stale offline edit is a visible conflict, not a silent overwrite.
create function public.guard_habit_version() returns trigger language plpgsql as $$
begin
  if new.version <> old.version + 1 then
    raise exception 'version conflict: expected %, got %', old.version + 1, new.version using errcode = '40001';
  end if;
  new.updated_at := now();
  return new;
end $$;
create trigger habits_version before update on public.habits
  for each row execute function public.guard_habit_version();

grant usage on schema public to authenticated;
grant select, insert, update on public.habits, public.log_events to authenticated;
-- anon gets nothing at all.
