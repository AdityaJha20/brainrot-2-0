#!/usr/bin/env bash
# Runs the proposed schema + RLS tests in a throwaway local PostgreSQL (no Docker, no Supabase account).
set -euo pipefail
cd "$(dirname "$0")"
PG_BIN="${PG_BIN:-/usr/lib/postgresql/16/bin}"
DATA="$(mktemp -d)"
PORT="${PGPORT_TEST:-55432}"
RUN_AS=()
if [ "$(id -u)" = "0" ]; then RUN_AS=(runuser -u postgres --); chown postgres "$DATA"; fi
cleanup() { "${RUN_AS[@]}" "$PG_BIN/pg_ctl" -D "$DATA" -m immediate stop >/dev/null 2>&1 || true; rm -rf "$DATA"; }
trap cleanup EXIT
"${RUN_AS[@]}" "$PG_BIN/initdb" -D "$DATA" -U postgres -A trust >/dev/null
"${RUN_AS[@]}" "$PG_BIN/pg_ctl" -D "$DATA" -o "-p $PORT -k /tmp -c listen_addresses=''" -w start >/dev/null
PSQL=("${RUN_AS[@]}" "$PG_BIN/psql" -h /tmp -p "$PORT" -U postgres -d postgres -v ON_ERROR_STOP=1 -q)
"${PSQL[@]}" -f test/00_supabase_auth_stub.sql
for m in migrations/*.sql; do "${PSQL[@]}" -f "$m"; done
"${PSQL[@]}" -f test/10_rls_tests.sql 2>&1 | sed 's/^psql:[^ ]* NOTICE:  /  /' | grep -v '^[[:space:]]*$'
