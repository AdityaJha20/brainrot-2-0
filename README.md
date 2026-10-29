# Brain Rot 2.0

A personal, Android-first habit tracker with a synced web companion, built to run at a ₹0 recurring cost.
The product source of truth is the *Brain Rot 2.0 Complete Notion Handoff*.

> **Current stage: Phase 0 (feasibility and foundation).** This repository holds test builds, prototypes and
> evidence, **not** the product. Start with **[docs/phase0/REPORT.md](docs/phase0/REPORT.md)**.

## Layout

| Path | What it is |
|---|---|
| `docs/phase0/` | Phase 0 report, backend comparison, architecture, widget findings, phone test protocol, evidence, sourced research |
| `docs/decisions/` | Decision log (Proposed / Accepted) |
| `shared-fixtures/scoring/` | The scoring contract: JSON test cases that **both** scoring engines must pass |
| `core/` | Pure Kotlin: scoring engine, export (JSON/CSV), widget geometry and double-tap timing. Fast JVM tests |
| `android/` | Phase 0 Android test app: Glance widget, heatmap bitmap, Room database, quick-entry screen, probe tools |
| `web/scoring/` | TypeScript scoring engine (twin of `core/`), zero dependencies |
| `backend-spikes/firebase/` | Proposed Firestore Security Rules + tests against the local emulator (`demo-` project, no account) |
| `backend-spikes/supabase/` | Proposed Postgres schema + Row Level Security tests against a local PostgreSQL |

## Running the checks locally

Needs JDK 17+ (21 used), Node 22.18+, the Android SDK (platform 37, build-tools 36), and PostgreSQL 16 for the Supabase test.

```bash
./gradlew ktlintCheck :core:test :android:testDebugUnitTest :android:assembleDebug
```

```bash
(cd web/scoring && npm test)
```

```bash
(cd backend-spikes/firebase && npm ci && npm test)
```

```bash
./backend-spikes/supabase/run-local-tests.sh
```

The debug APK is written to `android/build/outputs/apk/debug/android-debug.apk`. CI (`.github/workflows/ci.yml`) runs all of the above without any secrets or cloud accounts.

## Ground rules

- ₹0 recurring cost. Never link a billing account or enable a paid product without explicit approval.
- Offline-first and manual-first. AI is optional and never the source of truth.
- Never claim a feature works on the target phone (OPPO F29) unless it was tested there.
- No silent changes to scoring, privacy, security or sync rules. Record decisions in `docs/decisions/`.
