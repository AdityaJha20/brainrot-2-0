# Brain Rot 2.0: Phase 0 report

**Date:** 29 Oct 2026 · **Scope:** feasibility, technical validation and architecture, no product implementation · **Cost so far:** ₹0. No accounts or cloud resources were created, and nothing that could bill was enabled.

## In one minute

- **Everything that could be tested without your phone or a cloud account was built and tested. All of it passes.**
  - Offline storage that survives restarts, plus a durable sync queue.
  - One scoring engine in Kotlin and TypeScript, agreeing on 20 shared test cases.
  - The contribution graph rendered as a widget image.
  - JSON/CSV export.
  - Security rules for both backends, tested locally.
  - A sideloadable test APK.
- **A true double-tap is not something Android widgets support.** It can only be simulated by timing two taps. The test app measures whether that's good enough on your OPPO F29, and includes two documented fallbacks. **Nothing has been tested on your phone yet.**
- **Backend:** I provisionally recommend **Firebase (free Spark plan, never linked to billing)**, mainly because Supabase's free projects pause after about a week of inactivity. Full sourced comparison: [backend-comparison.md](backend-comparison.md).
- **Two things need you:** creating the private GitHub repo, and running the phone test. Neither needs a Firebase/Supabase account.

## Repository situation

- **No existing Brain Rot code anywhere.** I didn't assume `1st-project--AG-` contains it, and didn't open it.
- **`brain-rot-2.0` doesn't exist yet, and I can't create repositories from this session.** The session can only use repos you attach. Steps for you are below.
- **⚠️ `AdityaJha20/brainrot-2-0` (cloned earlier at your request) is PUBLIC and empty.** I disconnected my local copy from it and pushed nothing there. You may want to delete it or make it private.
- All Phase 0 work is committed **locally** in this cloud session and will be pushed to `brain-rot-2.0` once it exists. **This cloud container is temporary.** Until the work is pushed, it lives only here.

## Status of the 10 Phase 0 tasks

| # | Task | Status | Notes |
|---|---|---|---|
| 1 | Inspect context, confirm repo situation | ✅ Done | See above |
| 2 | Minimal reproducible foundation | ✅ Done locally · ⏳ CI not run yet | Gradle 9.6 + AGP 9.5 (Oct 2026 stable), Kotlin 2.4, ktlint, Node test runner, gitleaks, GitHub Actions workflow (written, not yet run because there's no repo) |
| 3 | Android widget prototype (Glance) | ✅ Built and unit-tested · ⏳ **Not tested on the F29** | Graph mode, quick-entry mode, 3 ways to switch, 2 quick-entry variants, auto-return after 90 s |
| 4 | Contribution graph as an image | ✅ Done (local) · ⏳ on-device display | 23 weeks on 4×2, 489 KB in 16-bit colour vs a 15.6 MB Android limit on the F29. A layout bug was found and fixed. |
| 5 | Widget interaction options / double-tap | ✅ Researched and probe built · ⏳ **needs phone measurement** | [widget-findings.md](widget-findings.md), [device-test-protocol.md](device-test-protocol.md) |
| 6 | Room/SQLite offline persistence | ✅ Done (Robolectric, real SQLite) · ⏳ phone kill/reboot test | 7 tests: restart survival, sync queue, no duplicates, tombstones, backdating |
| 7 | Firebase vs Supabase comparison | ✅ Research and local security tests · ⏳ live tests need accounts | 14 Firestore emulator tests, 8 Postgres RLS tests |
| 8 | Shared scoring logic with common fixtures | ✅ Done | 20 fixtures pass identically in Kotlin and TypeScript. The rules are a *proposal* (see decisions). |
| 9 | Web companion and sync architecture | ✅ Designed · 🟡 partly prototyped | TypeScript scoring and Firestore web SDK offline/conflict behaviour tested; no web UI built (that's Phase 4) |
| 10 | Backup/export | ✅ Done | JSON round-trip with identical scores; CSV with formula-injection guard; both in the app |

**Blocked (and why):**
- **Phone tests:** need you and the OPPO F29.
- **Live cloud tests** (Google sign-in on phone and web, real tokens, quotas, deployment): need a Firebase project, which you chose not to create yet.
- **Pushing and CI:** need the `brain-rot-2.0` repo.

## Main findings

1. **Double-tap isn't a widget gesture.** Android's docs: *"The only gestures available for widgets are touch and vertical swipe."* The simulated version is built and logs real timings for the phone test. → [widget-findings.md](widget-findings.md)
2. **The graph must be one image**, because Glance silently drops more than 10 children per row or column. One image is cheap: it's well under 1/30 of Android's limit on the F29.
3. **Offline-first works as designed:** "saved" means saved on disk, retries can't duplicate, and deletes are tombstones that sync.
4. **Both engines produce identical results.** Kotlin and TypeScript agree on all 20 cases (partial progress, over-target, weights, rest days, missed vs unknown, streaks, backdating, time zones, after-midnight entries).
5. **At ₹0 there are no managed backups** on either Firebase or Supabase. Backup = export from the phone (built). There is also **no server code on Firebase Spark**, which matters only if hosted AI is added later.
6. **Supabase's free projects pause after about 1 week of inactivity.** This is the main reason Firebase is recommended for a personal tool.
7. **Firebase Auth processes your Google identity in the US.** Habit data can stay in Mumbai.
8. **Tooling note:** current AndroidX libraries require *compiling* against API 37. The app still *targets* Android 16 (API 36), so behaviour on your phone is unaffected.

Evidence for all of this: [evidence/test-run-2026-10-29.md](evidence/test-run-2026-10-29.md).

## Recommended architecture

Summary (details in [architecture.md](architecture.md)):
- Native Kotlin app.
- Room on the phone as the source of truth, with an append-only event log and a durable sync queue.
- Firebase Auth + Firestore (Spark) as the cloud copy, guarded by tested Security Rules.
- A static Next.js web companion on Firebase Hosting that uses the TypeScript scoring twin.
- One shared set of scoring fixtures that both engines must pass in CI.
- App-level JSON/CSV export as the ₹0 backup.

## Widget recommendation

Use a **small "Log" button that switches the widget into quick entry**, with Done and auto-return. Tapping the graph does nothing. Keep the double-tap only if your phone test meets the agreed bar. Quick entry would offer **+/− for amounts and checkboxes for yes/no habits**, which also resolves the checkbox vs +/− conflict. **These are recommendations; I'll wait for your phone results and your approval.**

## Decisions that need your approval

1. **Backend:** Firebase Spark (recommended) or Supabase Free? → [backend-comparison.md](backend-comparison.md)
2. **Widget entry method:** decide after the phone test. Double-tap if it passes the bar; otherwise the Log toggle (recommended) or the quick-entry screen.
3. **Pass bar for double-tap** (suggested): ≥19/20 recognised, 0/20 false triggers, ≥9/10 after the phone was asleep, and the delay feels fine to you.
4. **Quick-entry content:** checkboxes only, or +/− for amounts plus checkboxes for yes/no (recommended).
5. **Can the web companion log and edit**, or only view? I recommend log and edit, using the same rules.
6. **Scoring v0 placeholders.** Needed before Phase 2; the first three shape the Phase 1 data model:
   - (a) "Done" means reaching 100% of target.
   - (b) Only a day you've *reviewed* can count as "missed"; unreviewed days are "unknown".
   - (c) Rest and skipped days don't count and don't break streaks.
   - (d) Heatmap levels at score 34 and 67, with the brightest level only for a perfect day = all scheduled habits done.
   - (e) Which day an entry belongs to: the time zone you were in, with the day starting at midnight.
7. **Repository:** create `brain-rot-2.0` (private), and decide what to do with the public, empty `brainrot-2-0`.
8. **Small technical choices** (just confirm):
   - minSdk 26 (Android 8+).
   - Quick entry auto-returns to the graph after 90 s.
   - Shared JSON fixtures instead of Kotlin Multiplatform.
   - Keep Next.js for the web as the handoff says (Vite would be slightly simpler).

## Exact next steps

### For you (no Firebase/Supabase account needed)
1. **Create the repo:**
   - Go to <https://github.com/new>.
   - Owner **AdityaJha20**, name **`brain-rot-2.0`**, **Private**.
   - Leave "Add README / .gitignore / license" **unticked**, so it's empty. Click **Create repository**.
   - If the Claude GitHub App only has access to selected repositories, add this one: GitHub → Settings → Applications → Installed GitHub Apps → Claude → Configure → Repository access. (You can also manage the connection from <https://claude.ai/connect-github>.)
   - Then tell me, and I'll attach it and push the Phase 0 commit.
2. **Optional:** make `brainrot-2-0` private or delete it (repo → Settings → Danger zone).
3. **Phone test:** install `android-debug.apk` and follow [device-test-protocol.md](device-test-protocol.md). Send me the results table, screenshots, and the shared tap log.
4. **Answer the decisions above.** Short answers are fine.

### For me, after that
1. Push to `brain-rot-2.0` and confirm the CI run passes on GitHub (all jobs run without secrets).
2. Turn your phone results into the widget decision record.
3. Record the agreed decisions in [`../decisions/`](../decisions/) and update the scoring fixtures if you change any placeholder.
4. Only then propose a small Phase 1 plan (core tracker), in reviewable steps.

### Later, when you're ready for the cloud (not now)
For Firebase:
1. Go to console.firebase.google.com and create a project. Stay on **Spark** and decline any billing prompt.
2. Choose Firestore location **asia-south1 (Mumbai)**. This can't be changed later.
3. Enable **Google** sign-in.
4. Register an Android app with package `app.brainrot.phase0` (or the final package name) and the debug key's **SHA-1**. I'll give you the exact command.
5. Register a web app.

Then I run the blocked live checks: sign-in on phone and web, the `auth_time` revoke rule with real tokens, the read-budget check, and a Hosting deploy.

## Changes vs the handoff

**No product requirement was changed.** These are implementation choices, all reversible, each flagged here or in the decisions list:
- compileSdk 37 with targetSdk 36, minSdk 26.
- The widget sends a 16-bit image.
- The database uses durable sync writes (WAL with `synchronous=FULL`).
- Quick entry auto-returns after 90 s.
- Scoring placeholders are exposed as named settings rather than chosen.
- The test app has no internet permission (Phase 0 only).

The handoff's "Dashboard candidates", AI coach, goals, reminders, pixel companion and full design system are **not** part of Phase 0 and were not started.
