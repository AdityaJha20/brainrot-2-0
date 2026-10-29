# Recommended architecture (Phase 0 proposal)

**Status:** proposal for your approval. It keeps the handoff's leading recommendation (native Kotlin, Room, Firebase, Next.js) and adds what Phase 0 learned. Anything marked **(decision)** is listed in [REPORT.md](REPORT.md#decisions-that-need-your-approval).

## The big picture

```
 OPPO F29 (source of truth)                         Cloud copy (Firebase Spark, ₹0)        Browser
 ┌───────────────────────────────┐                 ┌──────────────────────────────┐      ┌─────────────────────┐
 │ App + widget + quick entry    │                 │ Firebase Auth (Google)       │      │ Web companion       │
 │   │ write                      │   sync worker   │ Firestore                    │      │ (static Next.js)    │
 │   ▼                            │ ──────────────▶ │  /users/{uid}/habits         │ ◀──▶ │  Firestore web SDK  │
 │ Room (SQLite)                  │  (when online)  │  /users/{uid}/events         │      │  + IndexedDB cache  │
 │  habits · events · sync_queue │ ◀────────────── │ Security Rules = the guard   │      │  + TS scoring twin  │
 │ Kotlin scoring engine (core)  │   pull changes  └──────────────────────────────┘      └─────────────────────┘
 └───────────────────────────────┘                    Firebase Hosting serves the web app
            ▲                                                     ▲
            └──────── shared-fixtures/scoring/*.json ─────────────┘   both engines must pass the same tests
```

## Key choices, in plain language

### 1. The phone's database is the source of truth
Every log is saved to SQLite on the phone first. The app says "saved" only after the save is on disk. The same transaction also adds an entry to a **sync queue** table. A background job later sends queued entries to the cloud when there is a connection.

*Why:* this is the handoff's offline-first requirement, and Google's own offline-first guidance. The app never depends on the network, a quota, or a cloud outage.
*Tested:* 7 database tests (restart survival, queue entries, no duplicates on retry, delete propagation, backdating, export).

### 2. Logs are append-only events
We never overwrite "today's total". Each +0.5 L is its own event, and a correction is a new event. A delete is a **tombstone**: a "deleted" flag that also syncs. Totals, scores and streaks are always recalculated from events.

*Why:* nothing is lost, every number can be explained, and two devices adding events at the same time can never conflict. Tested in the Firestore emulator: both devices' offline events survive.

### 3. Settings changes use version numbers
Habit settings (target, schedule) can be edited on the phone or the web. Each edit must carry "next version". If the phone edits version 3 offline while the web already saved version 4, the phone's edit is **rejected** and shown as a conflict, instead of silently overwriting. *Tested on both backends.*

### 4. One set of scoring rules, two languages, shared tests
The Android app runs scoring in Kotlin (`core/`); the web runs it in TypeScript (`web/scoring/`). Both must pass the same 20 JSON test cases in `shared-fixtures/scoring/`. If they ever disagree, CI fails.
*Why not compute scores in the cloud?* At ₹0 there is no server code on Firebase, and the app must score offline anyway.
*Why not one shared language?* Kotlin Multiplatform could compile Kotlin to JavaScript. That's heavier tooling for a solo project; shared fixtures give the same safety with plain tools. **(decision, low stakes)**

### 5. The scoring rules are a proposal, with every open question a named setting
The handoff leaves weights, partial credit, streak thresholds and the perfect-day definition open. The engine implements the handoff's *proposed baseline*. Each open item is a named setting with a placeholder value (see `shared-fixtures/scoring/README.md`), so nothing is locked in. **(decision)**

### 6. Widget = one image + explicit buttons
The contribution graph is drawn as one image at the widget's pixel size. Quick entry uses widget checkboxes and +/− buttons that write straight to the phone database. How you enter quick entry depends on the phone test ([widget-findings.md](widget-findings.md)). **(decision after device test)**

### 7. Web companion: static site, reads and writes the same events
- A static Next.js export (no server code), hosted on Firebase Hosting (₹0). Hosting there also avoids a known redirect sign-in problem.
- **Proposal:** the web can log and edit too, using the same events, rules and TypeScript scoring twin. The alternative is a read-only web app, which is simpler but less useful. **(decision)**
- **Read budget rule:** the web never downloads all history on each visit. It keeps an IndexedDB cache and asks only for "changed since last sync". This keeps far inside the 50,000 reads/day free limit.
- Note: the handoff names Next.js. A plain Vite + React app would be slightly simpler for a static-only site. I'm keeping Next.js unless you prefer otherwise.

### 8. Backup and export
- **JSON** = full backup: habits, all events including tombstones, and notes. It round-trips with no loss, and a re-imported file produces identical scores (tested).
- **CSV** = log rows for spreadsheets. Text that a spreadsheet could run as a formula is neutralised (tested).
- Firebase's managed backups need billing, so at ₹0 **backup = export from the phone**. That costs zero cloud reads and works offline. A later option is exporting to your own Google Drive.
- **Honest limits:**
  - Anything logged on a lost phone that never synced is gone.
  - Data deleted in the cloud can remain in Google's own backups for up to about 6 months (Google Cloud's stated deletion timeline). We won't claim instant deletion everywhere.

### 9. Security and privacy
- Every cloud record lives under `/users/{your uid}`. Rules refuse everyone else, including signed-out requests (tested).
- No secrets in the app or web bundle. Firebase web config is public by design; the rules are the guard. A secret scan (gitleaks) runs in CI.
- **Sign out a lost phone at ₹0:** sign in again on a trusted device, then "revoke older sign-ins". The rule logic is tested in the emulator; whether real tokens carry the needed `auth_time` claim still has to be confirmed in a real project.
- **AI stays optional and is not part of Phase 0–4.** When it's added, the key must not ship in the app. That needs a decision on where it runs (see the comparison doc).

### 10. Cost model (₹0, with honest limits)

| Item | Free allowance (checked 29 Oct 2026) | Our expected use |
|---|---|---|
| Firestore writes | 20,000/day | ~10–100/day |
| Firestore reads | 50,000/day | a few hundred/day with the "changed since" rule |
| Firestore storage | 1 GiB | ~10–50 MB after years |
| Firebase Hosting | 10 GB storage; transfer limit stated as 10 GB/month or 360 MB/day (the official pages differ) | a few MB |
| GitHub Actions (private repo, Free) | about $12 of compute/month ≈ 2,000 Linux minutes; blocked, not billed, when used up | ~8–12 min per push for the full CI |
| GitHub artifact storage | 500 MB | APKs kept 14 days |

Rules we follow: never link a billing account; never enable products that ask for one (Storage, Functions, backups, App Hosting); recheck pricing before release.
