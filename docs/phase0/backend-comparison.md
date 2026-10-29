# Firebase vs Supabase for Brain Rot 2.0

**Date checked:** 29 Oct 2026. **Status:** recommendation for your approval, not a final decision.

Every factual claim below comes from the sourced research notes in [`research/`](research/). Those notes give the exact URL and a short quote for each claim, and mark each one VERIFIED (read on an official page that day) or INFERRED (reasoning). Free tiers and prices change without notice. **Nothing here promises that either service will stay free.** Recheck the pricing pages before any release.

## What both options look like in our design

Neither backend is the source of truth. The phone's own SQLite database (Room) is. The phone records every log locally first and queues a sync operation. The backend only receives copies, so the app keeps working if the backend is down, over quota, or paused. That design is already prototyped and tested (see [REPORT.md](REPORT.md)).

So "which backend" really means: where do copies go, how does the web companion read and write them, and who enforces "only I can see my data"?

## Side-by-side

Legend: ✅ good fit · ⚠️ workable with care · ❌ not available at ₹0

| Requirement | Firebase (Spark, no billing) | Supabase (Free plan) |
|---|---|---|
| **Payment method needed?** | ✅ No. Spark needs "no payment information". Billing only starts if you link a billing account, which needs a card. | ⚠️ Probably not. "You will not be charged while using the Free Plan" is verified, but no official page says outright that sign-up needs no card. |
| **What happens over the limit** | ✅ The product is switched off, not billed. ⚠️ Firebase's own pages disagree on whether that lasts until the daily reset or for the rest of the month. | ✅ Not billed: notice, then a grace period, then restrictions (`402` errors or read-only). |
| **Goes to sleep if unused** | ✅ No inactivity pausing found. | ❌ **Paused after about 1 week without "sufficient" database activity.** You restore it by hand in the dashboard, and data is kept for up to 1 year. A week without the phone syncing (holiday, lost phone) means the project stops until you resume it. |
| **Free capacity vs our needs** (≈10–50 events/day) | ✅ 1 GiB storage, 20,000 writes and 50,000 reads per day. ⚠️ Reads are the only real risk: a web page that re-downloads all history on every load could use the whole day's reads. Design rule: read only what changed since the last sync. | ✅ 500 MB database, 5 GB transfer. Five years of logs is an estimated 20–50 MB. |
| **Android sign-in with Google** | ✅ Official Credential Manager → Firebase Auth flow. You register the SHA-1 fingerprint of each signing key; no Play Store needed (inferred). | ✅ Official Credential Manager → `signInWithIdToken` guide, with nonce. ⚠️ The Kotlin library (supabase-kt) is still community-maintained, "working towards" official status, so a package rename is expected. |
| **Web sign-in** | ✅ Works. ⚠️ Redirect sign-in breaks in modern browsers unless the site is on Firebase Hosting (`authDomain` = your `web.app` domain) or uses a popup. | ✅ Standard OAuth redirect (PKCE). Works from any static host. |
| **"Only I can see my data"** | ✅ Security Rules. **Tested locally:** 14 tests pass in the official emulator. | ✅ Row Level Security in Postgres. **Tested locally:** 8 tests pass in PostgreSQL 16. |
| **Sign out a lost phone** | ⚠️ No server code at ₹0. **A ₹0 rule-based approach was tested in the emulator** (re-sign in on a trusted device, which revokes older sign-ins). It still needs a live check that real tokens carry `auth_time`. The Admin SDK can also revoke from a local script. | ✅ Built in: `signOut(scope = global)`. ⚠️ Kotlin defaults to *local* only, and access tokens stay valid for up to 1 hour. |
| **Offline on Android** | ✅ The Firestore SDK has an offline cache and queue that survives restarts. Since Room is our source of truth, we use it mainly as transport. | ⚠️ No official offline mode. Fine for us, because Room plus our own queue already does this (tested). |
| **Offline on the web companion** | ✅ Opt-in IndexedDB cache with pending writes. **Tested in the emulator:** offline write → reconnect → visible on another client. | ⚠️ None built in. Online-only, or we build our own browser queue. |
| **Edit conflicts** (phone and web change the same setting) | ✅ Default is last write wins. Our rules require each edit to carry the next version number, so a stale edit is **rejected and rolled back** instead of silently overwriting. **Tested.** | ✅ Same approach with a database trigger. **Tested.** |
| **Server-side code** (e.g. a future AI proxy) | ❌ Cloud Functions and Cloud Run need Blaze, a paid plan with a card. An AI proxy would need another free host (e.g. Cloudflare Workers), or your own API key kept on your device. | ✅ Edge Functions: 500,000 invocations included. |
| **Backups at ₹0** | ❌ Scheduled backups, point-in-time recovery and managed export all need billing. ✅ App-level JSON/CSV export (built and tested) costs zero cloud reads. | ❌ No backups on Free. ✅ Self-export with `pg_dump` (e.g. a nightly GitHub Actions job holding a database password secret) plus the same app export. |
| **Data modelling / migrations** | ⚠️ Schemaless documents: shapes are enforced in Security Rules, and migrations happen in app code (no server code at ₹0). | ✅ Real SQL: tables, constraints, foreign keys, migration files. Better for reporting queries. |
| **Testing without an account** | ✅ Emulator with a `demo-` project needs no Google account. Used here. | ⚠️ The official local stack needs Docker (an experimental native runtime exists). We tested the policies in plain PostgreSQL with a small stand-in for `auth.uid()`. |
| **Web hosting at ₹0** | ✅ Firebase Hosting on the same project, no card. It also fixes the redirect sign-in issue. | ✅ Any static host (Cloudflare, Vercel Hobby for personal use). Note: GitHub Pages can't serve from a private repo on GitHub Free. |
| **Privacy / AI training** | ✅ Google Cloud terms: no use of customer data for ads, and no AI training "without Customer's prior permission". ✅ Firestore data can stay in Mumbai (asia-south1). ⚠️ **Firebase Auth processes data in the US only** (your email and Google identity). ⚠️ Firebase's terms acceptance says use is for "trade, business, craft, or profession". | ✅ Terms: no AI training on customer data "without Customer's prior written consent". ✅ Mumbai region (ap-south-1). ⚠️ Don't paste personal data into the dashboard's AI assistant (it goes to third-party models). |
| **Solo-developer upkeep** | ✅ Low. Watch read usage, and never click anything that links billing (enabling Storage, Functions or backups all ask to). Don't take the optional "Identity Platform" upgrade. | ⚠️ Medium. Keep it from pausing, run your own backups, keep RLS tests up to date, and track the Kotlin library's move to official. |
| **Surprise-cost risk if you ever upgrade** | ⚠️ On Blaze, budget alerts "do not pause services", and Firestore can't be spend-capped. Stay on Spark. | ✅ Pro has a spend cap on by default, but compute and add-ons are excluded. Not relevant while on Free. |

## Recommendation (needs your approval)

**I provisionally recommend Firebase (Spark plan, no billing account ever linked)**. Room on the phone stays the source of truth, and the web companion is hosted on Firebase Hosting.

Why, in plain terms:
1. **It doesn't fall asleep.** For a personal tool you might not open for a week, Supabase's 7-day pause is the biggest practical risk. Firebase has no equivalent.
2. **The web companion gets offline support for free** (tested), and sign-in, database and hosting live in one place, which avoids the redirect sign-in pitfall.
3. **It fits the ₹0 rule cleanly:** there is no card to add, and over-limit means "switched off", not "billed".
4. Its security model (rules) is **already tested locally**, including stale-edit rejection and a ₹0 "sign out other devices" rule.

What you give up, and how we cover it:
- **No backups at ₹0.** We cover it with app-level JSON export from the phone (built and tested) and a reminder to export regularly. Optionally, later, export to your Google Drive.
- **No server code at ₹0.** Not needed for core tracking. If hosted AI is ever added (Phase 5), it needs a separate decision: a Cloudflare Worker (free tier), your own API key on-device, or no hosted AI.
- **Your Google identity is processed in the US** by Firebase Auth. Habit data itself can stay in India.
- **Schemaless data.** We use a `schemaVersion` field on documents and keep rules strict.

**When Supabase would be the better choice:** you want SQL reporting or server-side logic on the free tier (e.g. an AI proxy via Edge Functions), and you're comfortable with the weekly-activity requirement or a keep-alive job.

## What still needs a real cloud project (blocked until you're ready)

Nothing below has been tested. No cloud project exists.

| Check | Why it can't be done locally |
|---|---|
| Google sign-in on the OPPO F29 and in a browser | Needs real OAuth client IDs, plus the SHA-1 of the debug key registered in a project |
| Real tokens contain `auth_time`, so the revoke rule works in production | The emulator uses whatever claims we give it |
| Real quota behaviour (reads used by a web reload, the over-limit response) | The emulator has no quotas |
| Deploying the web companion to Firebase Hosting | Needs a project |
| Supabase pause/resume behaviour (only if Supabase is chosen) | Needs a project |

The exact steps for when you are ready are in [REPORT.md § Next steps](REPORT.md#exact-next-steps).
