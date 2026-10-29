# Firebase research for "Brain Rot 2.0" (₹0, Spark-only)

**Date checked:** 2026-10-29. All VERIFIED items were read today on the official page cited (WebFetch, or curl plus text extraction).

Legend:
- `[VERIFIED url "quote"]`: read on an official page today. The quoted phrase is verbatim or nearly verbatim.
- `[INFERRED]`: my own reasoning, or something no official page stated outright.

---

## 1. Spark vs Blaze: what is available at ₹0

**Plan basics**
- Spark needs no payment information. [VERIFIED https://firebase.google.com/docs/projects/billing/firebase-pricing-plans "No payment information needed to get started or to use only the no-cost Firebase products"]
- Linking a billing account upgrades the project to Blaze automatically. [VERIFIED same page "linking a Cloud Billing account to your project will auto-upgrade your project to the Blaze pricing plan"]
- Blaze needs a payment method. [VERIFIED same page "Cloud Billing accounts require a payment method"]

**Products that are no-cost on any plan**
- Products with full no-cost usage on any plan: A/B Testing, App Check, App Distribution, Authentication (most options), FCM, Crashlytics, Firebase ML (model deployment), In-App Messaging, Performance Monitoring. [VERIFIED same page "you get full usage of the following Firebase products at no charge — even in your production apps"]
- Products with a no-cost quota on any plan, and pay-as-you-go beyond it on Blaze: App Hosting and Hosting, Authentication (phone), Phone Number Verification, SQL Connect, Extensions, Cloud Firestore, Cloud Storage, Cloud Functions, Realtime Database, Test Lab, AI Logic, Remote Config. [VERIFIED same page, section "Firebase products with a paid-tier of usage"]
  - In practice several of these still need Blaze. See the Cloud Functions, Cloud Storage and App Hosting entries below.

**Product by product**

| Product | On Spark? | Source |
|---|---|---|
| **Cloud Functions** | **No. Deploying requires Blaze.** The pricing page shows "Not applicable" for every Spark entry. | [VERIFIED https://firebase.google.com/docs/functions/get-started "to deploy functions, your project must be on the Blaze pricing plan"] [VERIFIED https://firebase.google.com/pricing: Spark column "Not applicable"]. The emulator works on any project: [VERIFIED same get-started page "You can emulate functions in any Firebase project"] |
| **Cloud Run** | **No** | [VERIFIED https://firebase.google.com/docs/projects/billing/firebase-pricing-plans "Paid Google Cloud products and features (like Pub/Sub, Cloud Run, or BigQuery streaming for Analytics) are not available for proje[cts on the Spark plan]"] |
| **Firestore scheduled backups** | **No. Blaze only.** | [VERIFIED https://firebase.google.com/docs/firestore/backups "This feature requires the Blaze pricing plan."] Retention: "up to 14 weeks (`14w`)". Schedules: "up to one daily backup schedule and up to one weekly backup schedule" per database |
| **Firestore PITR** | **No. Billing required.** | [VERIFIED https://firebase.google.com/docs/firestore/pitr "PITR storage doesn't have a free tier and you must have billing enabled if you want to use PITR"]. Window: "PITR data is retained for 7 days". Without PITR: "you can read (but not export) documents at any microsecond-granularity timestamp within the past hour" |
| **Managed export/import (`gcloud firestore export`)** | **No. Blaze plus a Cloud Storage bucket.** | [VERIFIED https://firebase.google.com/docs/firestore/manage-data/export-import "Firebase projects must be on the Blaze plan to use the managed export and import service"; "Only Google Cloud projects with billing enabled can use the export and import functionality"] |
| **Firestore TTL deletes, restore, clone** | **No** | [VERIFIED https://firebase.google.com/docs/firestore/quotas "The following operations and features don't include free usage. You must enable billing to use these features: TTL deletes PITR data Backup data Restore operations Clone operations"] |
| **Firebase Hosting** | **Yes.** Storage 10 GB. Transfer: the pricing page says 360 MB/day; the Hosting docs say 10 GB/month. Details in §6. | [VERIFIED https://firebase.google.com/pricing "Storage 10 GB … Data transfer 360 MB/day"] [VERIFIED https://firebase.google.com/docs/hosting/usage-quotas-pricing "at no cost up to 10 GB/month"] |
| **App Hosting (SSR Next.js)** | Treat as **no**: the pricing page says it requires a billing account. | [VERIFIED https://firebase.google.com/pricing: some Firebase Studio integrations, such as Firebase App Hosting, "require a billing account"] |
| **App Check** | **Yes, at no cost.** Each attestation provider has its own quota. | [VERIFIED https://firebase.google.com/docs/app-check "Play Integrity has a daily quota of 10,000 calls for its Standard API usage tier"; "reCAPTCHA Enterprise is no-cost for 10,000 assessments each month, and has a cost beyond that"]. Play Integrity setup needs Play Console: [VERIFIED https://firebase.google.com/docs/app-check/android/play-integrity-provider "In the Google Play Console, select your app… click Link Cloud project"]. That provider also "supports Android apps that are published on Google Play, outside Google Play, or both". [INFERRED] A Play Console developer account (a one-time registration fee) is probably needed to link Play Integrity. App Check is optional for this project. |
| **Authentication (Google sign-in)** | **Yes.** No MAU limit on the base product. | [VERIFIED https://firebase.google.com/pricing: Identity Platform MAUs Spark "50K MAUs"; SAML/OIDC "50 MAUs"; Phone auth Spark "Not applicable"] |
| **Auth with Identity Platform upgrade** | Optional. On Spark the upgrade brings a **3,000 DAU/day** limit. | [VERIFIED https://firebase.google.com/docs/auth "When upgraded, no-cost (Spark) plan projects will be limited to 3,000 daily active users"] [VERIFIED https://firebase.google.com/docs/auth/limits "Tier 1 Daily Active Users 3000 per day; Tier 2 Daily Active Users 2 per day"]. **Do not upgrade.** It is not needed for Google sign-in. |
| **Cloud Storage for Firebase** | **Not usable on Spark since 2026-02-03**, even for default buckets | See below |

**Cloud Storage change (announced 2024, enforced 2026)**
- Storage now needs Blaze even for default buckets. [VERIFIED https://firebase.google.com/docs/storage/faqs-storage-changes-announced-sept-2024 "Cloud Storage for Firebase (even default buckets) now requires projects to be on the pay-as-you-go Blaze pricing plan."]
- Effective date: [VERIFIED same "This requirement went into effect starting February 03, 2026."]
- What Spark projects get: [VERIFIED same "If your project is on the Spark pricing plan, you won't have access to any Cloud Storage buckets (including default buckets)"; API calls "will return 402 or 403 errors"]
- Consequence: no file attachments and no Storage-based backup blobs at ₹0. [INFERRED]

**Auth rate limits relevant to one user**
- Sign-up limit: [VERIFIED https://firebase.google.com/docs/auth/limits "New account creation 100 accounts/hour for each IP address"]
- Limits are not guaranteed: [VERIFIED same "Limitations apply per customer and can change without notice"]
- Nothing on that page would bind a single Google sign-in user. [INFERRED]

## 2. Firestore free quota on Spark

**Free quota**
- [VERIFIED https://firebase.google.com/docs/firestore/quotas and https://firebase.google.com/docs/firestore/pricing] "Stored data 1 GiB; Document reads 50,000 per day; Document writes 20,000 per day; Document deletes 20,000 per day; Outbound data transfer 10 GiB per month"
- Reset: [VERIFIED same "Quotas are applied daily and reset around midnight Pacific time."] Midnight Pacific is about 12:30–13:30 IST, depending on US daylight saving time. [INFERRED]

**Which database gets it**
- One database per project: [VERIFIED https://firebase.google.com/docs/firestore/pricing "Cloud Firestore allows exactly one free database per project."]
- It does not have to be `(default)`: [VERIFIED same "The first database you create (regardless of its ID) qualifies for the free quota."]
- Extra databases need Blaze: [VERIFIED https://firebase.google.com/docs/firestore/locations "If your project is on the pay-as-you-go Blaze pricing plan, then you can optionally create multiple databases"]
- So on Spark, plan for one database, and the quota is effectively per project. [INFERRED]

**Location**
- asia-south1 (Mumbai) and asia-south2 (Delhi) are both listed. [VERIFIED https://firebase.google.com/docs/firestore/locations "asia-south1 Mumbai asia-south2 Delhi"]
- The location is permanent: [VERIFIED same "once you provision a database instance, you cannot change its location setting"]
- No location restriction on the Firestore free tier is stated. The pricing page only says pricing varies by location ("Pricing by location"). [VERIFIED https://firebase.google.com/docs/firestore/pricing]
  - Contrast with new-style Storage buckets, whose no-cost quota is limited to us-central1, us-west1 and us-east1 (https://firebase.google.com/pricing).
- [INFERRED] The free quota applies in asia-south1. I found no official statement saying it does not.

**When the quota is exceeded on Spark**
- Spark shuts the product off. There is no charge. [VERIFIED https://firebase.google.com/docs/projects/billing/firebase-pricing-plans "If you exceed the no-cost quota limit in a calendar month for any product, your project's usage of that specific product will be shut off for the remainder of that month… To use that specific product again, you'll need to wait until the next billing cycle or upgrade to the Blaze pricing plan."]
- The same page also says: [VERIFIED "Cloud Firestore and Cloud Storage have no-cost tiers that reset every day"]
- [INFERRED] The two statements conflict ("remainder of that month" versus daily reset). For Firestore, the most likely practical behavior is that requests fail until the daily reset. The worst case is a lockout for the rest of the month. Either way it is a **hard stop, not a bill**.
- [INFERRED] Failed requests probably surface as `RESOURCE_EXHAUSTED`. [VERIFIED that the code exists: https://firebase.google.com/docs/reference/android/com/google/firebase/firestore/FirebaseFirestoreException.Code "RESOURCE_EXHAUSTED Some resource has been exhausted, perhaps a per-user quota"]
- [INFERRED] At 10–50 events per day the app uses less than 0.3% of the write quota. Reads are the only realistic risk. See the §8 cost traps.

## 3. Firestore offline behavior

**Android**
- Persistence is on by default: [VERIFIED https://firebase.google.com/docs/firestore/manage-data/enable-offline "For Android and Apple platforms, offline persistence is enabled by default."]
- Cache size: [VERIFIED same "The default cache size threshold is 100 MB."] It is configurable with `setCacheSizeBytes`, minimum 1 MB.
- Queued writes: [VERIFIED same "Write operations are queued until network access is re-enabled."]
- Conflicts: [VERIFIED same, for multiple changes to the same document "it's last write wins"]

**Pending writes survive restarts**
- **Yes.** [VERIFIED https://firebase.google.com/docs/reference/android/com/google/firebase/firestore/FirebaseFirestore, `waitForPendingWrites`: "waits for all previously issued writes (including those written in a previous app session)"]
- [VERIFIED same, `terminate()`: "does not cancel any pending writes… The next time you start this instance, it will resume attempting to send these writes to the server."]
- The queue is per user: [VERIFIED same "Waits until all currently pending writes for the active user… Any outstanding waitForPendingWrites() Tasks are cancelled during user changes."]
- [VERIFIED source https://raw.githubusercontent.com/firebase/firebase-android-sdk/main/firebase-firestore/src/main/java/com/google/firebase/firestore/local/SQLiteMutationQueue.java "A mutation queue for a specific user, backed by SQLite."]
- `clearPersistence()` drops pending writes: [VERIFIED reference page "Clears the persistent storage, including pending writes and cached documents."]
- [INFERRED] Signing out with writes still pending leaves them queued under that uid. They are sent only after the same user signs in again.

**Web**
- Persistence is off by default: [VERIFIED enable-offline page "For the web, offline persistence is disabled by default."]
- It is enabled with `initializeFirestore(app, { localCache: persistentLocalCache({ tabManager: persistentMultipleTabManager() }) })`. [VERIFIED same "Defaults to single-tab persistence if no tab manager is specified"; "Use multi-tab IndexedDb persistence."]
- Browser support: [VERIFIED same "offline persistence is supported only by the Chrome, Safari, and Firefox web browsers."]
- Privacy: the page warns that the cache isn't cleared between sessions, so the app should ask whether the device is trusted.
- Web pending writes also survive reloads: [VERIFIED https://firebase.google.com/docs/reference/js/firestore_ `waitForPendingWrites`: "(including those written in a previous app session)"]

**Transactions**
- [VERIFIED https://firebase.google.com/docs/firestore/manage-data/transactions "Transactions will fail when the client is offline."]
- Batches are limited to 500 writes: [VERIFIED Android `FirebaseFirestore.batch()` reference "The maximum number of writes allowed in a single batch is 500"]
- [INFERRED] Batched writes, unlike transactions, are queued offline like normal writes.

**serverTimestamp**
- [VERIFIED https://firebase.google.com/docs/firestore/manage-data/add-data "a server timestamp which tracks when the server receives the update"]
- Before the server confirms, snapshots return `null` by default. They can return `'estimate'` (local clock) or `'previous'` instead. [VERIFIED https://firebase.google.com/docs/reference/js/firestore_.snapshotoptions "By specifying 'estimate', pending server timestamps return an estimate based on the local clock… If omitted or set to 'none', null will be returned"]
- [INFERRED] For offline-logged habit events, the event time must be a **client timestamp field**. `serverTimestamp` records when the write synced, which could be days later.
- Pending-write flag: [VERIFIED https://firebase.google.com/docs/firestore/query-data/listen "metadata.hasPendingWrites property that indicates whether the document has local changes that haven't been written to the backend yet"]

**Conflict granularity**
- [INFERRED] Firestore applies writes in the order they reach the server. A stale device that comes back online later overwrites newer data. This is "last write wins" by arrival time, not by client timestamp.
- [INFERRED] `update()` and `set(..., {merge:true})` only touch the named fields, so conflicts are effectively field-level for those calls. `set()` without merge replaces the whole document. [VERIFIED https://firebase.google.com/docs/firestore/manage-data/add-data "its contents will be overwritten with the newly provided data, unless you specify that the data should be merged"]

**Room as source of truth**
- [VERIFIED https://developer.android.com/topic/architecture/data-layer/offline-first "The local data source is the canonical source of truth for the app. It should be the exclusive source of any data that higher layers of the app read."]
- On a write queue: [VERIFIED same "On Android this queue might be maintained with a Room database, and drained as persistent work using WorkManager."]
- Firestore is offered as the alternative: [VERIFIED same "Managed cloud services: Solutions like Cloud Firestore can provide an all-in-one local cache… The SDK handles disk caching, offline write queuing, and bidirectional sync automatically"]
- On conflicts: [VERIFIED same, describes timestamp-based "last write wins"]
- [INFERRED] Room plus a durable sync queue on top of Firestore means two offline layers. Pick one:
  - (a) Make Room the source of truth and push to Firestore from a WorkManager worker. Firestore's own pending-write queue then handles transport. Use idempotent, client-generated document IDs so retries do not duplicate events.
  - (b) Rely on Firestore's cache alone.
  - Option (a) fits the project, but Firestore persistence should then stay on (the default) or be disabled deliberately, so the two queues don't diverge.

## 4. Security Rules, sessions, testing

**Per-user isolation**
- [VERIFIED https://firebase.google.com/docs/rules/rules-and-auth "allow read, write: if request.auth != null && request.auth.uid == userId;"]
- Use the path `users/{userId}/...` with a recursive wildcard. [INFERRED]

**auth_time**
- The Firestore rules reference says `request.auth.token` is "a map of JWT token claims". It lists email, email_verified, phone_number, name, sub, firebase.identities, firebase.sign_in_provider and firebase.tenant. **It does not list `auth_time`.** [VERIFIED https://firebase.google.com/docs/reference/rules/rules.firestore.Request "token - a map of JWT token claims"]
- Firebase ID tokens do carry `auth_time`. [VERIFIED https://firebase.google.com/docs/auth/admin/verify-id-tokens "auth_time Authentication time Must be in the past. The time when the user authenticated."]
- The official revocation pattern uses `auth.token.auth_time` in **Realtime Database** rules. [VERIFIED https://firebase.google.com/docs/auth/admin/manage-sessions `auth.token.auth_time > root.child('metadata').child(auth.uid).child('revokeTime').val()`]
- [INFERRED] `request.auth.token.auth_time` is very likely readable in Firestore rules, since the token is the full claim map. The Firestore docs don't promise it, so test it in the emulator.
  - It is in seconds since the epoch. `request.time` is a Timestamp, so compare it with e.g. `revokedBefore.toMillis() / 1000`.
  - `auth_time` reflects the original sign-in, not a token refresh. A "revoked-before" field in a user-only-readable doc that the client cannot write therefore works as a kill switch.
  - Without Admin SDK or Functions, the user's own client would have to write that field. Rules would then allow the write only by a recently signed-in token. [INFERRED]

**Revoking refresh tokens**
- Admin SDK only: [VERIFIED https://firebase.google.com/docs/auth/admin/manage-sessions "The Firebase Admin SDK provides the ability to revoke refresh tokens for a specified user."]
- ID token lifetime: [VERIFIED same "Firebase ID tokens are short lived and last for an hour."]
- [INFERRED] The Admin SDK can run from a local machine with a service-account key at ₹0. No Cloud Functions are needed for a one-off revoke.
- [INFERRED] A disabled user or a password change would also invalidate sessions.

**Rules unit testing**
- [VERIFIED https://firebase.google.com/docs/rules/unit-tests "Use the @firebase/rules-unit-testing module to interact with the emulator that runs locally"; `initializeTestEnvironment` "Successful execution requires emulators to be running"; `authenticatedContext` "behaves like an authenticated Authentication user"; `assertFails`/`assertSucceeds`]
- The recommended version is the one shipped with the v9 JS SDK: [VERIFIED "We recommend the v9 testing library"; "The v9 Rules Unit Testing library is always aware of the emulators and never touches your production resources"]
- Example project ID on that page: `demo-project-1234`.

**Demo projects**
- They need no real project or account. [VERIFIED https://firebase.google.com/docs/emulator-suite/connect_firestore "A demo Firebase project has no real Firebase configuration and no live resources… Project IDs for demo projects have the demo- prefix… your apps and code interact with emulators only… Easier setup, since you can run the emulators without ever creating a Firebase project… there is no chance of data change, usage and billing"]
- Emulator prerequisites: [VERIFIED https://firebase.google.com/docs/emulator-suite/install_and_configure "Node.js version 16.0 or higher. Java JDK version 11 or higher."]
- [INFERRED] `firebase emulators:start --project demo-brainrot` runs without logging in. A Google account is only needed later to deploy.

## 5. Android Google sign-in

**Legacy API**
- Deprecated, with no removal date given: [VERIFIED https://developer.android.com/identity/sign-in/legacy-gsi-migration "Google Sign-In for Android (as part of com.google.android.gms:play-services-auth…) is deprecated and will be removed from the Google Play services Auth SDK in a future release."]
- Replacement: Credential Manager for authentication, and `AuthorizationClient` for Drive and similar scopes.

**Recommended flow**
- [VERIFIED https://firebase.google.com/docs/auth/android/google-signin] The page:
  - builds a `GetGoogleIdOption` inside a `GetCredentialRequest`
  - calls `GoogleIdTokenCredential.createFrom(credential.data)`
  - then calls `GoogleAuthProvider.getCredential(idToken, null)` and `auth.signInWithCredential(credential)`
- The server client ID is the **Web** client ID: [VERIFIED same "Your server's client ID, not your Android client ID"]
- Sign-out: `auth.signOut()` plus `clearCredentialState(ClearCredentialStateRequest())`. [VERIFIED same "When a user signs out, clear the current user credential state from all credential providers."]

**Dependencies and options**
- [VERIFIED https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation] The page lists:
  - `androidx.credentials:credentials:1.7.0-alpha03`
  - `androidx.credentials:credentials-play-services-auth:1.7.0-alpha03`
  - `com.google.android.libraries.identity.googleid:googleid:<latest version>`
- It also covers `GetSignInWithGoogleOption.Builder(serverClientId = WEB_CLIENT_ID)` for the button flow.
- On `setFilterByAuthorizedAccounts`: "the default value… is true".
- [INFERRED] 1.7.0-alpha03 is an alpha. Use the latest stable `androidx.credentials` release unless the alpha is needed.

**Supported devices**
- [VERIFIED https://developer.android.com/identity/sign-in/credential-manager-siwg "works on devices running Android 4.4 (API level 19) and higher"]

**SHA fingerprints**
- [VERIFIED https://firebase.google.com/docs/auth/android/google-signin "Specify your app's SHA-1 fingerprint"]
- [VERIFIED https://developers.google.com/android/guides/client-auth "Certain Google Play services (such as Google Sign-in…) require you to provide the SHA-1 of your signing certificate so we can create an OAuth2 client"; "You should get both the release and debug certificate fingerprints"; "When using Play App Signing, the upload key certificate will be different than the app signing key certificate."]
- The debug keystore is `~/.android/debug.keystore`, password `android`.
- [INFERRED] Register the SHA-1 for the debug key and the release keystore. SHA-256 is optional for Google sign-in; it is used by App Check and App Links.
- [INFERRED] **No Play Store is needed** for a sideloaded APK. Sign-in only checks package name plus signing-certificate SHA-1 against the OAuth client. The device needs Google Play services, and `credentials-play-services-auth` relies on it. Nothing I read states "sideloading is OK" explicitly. The Play Integrity App Check page does say it supports apps "published… outside Google Play".

## 6. Web: Hosting, Next.js, redirect sign-in

**Hosting limits**
- Storage: [VERIFIED https://firebase.google.com/docs/hosting/usage-quotas-pricing "Storage for your Hosting content is at no cost up to 10 GB"]. At the limit "you won't be able to deploy new content".
- Transfer: [VERIFIED same "Data transfer from the CDN to your end users is at no cost up to 10 GB/month. If you are not on the Blaze plan… we offer a short grace period but then your sites will be disabled. Your sites will remain disabled until the start of the next month"]
- The pricing page uses a daily figure: [VERIFIED https://firebase.google.com/pricing "Data transfer 360 MB/day"]
- [INFERRED] The 10 GB/month and 360 MB/day figures differ, and the daily one is stricter. For a one-user static app either is far more than needed. Avoid shipping large source maps or images.
- Maximum file size: [VERIFIED usage page "maximum size limit of 2 GB for individual files"]

**Next.js**
- Framework-aware Hosting is closed to new users: [VERIFIED https://firebase.google.com/docs/hosting/frameworks/nextjs "Framework-aware Hosting is an early public preview"; new participation is closed; "we recommend 'graduating' to App Hosting"]
- SSR and image optimization deploy Cloud Functions (Blaze): [VERIFIED same, image optimization "will trigger creation of a function"]
- **Use `output: 'export'`** and deploy `out/` as plain static Hosting. [INFERRED]
- The Next.js docs (vendor-official, not Firebase) list what static export cannot do: [VERIFIED https://nextjs.org/docs/app/guides/static-exports "Unsupported Features… Dynamic Routes without generateStaticParams(), … Rewrites, Redirects, Headers, … Image Optimization with the default loader, … Server Actions"]
- [INFERRED] Use client-side routing with query params, or Hosting `rewrites` in `firebase.json` for SPA fallbacks.

**signInWithRedirect**
- Why it breaks: [VERIFIED https://firebase.google.com/docs/auth/web/redirect-best-practices "uses a cross-origin iframe that connects to your app's Firebase Hosting domain… this mechanism doesn't work with browsers that block third-party storage access"]
- Affected browsers: from June 24, 2024 a fix is "required for redirect sign-in to work on Google Chrome M115+". Firefox 109+ and Safari 16.1+ already required one.
- Options:
  1. Use your Firebase Hosting custom domain or web.app domain as `authDomain`.
  2. `signInWithPopup()`.
  3. Proxy `/__/auth/` to firebaseapp.com.
  4. Self-host the helper code.
  5. Do provider sign-in yourself, then call `signInWithCredential()`.
- Not affected: apps hosted on a firebaseapp.com subdomain.
- [INFERRED] Hosting on Firebase Hosting (`<project>.web.app`) with `authDomain: "<project>.web.app"` fixes redirect at ₹0. `signInWithPopup` on desktop is the simplest choice.

## 7. Backup, export and recovery at ₹0

**Not available on Spark**
- Scheduled backups, PITR and managed export all require billing. [VERIFIED, links in §1]

**What remains at ₹0**
- [INFERRED] **Client-side export.** The app reads its own docs and writes JSON. This could be Android (SAF/Downloads, or the user's Google Drive via `AuthorizationClient`) or a browser download from the web app.
  - A full export of 50k docs costs about 50k reads, the whole daily read quota. Make it incremental, using an `updatedAt` cursor and a stored last-export marker, or spread it over days.
  - **Room already holds a full local copy**, so exporting from Room costs 0 Firestore reads.
- [INFERRED] **Admin SDK script locally**, using a service-account key and Node or Python. It reads everything (reads count against the quota) and writes JSON. No Blaze is needed, since it is just API reads.
- [INFERRED] **gcloud/REST** reads also count as normal reads.
- Built-in short-term recovery: [VERIFIED https://firebase.google.com/docs/firestore/pitr "you can read (but not export) documents at any microsecond-granularity timestamp within the past hour"]. [INFERRED] Whether a client SDK can do stale reads on Spark is unverified; this is a server-API feature.

**Deletion and retention in Google systems**
- Overall commitment: [VERIFIED https://cloud.google.com/docs/security/deletion "Google Cloud commits to delete customer data within a maximum period of about six months (180 days)"]
- Marked for deletion: "our goal is to perform this step within a maximum period of 24 hours". Then "an internal recovery period of up to 30 days might apply".
- Active systems: "it generally takes about two months to delete data from active systems".
- Backups: "The Google backup cycle is designed to expire deleted data within data center backups within six months of the deletion request."
- Project deletion: [VERIFIED https://cloud.google.com/resource-manager/docs/creating-managing-projects "These projects are fully deleted after 30 days."] This means soft-deleted projects.
- Subcollections: [VERIFIED https://firebase.google.com/docs/firestore/data-model "Deleting a document does not delete its subcollections!"] The client must delete subcollection docs itself when wiping data.

## 8. Data modeling and migration

**Schemaless**
- [VERIFIED https://firebase.google.com/docs/firestore/data-model "Cloud Firestore is schemaless… Documents within the same collection can all contain different fields"]
- [INFERRED] Put a `schemaVersion` field on each doc. Migrate lazily on read in the client, and enforce shapes in rules (`request.resource.data.keys().hasOnly([...])`). There is no server-side migration runner at ₹0, since there are no Functions.

**Limits** [VERIFIED https://firebase.google.com/docs/firestore/quotas]

| Limit | Value |
|---|---|
| Maximum document size | "1 MiB (1,048,576 bytes)" |
| Composite indexes per database | "200 when you have not enabled billing"; "1000 when you enable billing" |
| Single-field index configurations | same as composite: 200 without billing, 1000 with billing |
| Index entries per document | "40,000" |
| Fields in a composite index | "100" |
| Index entry size | "7.5 KiB" |
| Indexed field value | "1500 bytes", truncated beyond that |
| Subcollection depth | "100" |
| Field path size | "1,500 bytes" |
| Map/array depth | "20" |
| API request size | "10 MiB" |
| Transaction time limit | "270 seconds, with a 60-second idle expiration time" |
| Field transforms per commit | "500" |
| Security Rules access calls (get/exists) | 10 per single-doc request, 20 per batch. Exceeding them gives permission denied. |

**Write rates and IDs**
- The quotas page today gives no per-document write rate. [VERIFIED: absent from https://firebase.google.com/docs/firestore/quotas]
- Monotonic IDs: [VERIFIED https://firebase.google.com/docs/firestore/best-practices "Do not use monotonically increasing document IDs… Such sequential IDs can lead to hotspots"]. Hotspot risk comes from creating "documents with a monotonically increasing field, like a timestamp, at a very high rate".
- Ramp-up rule: "500/50/5" (start at 500 ops/sec, +50% every 5 minutes).
- [INFERRED] None of this matters at 50 writes per day. Still, use random or UUID document IDs, which also make retries idempotent.

**Cost traps** [VERIFIED https://firebase.google.com/docs/firestore/pricing]
- "There is a minimum charge of one document read for each query that you perform, even if the query returns no results."
- Listeners: "charged for a read each time a document in the result set is added or updated". With persistence on, "if the listener is disconnected for more than 30 minutes… you will be charged… as if you had issued a brand-new query". With persistence off, every reconnect counts as a new query.
- Offsets: "when you send a query that includes an offset, you are charged a read for each skipped document". Use cursors instead.
- Aggregations: count/sum/avg are charged "one read operation for each batch of up to 1000 index entries", with a minimum of 1 read.
- Rules `get()`/`exists()`: [VERIFIED https://firebase.google.com/docs/firestore/security/rules-conditions "you will be billed for reading documents even if your rules reject the request"]
- [INFERRED] The biggest trap is a web or Android listener on the **whole events collection**. Each cold start after more than 30 minutes re-reads every matching doc. At 50k docs, a single web tab reload uses the full daily read quota.
  - Fix: listen only to a bounded window, e.g. `where(updatedAt > lastSync)` or the last 30 days.
  - Keep per-day or per-month aggregate docs.
  - Use `count()` for stats.
  - Never page with offsets.

## 9. Unexpected-cost risk

- [VERIFIED https://firebase.google.com/docs/projects/billing/firebase-pricing-plans] On Spark, over-quota usage is shut off ("will be shut off for the remainder of that month"), not billed.
- Billing only starts through a billing account, which needs a payment method ("linking a Cloud Billing account to your project will auto-upgrade your project to the Blaze pricing plan"; "Cloud Billing accounts require a payment method").
- [INFERRED] Without a billing account linked, no charges are possible. I found no single official sentence saying "Spark cannot be charged"; this follows from the two quotes above.
- [INFERRED] Watch for console prompts that upgrade the project, e.g. enabling Storage, Functions, backups, PITR or App Hosting. Each one asks to link billing.
- Budgets don't cap spending on Blaze: [VERIFIED https://firebase.google.com/docs/projects/billing/avoid-surprise-bills "Budget alerts do not pause services."]
- Spend-cap budgets exist only for some services: [VERIFIED same "Spend caps budgets are only available for the following Firebase services: Firebase AI Logic, Firebase App Hosting, Cloud Functions for Firebase, and Firebase Extensions."] **Firestore cannot be capped on Blaze.**
- The Identity Platform upgrade on Spark trades MAU billing for a 3,000 DAU/day limit (§1). It costs no money, but don't do it.

## 10. Privacy and terms

**Processor role**
- [VERIFIED https://firebase.google.com/support/privacy "When customers use Firebase, Google is generally a data processor under GDPR and processes personal data on their behalf"; "Firebase terms include Data Processing and Security Terms"]
- Firestore, Auth, App Check, Functions and Storage are under the GCP Terms and the Cloud Data Processing Addendum: [VERIFIED https://firebase.google.com/terms, product list under "Google Cloud Platform Terms of Service"] [VERIFIED https://firebase.google.com/support/privacy "Certain Firebase services governed by the Google Cloud Platform (GCP) Terms of Service are already covered by associated data processing terms, the Cloud Data Processing Addendum"]
- Processing scope: [VERIFIED https://firebase.google.com/terms/data-processing-terms "Customer instructs Google to process Customer Personal Data in accordance with applicable law only: (a) to provide, secure and monitor the Services…"]

**Advertising and AI training**
- Ads: [VERIFIED https://cloud.google.com/transparency "Google Cloud does not use customer data for advertising"; "Google does not sell customer data to third parties"]
- Training: [VERIFIED https://cloud.google.com/terms/service-terms §18 Training Restriction "Google will not use Customer Data to train or fine-tune any AI/ML models without Customer's prior permission or instruction."]
- [INFERRED] These are Google Cloud-wide statements. Firestore is a GCP-terms service, so they apply. I found no Firebase-specific statement on ads or training.

**Data location**
- Firestore location is selectable: [VERIFIED https://firebase.google.com/support/privacy "For some services, you can make a specific data location selection, which restricts processing to that location", with Cloud Firestore listed]
- **Auth is US-only**: [VERIFIED same "The Firebase Authentication service is run only from US data centers. As a result, Firebase Authentication processes data exclusively in the United States."]
- [INFERRED] Habit data in asia-south1 stays in India at rest. The user's email and Google identity live in US Auth.

**Terms acceptance**
- The ToS acceptance text reads: [VERIFIED https://firebase.google.com/terms "I agree that my use of any Firebase service is for purposes related to my trade, business, craft, or profession"]
- [INFERRED] This is a minor point for a purely personal app. The owner should be aware of it when accepting.

**Encryption**
- [VERIFIED https://firebase.google.com/support/privacy "Firebase services encrypt data in transit using HTTPS"; Cloud Firestore and Authentication are listed as encrypting data at rest]

---

## Gotchas for this project

1. **No server code at ₹0.** Cloud Functions and Cloud Run need Blaze. All logic lives in the clients plus Security Rules: validation, migrations, aggregates, revocation.
2. **No managed backups at ₹0.** Backups, PITR, managed export and TTL all need billing. Build your own export, ideally from Room (0 reads), to a user-chosen file or Drive.
3. **Cloud Storage is gone on Spark** (since 2026-02-03), including default buckets. Don't plan attachments or blob backups there.
4. **Read quota is the only real limit.** 50,000 reads per day. A full-collection listener re-reads everything after more than 30 minutes offline or on each web cold start. Scope queries to a delta window or recent range.
5. **Two offline queues** (Room sync queue plus Firestore's SQLite mutation queue, which survives restarts and is per uid) can diverge. Use client-generated IDs, idempotent upserts and a client `updatedAt`. Don't rely on `serverTimestamp` for event time. Transactions fail offline.
6. **Last write wins by server arrival, not by client time.** A stale device can clobber newer data. Enforce `request.resource.data.updatedAt > resource.data.updatedAt` in rules, or make events append-only.
7. **The Spark over-quota response is a shutdown, not a bill.** The docs conflict on duration ("remainder of that month" vs daily reset). Handle `RESOURCE_EXHAUSTED` gracefully and keep working offline from Room.
8. **Pick the Firestore location once.** asia-south1 or asia-south2, and it cannot be changed. One free database per project; the first one created gets the free quota.
9. **Don't click "upgrade to Identity Platform"** (Spark limit becomes 3,000 DAU) or anything that links billing (auto-upgrades to Blaze).
10. **Revoking refresh tokens needs the Admin SDK.** This works from a local script at ₹0. `auth_time` is a standard ID-token claim but is **not listed** in the Firestore rules reference. Verify a "revokedBefore" rule in the emulator first.
11. **Web.** Firebase Hosting with `output: 'export'`; don't use framework-aware Hosting or App Hosting. Set `authDomain` to the web.app domain, or use `signInWithPopup`. Web persistence is opt-in, and its cache persists on shared devices.
12. **Android.** Use Credential Manager with `GetGoogleIdOption` and the **Web** client ID as server client ID. Register SHA-1 for both the debug and release keystores. Sideloading works [INFERRED]; App Check with Play Integrity would need a Play Console link, so skip App Check or use the debug provider.
13. **Auth processes data only in the US.** Firestore data can stay in India.
14. **Demo-project emulators** (`demo-*`) allow fully offline rules testing with no Google account. Use `@firebase/rules-unit-testing` (v9 library), Node ≥16 and JDK ≥11.
