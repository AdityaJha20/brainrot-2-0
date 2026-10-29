# Supabase + free static hosting + GitHub Actions: research for "Brain Rot 2.0"

**Date checked:** 2026-10-29 (all pages fetched on this date)
**Method:** WebFetch/WebSearch of official vendor pages. WebFetch returns text through a summarizing model, so a "quote" is the short phrase that tool returned as verbatim. Re-check numbers on the live page before you rely on them.
**Tags:**
- `[VERIFIED url "quote"]` means I read it on an official page today.
- `[INFERRED]` means my own reasoning or arithmetic, not stated on a page.
- `[UNVERIFIED]` means the official page didn't state it, or I saw it only in a search-result summary.

---

## Part A: Supabase

### A1. Free plan limits, payment method, overage behaviour

| Item | Free plan value | Tag |
|---|---|---|
| Database size | "500 MB database size" per project; shared CPU, 500 MB RAM | [VERIFIED https://supabase.com/pricing "500 MB database size"] |
| Disk | "Free Plan projects have a fixed disk size of 2 GB. The disk does not autoscale." | [VERIFIED https://supabase.com/docs/guides/platform/database-size "fixed disk size of 2 GB"] |
| Egress | "5 GB egress" + "5 GB cached egress" | [VERIFIED https://supabase.com/pricing "5 GB egress"] |
| MAU | "50,000 monthly active users" | [VERIFIED https://supabase.com/pricing "50,000 monthly active users"] |
| Projects | "Limit of 2 active projects"; "Paused projects do not count towards your free project limit." FAQ: "you can have as many paused projects as you want" | [VERIFIED https://supabase.com/pricing "Limit of 2 active projects"] [VERIFIED https://supabase.com/docs/guides/platform/billing-on-supabase "Paused projects do not count towards your free project limit"] |
| Edge Function invocations | "500,000 included" | [VERIFIED https://supabase.com/pricing "500,000 included"] |
| File storage | "1 GB file storage" | [VERIFIED https://supabase.com/pricing "1 GB file storage"] |
| Realtime | 200 concurrent peak connections; "2 Million included" messages/month | [VERIFIED https://supabase.com/pricing "2 Million included"] |
| Realtime rates | Concurrent connections "200"; messages/sec "100"; channel joins/sec "100"; broadcast payload "256 KB"; Postgres-changes payload "1,024 KB" | [VERIFIED https://supabase.com/docs/guides/realtime/limits "200" / "100"] |
| Log retention | 1 day (API & DB) | [VERIFIED https://supabase.com/pricing (table) "1 day"] |
| Backups / PITR | Automatic backups "Not included"; PITR "Not included" | [VERIFIED https://supabase.com/pricing "Not included"] |
| Auth rate limits (all plans, defaults) | Token endpoint (refresh, ID token, PKCE): "150 requests per 5 minutes, with bursts up to 30 requests". Sign-up/sign-in: "30 requests per 5 minutes". Built-in email: "2 emails per hour with the built-in email provider" | [VERIFIED https://supabase.com/docs/guides/auth/rate-limits "150 requests per 5 minutes"] |
| Data API (PostgREST) rate limit | No official page found. `/docs/guides/api/rate-limits` returned 404 today. | [UNVERIFIED] |

**Payment method:**
- None of these pages says whether the Free plan needs a card: the pricing page, the billing pages and the Billing FAQ. The Billing FAQ says only "We accept credit card payments only." [VERIFIED https://supabase.com/docs/guides/platform/billing-faq "We accept credit card payments only"]
- I read nothing on an official page that requires a card for Free, but I also found no explicit "no card needed" statement. [UNVERIFIED]
- On charges: "you will not be charged while using the Free Plan". [VERIFIED https://supabase.com/docs/guides/platform/cost-control "you will not be charged while using the Free Plan"]

**What happens when you exceed limits:**
- **Notice, grace period, then restriction.** "You will be notified when you exceed the Free Plan quota." A grace period applies, and after it "service restrictions will apply". Possible restrictions include pausing projects, read-only databases and disabled launches or transfers. Restricted orgs may get a "402 status code" on all API requests. A later overage gets no new grace period. [VERIFIED https://supabase.com/docs/guides/platform/billing-faq "service restrictions will apply" / "402 status code"]
- **Restrictions lift at the next cycle.** They end when the quota refills at the next billing cycle, or immediately on upgrade. [UNVERIFIED: search-result summary of the Billing FAQ]
- **The 500 MB DB quota does not trigger read-only mode.** "The 500 MB database size quota does not trigger it". Exceeding it can instead bring a Fair Use restriction (`402`), judged on the average daily DB size over the billing period. [VERIFIED https://supabase.com/docs/guides/platform/database-size "does not trigger it"]
- **Read-only mode is about the 2 GB disk.** "When disk usage reaches 95%, Supabase puts your database into read-only mode" (SQLSTATE 25006). It turns off automatically below 95%. A full disk shuts Postgres down until you upgrade. [VERIFIED https://supabase.com/docs/guides/platform/database-size "When disk usage reaches 95%"]
- **Fit for this project.** 50 events/day × 365 × 5 years ≈ 91k rows. At roughly 200–500 bytes/row with indexes, that is about 20–50 MB, so 500 MB is far away. Egress and MAU are negligible. [INFERRED]

### A2. Inactivity pausing (Free plan)

- **Rule:** "A Free plan project is considered inactive if it does not receive sufficient user database activity over the past week." [VERIFIED https://supabase.com/docs/guides/platform/free-project-pausing "sufficient user database activity over the past week"]
- **Pricing page wording:** "Free projects are paused after 1 week of inactivity." [VERIFIED https://supabase.com/pricing "paused after 1 week of inactivity"]
- **What counts as activity:** "Typically a few user requests to the database each day over the previous week is enough." Dashboard visits and API calls from a connected app also count. [VERIFIED https://supabase.com/docs/guides/platform/free-project-pausing "a few user requests to the database each day"]
- **Emails:** "A warning email roughly one week before the pause takes effect" and "A confirmation email once the project has been paused." [VERIFIED same URL]
- **Restore:** In the Dashboard, open the paused project and click "Resume project". "The project will return to its previous state, including data and configurations." [VERIFIED same URL "Resume project"]
- **Restore window is 1 year now.** "Once the project is paused, there is a 1-year window to restore the project on the platform from within Supabase Studio." The stated reason: "backups are only retained for a limited period." [VERIFIED same URL "1-year window to restore"]
- **History of the window.** Changelog: "Beginning June 24, 2024", paused Free projects "are restorable for 90 days following their pause date". After that, restore is replaced by a download of the latest logical backup plus Storage objects. [VERIFIED https://supabase.com/changelog/27497-paused-free-plan-projects-are-restorable-for-90-days "restorable for 90 days"]
- **So can a project paused for more than 90 days be restored?**
  - Per today's docs, yes, up to 1 year. [VERIFIED free-project-pausing page]
  - Beyond 1 year, the current page doesn't say whether a download is still offered. [UNVERIFIED]
- **Keep-alive pings.** The pausing page says nothing on cron or keep-alive pings. A Room→Supabase sync that runs daily would itself be DB activity. On days the owner doesn't use the app there is no activity, so a 7-day gap (holiday, phone lost) would pause the project. [INFERRED]
- **Paid plans:** "Paid projects cannot be paused and are not subject to pausing for inactivity." [VERIFIED free-project-pausing page]
- **Outage note:** A Supabase status incident (search result) reported an outage on 21 Oct 2026 affecting pause and restore in all regions. [UNVERIFIED: search-result summary only, https://status.supabase.com/incidents/01M5FHP5PVQBY4NVTBBH9XG0CH]

### A3. Backups, PITR, self-backup, deletion

- **No automatic daily backups on Free.**
  - Daily backups are listed only for paid plans: "We automatically back up all Pro, Team, and Enterprise Plan projects on a daily basis." Pro keeps 7 days, Team 14 days, Enterprise up to 30 days. [VERIFIED https://supabase.com/docs/guides/platform/backups "back up all Pro, Team, and Enterprise Plan projects"]
  - The pricing page says "Not included" for Free. [VERIFIED https://supabase.com/pricing]
- **PITR is a paid add-on.** "Pro, Team and Enterprise Plan projects can enable PITR as an add-on". It needs "at least a Small compute add-on", at about $100/mo for 7 days of retention. [VERIFIED https://supabase.com/docs/guides/platform/backups "enable PITR as an add-on"]
- **Self-export is the official advice for Free.** "We recommend that free tier plan projects regularly export their data" using the CLI `db dump`, and keep off-site backups. [VERIFIED https://supabase.com/docs/guides/platform/backups "free tier plan projects regularly export their data"]
- **CLI `supabase db dump`:**
  - It "Runs `pg_dump` in a container with additional flags to exclude Supabase managed schemas."
  - Flags: `--data-only`, `--role-only`, `--linked`, `--db-url`, `-s/--schema`, `-f/--file`. [VERIFIED https://supabase.com/docs/reference/cli/supabase-db-dump "Runs pg_dump in a container"]
- **Official backup recipe:**
  ```
  supabase db dump --db-url [CONNECTION_STRING] -f roles.sql --role-only
  supabase db dump --db-url [CONNECTION_STRING] -f schema.sql
  supabase db dump --db-url [CONNECTION_STRING] -f data.sql --use-copy --data-only
  ```
  Restore uses `psql --single-transaction ... SET session_replication_role = replica ...`. Prerequisites include "Install Docker Desktop for your platform." [VERIFIED https://supabase.com/docs/guides/platform/migrating-within-supabase/backup-restore "Install Docker Desktop"]
- **Docker vs plain `pg_dump` (Docker inferred).** The CLI dump needs Docker or a compatible container runtime. A plain `pg_dump` (matching the major Postgres version) against the direct or session-pooler connection string avoids Docker. That makes a nightly GitHub Actions `pg_dump` job a cheap off-site backup; store the output as an encrypted artifact or commit it to a private repo. [INFERRED]
- **IPv6 caveat:** Use the direct connection string. If your network has no IPv6 and you lack the IPv4 add-on, use the Session pooler string. [VERIFIED backup-restore page "Session pooler"]
- **Storage objects are not in DB backups.** "Database backups do not include objects you store via the Storage API". [VERIFIED backups page]
- **Deletion is permanent.** "When you delete a project, we permanently remove all associated data, including any backups stored in S3." [VERIFIED https://supabase.com/docs/guides/platform/backups "permanently remove all associated data"]

### A4. Auth: Google sign-in, sessions, sign-out

**Android native (Credential Manager plus ID token):**
- The guide says: "implement native Google sign-in with Supabase using Credential Manager in your Android app." [VERIFIED https://supabase.com/docs/guides/auth/social-login/auth-google "using Credential Manager"]
- Dependencies are `androidx.credentials:credentials` and `com.google.android.libraries.identity.googleid:googleid`. Code uses `GetGoogleIdOption.Builder().setServerClientId("WEB_GOOGLE_CLIENT_ID")`. [VERIFIED same URL]
- **Nonce:**
  - "Providing a nonce is optional but recommended."
  - Pass the hashed (SHA-256 hex) nonce to Google via `.setNonce(hashedNonce)`.
  - Pass the raw nonce to Supabase: `supabase.auth.signInWith(IDToken) { ... nonce = rawNonce }`.
  - "you need to provide a hashed version to Google and a non-hashed version to signInWithIdToken".
  - "By default, Supabase Auth implements nonce validation". A "Skip nonce check" option exists for some native setups. [VERIFIED same URL "hashed version to Google"]
- **Client IDs:**
  - "You have to create OAuth client IDs for both a Web and Android application". The Android one needs "the SHA-1 certificate fingerprint used to sign your Android app".
  - "The Web client ID is the one used in your Android app." Register it in the Dashboard's Google provider page. [VERIFIED same URL]
  - Debug and release keystores (and Play App Signing) have different SHA-1s, so each needs registering. [INFERRED]
- **compose-auth alternative:** `install(ComposeAuth) { googleNativeLogin("WEB_GOOGLE_CLIENT_ID") }`. "On Android it uses the system's Credential Manager." [VERIFIED same URL "googleNativeLogin"]

**supabase-kt status:**
- Kotlin reference docs: "The Kotlin client library is created and maintained by the Supabase community, and is not an official library." [VERIFIED https://supabase.com/docs/reference/kotlin/introduction "not an official library"]
- The GitHub README (github.com/supabase-community/supabase-kt) has a notice: "We are working towards making supabase-kt official". It warns PR reviews may be slower during a namespace and repo transfer. [VERIFIED https://github.com/supabase-community/supabase-kt "working towards making supabase-kt official"]
- Modules: `auth-kt` (formerly `gotrue-kt`), `postgrest-kt`, `functions-kt`, `storage-kt`, `realtime-kt`, `compose-auth`, `compose-auth-ui`.
- Requirements: Android minSdk 26 (lower needs core library desugaring). You must add a Ktor client engine.
- No offline support is mentioned. [VERIFIED same URL]
- Releases page shows 3.9.0-beta-1 (12 Oct), 3.8.0 (26 Aug), 3.7.0 (20 Jul). The year isn't shown; presumably 2026. [VERIFIED https://github.com/supabase-community/supabase-kt/releases "3.8.0"; year INFERRED]
- **Planning implication:** Pin a stable version, not the beta. Expect the Maven group or namespace to change soon. [INFERRED]

**Web Google sign-in:**
- OAuth redirect: "In the browser, signInWithOAuth automatically redirects to the OAuth provider's authentication endpoint". [VERIFIED auth-google page]
- `signInWithOAuth` "supports the PKCE flow". [VERIFIED https://supabase.com/docs/reference/javascript/auth-signinwithoauth "supports the PKCE flow"]
- PKCE code exchange "must be initiated on the same browser and device where the flow was started". The code verifier is "stored locally". [VERIFIED https://supabase.com/docs/guides/auth/sessions/pkce-flow "same browser and device"]
- Works on a purely static site (client-side exchange). [INFERRED]
- Google One Tap / button: the callback calls `supabase.auth.signInWithIdToken({ provider: 'google', token: response.credential })`. [VERIFIED auth-google page]
- **Redirect allow-list:** "The URL in redirectTo should match the Redirect URLs list configuration". Wildcards are allowed but exact paths are recommended for production. [VERIFIED https://supabase.com/docs/guides/auth/redirect-urls "should match the Redirect URLs list"]
- **Consent screen:** Without a Supabase custom domain (a paid add-on), users see "`<project-id>.supabase.co` which does not inspire trust". [VERIFIED auth-google page "does not inspire trust"] For a single-user personal app this is cosmetic. [INFERRED]

**Sessions:**
- Access JWT defaults to 1 hour. Refresh tokens don't expire but are single-use, with a 10-second reuse interval. [VERIFIED https://supabase.com/docs/guides/auth/sessions "default expiration time of 1 hour"]
- Time-boxed or inactivity-timeout sessions: "This feature is only available on Pro Plans and up." [VERIFIED same URL]
- The sessions page does not state the default browser storage. supabase-js normally uses localStorage. [UNVERIFIED]
- On Android, store the session in encrypted storage. The supabase-kt default SessionManager is not described on the pages I read. [UNVERIFIED]

**Sign-out and revocation:**
- Scopes: "`global` (default)", "`local`", "`others`".
- **Important:** "JavaScript, Swift, Python, and C# default to the global scope. Dart and Kotlin default to local." So on Android you must pass `SignOutScope.GLOBAL` explicitly for "sign out everywhere".
- Revocation destroys refresh tokens, but "Access Tokens of revoked sessions remain valid until their expiry time". [VERIFIED https://supabase.com/docs/guides/auth/signout "Dart and Kotlin default to local"]

### A5. Row Level Security

- **Per-user policy pattern** (official example):
  ```sql
  create policy "..." on t for select to authenticated using ( (select auth.uid()) = user_id );
  -- insert: with check ( (select auth.uid()) = user_id );
  -- update: using (...) with check (...);
  -- delete: using (...);
  ```
  [VERIFIED https://supabase.com/docs/guides/database/postgres/row-level-security "(select auth.uid()) = user_id"]
- Wrapping in `select` "causes an initPlan to be run by the Postgres optimizer". [VERIFIED same URL]
- "Always name the role a policy applies to, using the to clause." [VERIFIED same URL]
- "Add an index on every column your policies filter on." [VERIFIED same URL]
- `auth.uid()`: "Returns the ID of the user making the request". It returns `null` when unauthenticated. [VERIFIED same URL]
- **How `auth.uid()` is derived.** SQL in supabase/auth migration `20211202183645_update_auth_uid.up.sql`:
  `nullif(coalesce(current_setting('request.jwt.claim.sub', true), (current_setting('request.jwt.claims', true)::jsonb ->> 'sub')), '')::uuid`
  [VERIFIED https://github.com/supabase/auth/blob/master/migrations/20211202183645_update_auth_uid.up.sql "current_setting('request.jwt.claims', true)::jsonb ->> 'sub'"]
  PostgREST sets these GUCs from the verified JWT. [INFERRED/UNVERIFIED: GitHub discussion, not docs]
- **Service key bypasses RLS.** "A secret key authorizes access through the service_role Postgres role, which has the bypassrls attribute". Never ship it in the app or web bundle. [VERIFIED RLS page]
- **Don't authorize on user_metadata.** "this information can be modified by authenticated end users". Use `raw_app_meta_data` instead. [VERIFIED RLS page]
- **Testing RLS locally:**
  - "All sql files use pgTAP as the test runner". Run with `supabase test db`, tests live in `./supabase/tests/database`. [VERIFIED https://supabase.com/docs/guides/database/testing "supabase test db"]
  - To impersonate a user, the local testing overview sets `set local role authenticated;` and `set local request.jwt.claim.sub = '<uuid>';`. Its troubleshooting section references `request.jwt.claims`. [VERIFIED https://supabase.com/docs/guides/local-development/testing/overview "set local role authenticated"]
  - Because `auth.uid()` reads either GUC, both work. [INFERRED from migration SQL]
- **Local stack:**
  - "That stack runs in Docker containers, so you need a container runtime installed first". Podman, Rancher Desktop, colima and OrbStack are listed as alternatives.
  - There is also "the experimental native runtime" that "runs each service as a process instead". It "supports Linux and macOS on Apple silicon". [VERIFIED https://supabase.com/docs/guides/local-development/cli/getting-started "experimental native runtime"]
  - CI example: `supabase start` then `supabase test db`. [VERIFIED testing overview page]

### A6. Offline-first and sync

- **No first-party offline mode.** Supabase blog (2025-10-08, Triplit joins Supabase): "solving offline mode in a way that works for everyone is a formidable challenge". "Our focus isn't to directly integrate Triplit into our platform". Supabase aims to be "an excellent partner to other syncing systems such as ElectricSQL, Zero, and PowerSync". [VERIFIED https://supabase.com/blog/triplit-joins-supabase "formidable challenge"]
- supabase-kt README does not mention offline support. [VERIFIED github README]
- **So the planned Room + durable outbox + pull-by-`updated_at` design is the expected route.** [INFERRED]
- **PowerSync** (Supabase partner listing):
  - "PowerSync connects to Supabase in a non-invasive way (no schema changes or write permissions required)". Writes go to "an upload queue". "integrates natively with Supabase Auth". "can be self-hosted and is also available as a hosted service (PowerSync Cloud) with a Free plan". [VERIFIED https://supabase.com/partners/integrations/powersync "upload queue"]
  - PowerSync Free: "Up to 2 GB data synced / month", "Up to 50 peak concurrent clients", "Up to 500 MB of data hosted on PowerSync Service", "No credit card required". [VERIFIED https://www.powersync.com/pricing "No credit card required"]
  - **But:** "Free projects are deactivated after 1 week of inactivity" (elsewhere on the page "longer than 2 weeks"; inconsistent). [VERIFIED same URL]
  - Kotlin SDK listed. [VERIFIED same URL "Kotlin"]
  - PowerSync replaces Room with its own SQLite layer. That is a second inactivity-pausing service. [INFERRED]
- **ElectricSQL** (now electric.ax):
  - Pay-as-you-go: "Under $5/mo waived"; reads and egress "ALWAYS FREE".
  - "You can sign up and start building without a payment method." [VERIFIED https://electric.ax/pricing "without a payment method"]
  - No native Kotlin/Android client was mentioned on the pricing page (footer: TanStack DB, PGlite). [UNVERIFIED for Kotlin; INFERRED to be TS/web-focused]
- **Other partners** listed in the catalog: RxDB, WatermelonDB (blog), Brick (Flutter blog). [VERIFIED via search listing of https://supabase.com/partners/catalog/rxdb and blog URLs; not opened individually]
- **Realtime** is not an offline sync. Its Free limits are in the A1 table. Postgres-changes payloads above the limit truncate fields larger than 64 bytes. [VERIFIED https://supabase.com/docs/guides/realtime/limits "64 bytes"] For a single user, polling or pull-on-resume is enough; Realtime is optional. [INFERRED]

### A7. Migrations and data modelling

- **Commands:** `supabase migration new <name>`, `supabase db diff -f <name>`, `supabase link`, `supabase db push`, `supabase db reset`. Migrations live in `supabase/migrations`, seed in `supabase/seed.sql`. [VERIFIED https://supabase.com/docs/guides/deployment/database-migrations "supabase db push"]
- `supabase init` creates a folder "which is safe to commit to version control". [VERIFIED getting-started page]
- **Relational fit:** It is plain Postgres, so FK, CHECK, UNIQUE, partial unique indexes and generated columns are all available. That suits habits, log_events and an idempotency key on client-generated UUIDs. [INFERRED]
- `supabase db push` does not need Docker. `db diff` and `db reset` use the local stack (Docker or the native runtime). [INFERRED]

### A8. Data privacy

- **DPA:** "supplements and forms part of the Supabase Terms of Service"; "Supabase acts as a processor/service provider, and Customer as controller/business". "Version 1 — August 1, 2026". There is no separate signature: "acceptance of the Agreement shall have the same effect as signing the SCCs". [VERIFIED https://supabase.com/legal/customer-resources/data-processing-addendum "forms part of the Supabase Terms of Service"]
- **AI training (Terms §8(b)):** "Supabase will not use, nor allow any third-party to use, Customer Data, Customer's AI Input, or any AI Output to train, fine-tune, or otherwise improve any artificial intelligence or machine learning model, without Customer's prior written consent." [VERIFIED https://supabase.com/terms "will not use, nor allow any third-party to use, Customer Data"]
- **Privacy policy:**
  - Supabase processes Customer Data "primarily as a processor". "We will not access the content of the databases itself" (EEA section). [VERIFIED https://supabase.com/privacy "primarily as a processor"]
  - Inputs to Supabase's AI support or chat tools are collected and may go to OpenAI or Amazon Bedrock. Avoid pasting personal data into the dashboard AI assistant. [VERIFIED same URL "inputs you provide to our AI-powered support tools"]
- **Region:** "South Asia (Mumbai), ap-south-1" is available. [VERIFIED https://supabase.com/docs/guides/platform/regions "South Asia (Mumbai)"] The page doesn't say whether region can change after creation; assume no and choose at creation. [UNVERIFIED/INFERRED]

### A9. Solo-dev operational complexity and cost risk

- **The Free plan cannot bill.** "you will not be charged while using the Free Plan". [VERIFIED cost-control page] Worst case is restriction (402, read-only, pause), not a bill. [INFERRED]
- **Spend cap on Pro:**
  - "Spend caps are on by default on the Pro Plan". [VERIFIED https://supabase.com/pricing]
  - It does not cover "Compute", "Custom Domain", "IPv4 address", "Point-in-Time-Recovery", "Log Drain" and others. [VERIFIED https://supabase.com/docs/guides/platform/cost-control "Usage items that are predictable and explicitly opted into by the user are excluded"]
  - Compute is billed per project: "Each project you launch increases your monthly Compute costs." [VERIFIED billing-on-supabase page]
  - Pro is "from $25/month", Micro compute is "$10", and Pro includes "$10/month in compute credits". [VERIFIED https://supabase.com/pricing]
  - None of this applies while on Free. [INFERRED]
- **Billing FAQ:** "When an invoice becomes overdue, we will pause your projects and downgrade your organization to the Free Plan." [VERIFIED billing-faq] Irrelevant if no card is ever added. [INFERRED]
- **Solo-dev chores:** keep-alive or usage within 7 days; your own backups (no Free backups); RLS tests; Google client-ID and SHA-1 setup; Docker or native runtime for local tests; watching for supabase-kt namespace changes. [INFERRED]

---

## Part B: Free static hosting for a Next.js `output: 'export'` site from a private GitHub repo

**Common point.** Supabase Auth's `signInWithOAuth` redirects to `<ref>.supabase.co/auth/v1/...` and back to your allow-listed URL. It doesn't rely on a third-party-storage iframe, so the Firebase-specific redirect problem below does not apply to Supabase Auth on any host. [INFERRED from supabase auth docs above]

### B1. Firebase Hosting (Spark)

- **Payment:** "No payment method needed" on Spark. [VERIFIED https://firebase.google.com/pricing "No payment method needed"]
- **Storage:** "10 GB". [VERIFIED https://firebase.google.com/pricing "10 GB"]
- **Data transfer (conflicting units):**
  - The pricing page says "360 MB/day". [VERIFIED https://firebase.google.com/pricing "360 MB/day"]
  - The hosting quotas doc says "10 GB/month". [VERIFIED https://firebase.google.com/docs/hosting/usage-quotas-pricing "at no cost up to 10 GB/month"]
  - The two pages disagree on units. Either is ample for one user. [INFERRED]
- **Over quota on Spark:** on transfer, "we offer a short grace period but then your sites will be disabled" until next month. On storage, no new deploys. [VERIFIED usage-quotas-pricing page "your sites will be disabled"]
- Custom domain + SSL and multiple sites per project are supported. [VERIFIED pricing page (checkmarks)]
- Max file "2 GB". [VERIFIED usage-quotas-pricing]
- Auth on Spark: "50K MAUs". [VERIFIED pricing page]
- **Private repo:** Deploys via the Firebase CLI or the GitHub Action, so a private repo is fine. [INFERRED]
- **Commercial use:** No restriction found. [UNVERIFIED]
- **OAuth concern (Firebase Auth only):**
  - The redirect flow "uses a cross-origin iframe that connects to your app's Firebase Hosting domain" and "doesn't work with browsers that block third-party storage access". Affected: Firefox 109+, Safari 16.1+, Chrome M115+.
  - Option 1 (custom `authDomain`) is only for apps hosted on Firebase Hosting.
  - "If you host your app with a service other than Firebase, use Option 2, Option 3, Option 4, or Option 5." [VERIFIED https://firebase.google.com/docs/auth/web/redirect-best-practices "If you host your app with a service other than Firebase"]
  - Only relevant if you choose Firebase Auth instead of Supabase Auth. [INFERRED]

### B2. Cloudflare (Workers Static Assets is the current recommendation)

- **Recommendation:**
  - "If you are starting a new project, use Workers instead of Pages."
  - "Pages continues to work, but new features and optimizations are focused on Workers." [VERIFIED https://developers.cloudflare.com/workers/best-practices/workers-best-practices/ "use Workers instead of Pages"]
  - The Next.js guide still says "To deploy a static Next.js export to Pages, refer to Static site". [VERIFIED https://developers.cloudflare.com/pages/framework-guides/nextjs/]
  - Both work for a static export. [INFERRED]
- **Workers Free:**
  - "Requests to static assets are free and unlimited"; Worker requests "100,000 per day". [VERIFIED https://developers.cloudflare.com/workers/platform/pricing/ "free and unlimited"]
  - Static assets: "Files per Worker version 20,000", "Individual file size 25 MiB". [VERIFIED https://developers.cloudflare.com/workers/platform/limits/]
  - SPA fallback: `not_found_handling = "single-page-application"` or `"404-page"`. [VERIFIED https://developers.cloudflare.com/workers/static-assets/]
  - Workers Builds (Git CI) Free: "3,000 per month" build minutes, 1 concurrent, "20 minutes" timeout. [VERIFIED https://developers.cloudflare.com/workers/ci-cd/builds/limits-and-pricing/]
- **Pages Free:** 500 builds/month, "1 build at a time", "Builds will timeout after 20 minutes", "up to 20,000 files", "25 MiB" max asset, 100 custom domains per project, "limit of 100 projects". [VERIFIED https://developers.cloudflare.com/pages/platform/limits/]
- **Card:** Search summary quoted the Cloudflare Workers page as "no credit card required". The page I fetched (cloudflare.com/products/workers/) did not show that wording. [UNVERIFIED]
- **Commercial use:** No restriction found. [UNVERIFIED]
- **Private repo:** Git integration with private GitHub repos is not confirmed on the pages read. Fallback: build in GitHub Actions and deploy with `wrangler deploy` and an API token. [UNVERIFIED / INFERRED]
- **OAuth concern:** None for Supabase Auth. For Firebase Auth, a non-Firebase host means Options 2–5 (popup or reverse proxy of `/__/auth/`, which a Worker can do). [INFERRED]

### B3. Vercel Hobby

- **Price and use:** "$0/mo." "Our Hobby plan is for personal, non-commercial use." [VERIFIED https://vercel.com/pricing "personal, non-commercial use"]
- **Docs wording:** "the Hobby plan restricts users to non-commercial, personal use only". [VERIFIED https://vercel.com/docs/plans/hobby]
- **Included:** Fast Data Transfer "First 100 GB", CDN Requests "First 1,000,000", Function Invocations "First 1,000,000". Projects 200, deployments/day 100, build time 45 min/deployment, 1 concurrent build. [VERIFIED https://vercel.com/docs/plans/hobby and https://vercel.com/docs/limits]
- **Over limit:** "you will have to wait until 30 days have passed before you can use the feature again"; "Hobby users can't purchase additional usage". [VERIFIED https://vercel.com/docs/plans/hobby "wait until 30 days have passed"]
- **Private repos:** "Vercel does not support connecting a project on your Hobby team to Git repositories owned by Git organizations." A personal-account private repo is fine; an org-owned repo is not. [VERIFIED https://vercel.com/docs/limits "owned by Git organizations"]
- **Card:** Not stated on the pages read. Upgrading to Pro asks for card details, implying Hobby doesn't. [UNVERIFIED/INFERRED]
- **OAuth concern:** None for Supabase. Preview-deployment URLs change per deploy, so you'd need a wildcard in Supabase Redirect URLs (allowed, "mainly for local development and preview deployments"). [VERIFIED redirect-urls page; INFERRED applicability]

### B4. Netlify Free

- **Credits:** "$0/month, forever, for 300 credits/month with a hard limit and no auto recharge option." "You'll never be charged for the Free plan." [VERIFIED https://docs.netlify.com/manage/accounts-and-billing/billing/billing-for-credit-based-plans/credit-based-pricing-plans/ "300 credits/month with a hard limit"]
- **Costs per item:**
  - Production deploy: 15 credits.
  - Bandwidth: 20 credits/GB.
  - Web requests: 2 credits per 10k.
  - Functions: 10 credits per GB-hour.
  - Deploy previews and branch deploys are not metered. [VERIFIED same URL]
- **Arithmetic:** 300 credits ÷ 15 = 20 production deploys per month, if nothing else is used. That is a real constraint for a dev who pushes often; deploy only on tags. [INFERRED]
- **When credits run out:** "all projects owned by that team enter a paused state until the start of the next billing cycle". Visitors see "Site not available". [VERIFIED https://docs.netlify.com/manage/accounts-and-billing/billing/resume-paused-projects/ "enter a paused state"]
- **Forum reports:** Free teams staying paused after the credit reset (community forum, not official). [UNVERIFIED]
- **Private repos:** "Private organization repositories" is Pro-only. Personal private repos are not addressed. [VERIFIED https://www.netlify.com/pricing/pro-vs-free/]
- **Card and commercial use:** Not stated. [UNVERIFIED]

### B5. GitHub Pages

- **Plans:** GitHub Free lists "GitHub Pages in public repositories" only. GitHub Pro adds Pages from private repos. "To publish a GitHub Pages site privately, you need to have an organization account" on Enterprise Cloud. [VERIFIED https://docs.github.com/en/get-started/learning-about-github/githubs-plans "GitHub Pages in public repositories"]
- **So on GitHub Free with a private repo, Pages is not available.** Workaround: a separate public repo holding only the built static output, pushed by Actions from the private repo. The built JS bundle is public anyway. [INFERRED]
- **Limits:** "no larger than 1 GB"; "soft bandwidth limit of 100 GB per month"; "soft limit of 10 builds per hour" (not for custom Actions workflows); deployments time out after "10 minutes". [VERIFIED https://docs.github.com/en/pages/getting-started-with-github-pages/github-pages-limits]
- **Prohibited:** Not for "e-commerce site" or "commercial software as a service (SaaS)". Should not handle "sensitive transactions like passwords or credit card numbers". [VERIFIED same URL] OAuth via Google/Supabase is not a password form on the site. [INFERRED]
- **No card needed.** [INFERRED]
- **SPA routing:** No rewrite support, so use the static export's per-route `index.html` files or `trailingSlash: true`. [INFERRED]

---

## Part C: GitHub Actions on GitHub Free (private repos)

- **Included compute is now a dollar amount.**
  - GitHub Free includes "$12 USD" of compute per month. "The included compute usage is expressed as a monetary amount, not a fixed number of minutes".
  - Linux 2-core x64 is "$0.006 USD" per minute, Windows "$0.010", macOS "$0.062".
  - Public repos on standard runners are free. [VERIFIED https://docs.github.com/en/billing/concepts/product-billing/github-actions "$12 USD" / "$0.006 USD"]
  - $12 ÷ $0.006 = **2,000 Linux minutes per month**; Windows ≈ 1,200; macOS ≈ 193. The old "multipliers" are replaced by per-minute rates. [INFERRED arithmetic]
- **No overage charge without a card.** Without a payment method, "usage is blocked once you consume the included compute usage". [VERIFIED same URL "usage is blocked once you consume the included compute usage"]
- **Storage:**
  - Artifact storage: GitHub Free "500 MB", calculated hourly (GB-hours). [VERIFIED same URL and https://docs.github.com/en/actions/reference/limits "500 MB"]
  - Cache: "10 GB per repository". [VERIFIED billing page]
  - Job max: "6 hours". [VERIFIED limits page]
- **Retention:** The default is "retained for 90 days". Private repos are configurable "between 1 day or 400 days". [VERIFIED https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/enabling-features-for-your-repository/managing-github-actions-settings-for-a-repository "between 1 day or 400 days"] Use `retention-days` on `upload-artifact` to keep APKs short-lived and inside 500 MB. [INFERRED]
- **Gradle caching:**
  - `gradle/actions/setup-gradle` caches Gradle User Home and wrapper distributions. It writes the cache only from "Jobs on the default" branch.
  - Enhanced Caching "uses the proprietary gradle-actions-caching library", "currently available as a Free Preview for private repositories".
  - Basic Caching "is free for all repositories (public and private)". [VERIFIED https://github.com/gradle/actions/blob/main/docs/setup-gradle.md "Free Preview for private repositories"]
  - Use `cache-provider: basic` to avoid preview-licence surprises. [INFERRED]
- **Is a debug APK build feasible? Yes.**
  - A small Compose app's `assembleDebug` on ubuntu-latest typically takes about 4–10 min cold and 2–5 min warm. [INFERRED; third-party PR evidence in search results: ~12 min cold → ~2 min warm on a larger project, not official]
  - At about 6 min per build, 2,000 min ≈ 330 builds/month. [INFERRED]
  - A debug APK is roughly 10–30 MB. With `retention-days: 7` that stays well under 500 MB. [INFERRED]
  - Adding a nightly `pg_dump` job (about 1 min) costs ~30 min/month. [INFERRED]

---

## Gotchas for this project

1. **7-day inactivity pause.** A single-user app idle for a week (holiday, broken phone) pauses the Supabase project. The app must tolerate a paused backend: the Room outbox keeps queuing and sync resumes after a manual "Resume project" in the Dashboard. The restore window is currently 1 year (was 90 days in 2024). A keep-alive ping is not mentioned in the docs either way; a GitHub Actions daily cron doing a trivial authenticated query is a common workaround but is not an officially sanctioned practice. [VERIFIED rule / INFERRED workaround]
2. **No backups on Free.** You own backups. Schedule a nightly `pg_dump` (or `supabase db dump`, which needs Docker) via GitHub Actions using a DB password secret, plus an in-app JSON/CSV export from Room as a second copy. Deleting a project deletes backups permanently.
3. **The Kotlin SDK signs out locally by default.** `signOut()` in supabase-kt defaults to `local`. Pass global scope for "sign out of all devices". Access JWTs stay valid up to 1 hour after revocation.
4. **supabase-kt is community-maintained but going official.** Expect a namespace or Maven-coordinate change. Pin a stable 3.x (3.8.0 seen; 3.9.0 is beta). minSdk 26.
5. **Google client IDs.** The Android app passes the *Web* client ID to `setServerClientId`. The Android client ID must carry the SHA-1 of every signing key (debug, CI debug keystore, release or Play signing). Use a committed or shared debug keystore in CI, or CI-built APKs will fail sign-in. Hash the nonce for Google and send the raw nonce to Supabase.
6. **RLS everywhere.** Use `(select auth.uid()) = user_id` with a `to authenticated` clause, an index on `user_id`, and `with check` on insert/update. Never ship the service-role/secret key in the APK or web bundle. Test with pgTAP via `supabase test db` (local stack needs Docker, or the experimental native runtime on Linux/Apple-silicon).
7. **No first-party offline sync.** The Room + outbox + idempotent upserts (client UUIDs, `on conflict do nothing/update`) + `updated_at`/tombstones design is the expected approach. PowerSync Free has its own 1-week inactivity deactivation, so it adds a second pause risk with no benefit for one user.
8. **Hosting choice.**
   - GitHub Pages won't serve from a private repo on GitHub Free.
   - Vercel Hobby is OK (personal, non-commercial) but cannot connect org-owned repos.
   - Netlify Free's 300 credits ≈ 20 production deploys/month, then the site pauses.
   - Cloudflare (Workers Static Assets, unlimited free asset requests) or Firebase Hosting Spark ("No payment method needed") are the lowest-risk ₹0 choices.
   - Add every production and preview origin to the Supabase Redirect URLs allow-list.
9. **Firebase redirect pitfall only applies if you use Firebase Auth.** If you ever switch to Firebase Auth on a non-Firebase host, `signInWithRedirect` breaks on modern browsers unless you use a popup or proxy.
10. **Region.** Pick Mumbai `ap-south-1` at project creation. Don't paste personal data into Supabase's AI assistant: its inputs go to OpenAI or Bedrock, per the privacy policy.
11. **Actions budget.** $12/month ≈ 2,000 Linux minutes. Usage is blocked, not billed, once exhausted with no card. Artifacts cap at 500 MB, so set `retention-days`. Use `setup-gradle` with `cache-provider: basic`.
12. **Free-plan restrictions are 402s, not bills.** Exceeding quotas (unlikely at this data volume) leads to a grace period and then 402 or read-only. The 2 GB disk at 95% triggers read-only. WAL from bulk imports can spike disk, so batch any history import.
