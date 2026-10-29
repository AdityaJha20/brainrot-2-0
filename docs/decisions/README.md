# Decision log

Each decision has a status: **Proposed** (waiting for the owner's approval), **Accepted**, or **Superseded**.
Nothing becomes Accepted without the owner's explicit approval. Rationale and evidence live in `docs/phase0/`.

| ID | Decision | Status | Evidence / rationale |
|---|---|---|---|
| D-001 | The phone's Room/SQLite database is the source of truth; the cloud holds copies via a durable sync queue | Proposed | Handoff §6–7 (offline-first); `RoomPersistenceTest`; [architecture](../phase0/architecture.md) §1 |
| D-002 | Activity is stored as append-only events; deletes are tombstones; totals are always recalculated | Proposed | Handoff §7 ("prefer append-only activity events"); Firestore + RLS tests |
| D-003 | Editable settings use version numbers; stale edits are rejected as conflicts | Proposed | Handoff §7 ("never silently merge different targets"); emulator + Postgres tests |
| D-004 | Backend: Firebase Spark (no billing account ever linked) | Proposed | [backend-comparison](../phase0/backend-comparison.md) |
| D-005 | One scoring spec, implemented in Kotlin and TypeScript, both gated by `shared-fixtures/scoring` | Proposed | 20/20 fixtures pass in both engines |
| D-006 | Scoring v0 placeholders (threshold 1.0, reviewed-only "missed", rest/skip not eligible, heatmap bins 34/67, perfect day = all eligible completed, event-zone day assignment, day starts 00:00) | Proposed — **owner input needed** | [fixtures README](../../shared-fixtures/scoring/README.md) |
| D-007 | Contribution graph rendered as a single bitmap (16-bit in the widget) | Proposed | Glance 10-children limit; `HeatmapRendererTest` |
| D-008 | Widget entry: "Log" toggle button (double-tap only if the phone test passes the agreed bar) | Proposed — **waiting for OPPO F29 test** | [widget findings](../phase0/widget-findings.md) |
| D-009 | Quick entry shows +/- for amounts and checkboxes for yes/no habits | Proposed — **waiting for owner preference** | Both variants built for comparison |
| D-010 | Web companion: static Next.js export on Firebase Hosting; can log and edit; reads only "changed since last sync" | Proposed | [architecture](../phase0/architecture.md) §7 |
| D-011 | Backup at ₹0 = app-level JSON (full) + CSV (logs) export from the phone | Proposed | `ExportTest`, `RoomPersistenceTest.exportFromTheDeviceDatabaseRoundTrips` |
| D-012 | Tooling: Gradle 9.6 / AGP 9.5 / Kotlin 2.4, compileSdk 37, targetSdk 36, minSdk 26; ktlint (official style); Node built-in test runner; gitleaks; GitHub Actions without secrets | Proposed | Builds and tests pass locally |
