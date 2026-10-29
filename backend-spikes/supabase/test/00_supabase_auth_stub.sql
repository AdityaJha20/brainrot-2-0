-- TEST-ONLY stand-in for the pieces of Supabase that RLS policies depend on, so the policies can be tested in a
-- plain local PostgreSQL without Docker or a Supabase account. On real Supabase these already exist.
-- auth.uid() mirrors Supabase's definition: it reads the JWT "sub" claim from request.jwt.claim(s).
create schema if not exists auth;

create or replace function auth.uid() returns uuid
language sql stable as $$
  select nullif(
    coalesce(
      current_setting('request.jwt.claim.sub', true),
      (current_setting('request.jwt.claims', true)::jsonb ->> 'sub')
    ), ''
  )::uuid
$$;

do $$ begin
  if not exists (select 1 from pg_roles where rolname = 'anon') then create role anon nologin; end if;
  if not exists (select 1 from pg_roles where rolname = 'authenticated') then create role authenticated nologin; end if;
end $$;
grant usage on schema auth to anon, authenticated;
