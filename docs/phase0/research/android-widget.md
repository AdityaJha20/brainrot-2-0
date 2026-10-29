# Android home-screen widget feasibility: OPPO F29 (India), Glance habit widget with double-tap

Date checked: 2026-10-29

Tags:
- [VERIFIED url "quote"] means I read it on an official or primary page today. Primary includes AOSP and AndroidX source on android.googlesource.com.
- [VERIFIED-3P url "quote"] means I read it today on a third-party page. It is reliable enough to use but is not official.
- [UNVERIFIED/INFERRED] means it comes from a search snippet, my own reasoning, or a source I could not render.

Note on OPPO pages: OPPO Community pages (community.oppo.com, communityin.oppo.com) and support.oppo.com answer pages build their content with JavaScript. WebFetch and curl returned only page shells, so claims that rely on them are marked UNVERIFIED.

---

## 1. OPPO F29 (non-Pro, India, 2025)

| Item | Value | Source |
|---|---|---|
| Chipset | Qualcomm Snapdragon 6 Gen 1 (SM6450, 4 nm). CPU is 4x A78 @2.2 GHz + 4x A55 @1.8 GHz, GPU is Adreno 710 | [VERIFIED https://www.oppo.com/in/newsroom/press/oppo-f29-durable-champion-launched-india/ "F29 5G: Snapdragon 6 Gen 1"; F29 Pro is "MediaTek Dimensity 7300 Energy"] [VERIFIED https://www.oppo.com/in/smartphones/series-f/f29-5g/specs/ "4 Arm Cortex-A78 cores@2.2GHz", "4 Arm Cortex-A55 cores@1.8GHz"] [VERIFIED-3P https://m.gsmarena.com/oppo_f29-13722.php "Qualcomm SM6450 Snapdragon 6 Gen 1 (4 nm)"] |
| Display | 6.7" (17.02 cm) AMOLED, FHD+ **2412 x 1080**, **394 ppi**, 120 Hz | [VERIFIED https://www.oppo.com/in/smartphones/series-f/f29-5g/specs/ "FHD+ 2412 × 1080 Pixels", "394 PPI", "17.02cm (diagonal)"] |
| Density bucket | 394 ppi is near xxhdpi. Android usually reports about 2.75x–3.0x density (about 360–392 dp wide), but the exact dp width depends on OPPO's default "display size" setting | [UNVERIFIED/INFERRED: read `adb shell wm density` and `wm size` on the device] |
| RAM / storage | 8 GB + 128 GB (Rs 23,999) and 8 GB + 256 GB (Rs 25,999). LPDDR4X RAM, UFS 3.1 storage | [VERIFIED https://www.oppo.com/in/smartphones/series-f/f29-5g/specs/ "8GB + 128GB", "8GB + 256GB", "LPDDR4X"] [VERIFIED press release above: prices. The text has a typo "8GB+12GB", but the spec table says 128GB] |
| Launch | Announced 20 Mar 2025, on sale 27 Mar 2025 (India) | [VERIFIED press release above] [VERIFIED-3P gsmarena "2025, March 20" / "Released 2025, March 27"] |
| Launch OS | ColorOS 15 based on Android 15 | [VERIFIED https://www.oppo.com/in/smartphones/series-f/f29-5g/specs/ "ColorOS 15.0"] [VERIFIED press release: ColorOS 15 based on Android 15] |
| Update policy | 2 years of OS updates and 3 years of security patches. That likely means ColorOS 16 and 17 (Android 16 and 17) | [VERIFIED press release above: "2 years of OS updates and 3 years of security patches"]. The ColorOS 17 inference is [UNVERIFIED/INFERRED] |
| ColorOS 16 / Android 16 for **F29 (non-Pro)** | **Status is unclear, and I could not confirm a stable build from an official page.** (a) Ytechb (10 Nov 2025) reports OPPO "silently pushed the beta update" of ColorOS 16 to the OPPO F29, and embeds a 6 Nov 2025 @OPPOIndia tweet naming "OPPO F29 5G". (b) Cashify lists the F29 as eligible. (c) The Feb 2026 roundups (nokiamob, Gizmochina) list the **F29 Pro 5G** as updated but **do not list the non-Pro F29**. The user's statement that the phone runs Android 16 fits either a beta/early-access build or a later stable build | [VERIFIED-3P https://www.ytechb.com/oppo-expands-coloros-16-beta-to-oppo-f29-and-oppo-a5-pro/ "silently pushed the beta update"] [VERIFIED-3P https://nokiamob.net/2026/02/05/oppo-coloros-16-update-list-which-devices-have-getting-android-16/ updated list: "the F series (F29 Pro 5G, F27 5G, F25 Pro)"] [UNVERIFIED cashify.in list] |
| ColorOS 16 for **F29 Pro** (for comparison) | Stable release in India on 30 Dec 2025, build CPH2705_16.0.2.401(EX01) | [VERIFIED-3P https://www.thetechoutlook.com/news/os/oppo-officially-announces-the-release-of-coloros-16-stable-update-for-oppo-f29-pro-5g-in-india/ "On 30th December", "CPH2705_16.0.2.401(EX01)"] |
| Stock launcher | ColorOS "System Launcher", package `com.android.launcher`, signed with OPPO's key. On older ColorOS builds the package was `com.oppo.launcher` | [VERIFIED-3P https://sysapps.bayton.org/packages/com.android.launcher label "System Launcher", "Signed by the device maker's own signing key (OPPO)"] [UNVERIFIED gist snippet for com.oppo.launcher]. **Action:** run `adb shell cmd package resolve-activity -a android.intent.action.MAIN -c android.intent.category.HOME` on the phone to confirm |

**Action for the team:** read Settings > About device for the exact ColorOS build, Android version and security patch, and record them alongside the test results.

---

## 2. Widget touch model (RemoteViews)

- Widgets support only touch and vertical swipe. [VERIFIED https://developer.android.com/develop/ui/views/appwidgets/overview "The only gestures available for widgets are touch and vertical swipe."] Horizontal swipe is reserved for paging between home screens. [same page: "that gesture is already taken on the home screen for the purpose of navigating between home screens."]
- **No official doc mentions double-tap**, and RemoteViews has no API for it. The click APIs are `setOnClickPendingIntent`, which is equivalent to `View#setOnClickListener` launching a PendingIntent, plus collection templates (`setPendingIntentTemplate` / `setOnClickFillInIntent`) and, from API 31, `setOnCheckedChangeResponse` for compound buttons. There is no long-click or touch-listener API. [VERIFIED AOSP RemoteViews.java https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/widget/RemoteViews.java "Equivalent to calling View#setOnClickListener ... to launch the provided PendingIntent"; grep found no `setOnLongClick*`]
- Supported views are limited to a fixed list, with "Descendants of these classes are not supported". Layouts: AdapterViewFlipper, FrameLayout, GridLayout, GridView, LinearLayout, ListView, RelativeLayout, StackView, ViewFlipper. Widgets: AnalogClock, Button, Chronometer, ImageButton, ImageView, ProgressBar, TextClock, TextView. API 31 adds CheckBox, RadioButton, RadioGroup and Switch. [VERIFIED AOSP RemoteViews.java class javadoc, quoted above] You also cannot use custom views. [VERIFIED https://developer.android.com/develop/ui/views/appwidgets "You can't use custom views or subclasses of the views that are supported by RemoteViews."] **This means you cannot attach a GestureDetector.**
- Long-press belongs to the launcher, which uses it to move, resize, remove and reconfigure widgets. [VERIFIED https://support.google.com/android/answer/9450271 resize: "On your home screen, touch and hold the widget"] [VERIFIED https://developer.android.com/develop/ui/compose/glance/configuration "Users can reconfigure their widget by touching & holding the widget and tapping the Reconfigure button"]
- **Possible double-tap hack (not recommended):** you could detect two clicks within N ms in the click handler, by timestamping in DataStore inside an ActionCallback. Every tap would still be delivered as a click, so the first tap would always do something (or nothing). Its latency is bounded by broadcast delivery and process start (see §3), so a reliable 300 ms window cannot be guaranteed. [UNVERIFIED/INFERRED from the click-only API plus the broadcast-receiver execution model]

---

## 3. Jetpack Glance

**Versions**
- Latest stable is **1.2.0 (26 Aug 2026)**. Before that came 1.2.0-rc01 (3 Dec 2025) and 1.2.0-beta01 (27 Aug 2025). The latest alpha is 1.3.0-alpha02 (1 Jul 2026). [VERIFIED https://developer.android.com/jetpack/androidx/releases/glance table: "August 26, 2026 | 1.2.0 | - | - | 1.3.0-alpha02"]
- 1.2.0's notes say "Performance improvements, bug fixes, and deprecation of legacy wear library". The 1.2.0-alpha01 additions include generated-preview APIs (`providePreview` / `setWidgetPreview`), `MultiProcessGlanceAppWidget`, non-experimental lambda actions and Image alpha. [VERIFIED same page]
- **minSdk:** 1.2.0-beta01 raised the default minSdk from 21 to **23**. [VERIFIED same page]
- 1.3.0-alpha01 needs compileSdk 37 and AGP 9.2.0+, and adds Remote Compose snap scrolling. Snap scrolling only works on **Android 17+** devices, so it does not apply to an Android 16 F29. [VERIFIED same page] [VERIFIED https://developer.android.com/develop/ui/compose/glance/build-ui "snap scrolling is supported on devices running Android 17 and higher"]

**Layout limits**
- **Max 10 children per Row/Column/Box.** Glance silently drops the rest: `children.take(10)`. [VERIFIED AndroidX source https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/RemoteViewsTranslator.kt "children.take(10).forEachIndexed"] [VERIFIED LayoutGenerator.kt in glance-layout-generator "private const val MaxChildCount = 10"]
- **Max 500 generated view IDs per widget.** Exceeding it fails with "There are too many views". [VERIFIED LayoutGenerator.kt "internal const val TotalViewCount = 500"; RemoteViewsTranslator.kt `check(it < TotalViewCount) { "There are too many views" }`]
- These limits mean a 7 x 20 grid (140 cells) cannot sit in one Row of 20 columns. Nested Rows that each hold at most 10 Columns of 7 Boxes would work in principle, using about 140 Boxes plus containers. That is a heavy RemoteViews tree. Rendering the graph as one `Image(ImageProvider(bitmap))` is simpler and cheaper. [UNVERIFIED/INFERRED]
- `LazyColumn` maps to a `ListView`, so RemoteViews collection limits apply. [VERIFIED build-ui page "Glance translates a LazyColumn into a ListView... the same limitations and restrictions of RemoteViews collections apply"] A `LazyVerticalGrid` translator also exists. [VERIFIED source dir listing translators/LazyVerticalGridTranslator.kt]
- **Images:** use `ImageProvider(resId)`, `ImageProvider(bitmap)`, `ImageProvider(uri)` or `ImageProvider(icon)`. The docs recommend resource IDs because they keep RemoteViews small. [VERIFIED build-ui "We recommend providing resource IDs directly to reduce the size of the final RemoteViews object"] `ImageProvider(bitmap)` counts toward the bitmap budget. [VERIFIED https://developer.android.com/about/versions/17/behavior-changes-17 "Jetpack Glance ImageProvider(bitmap)"]
- **CheckBox / Switch / RadioButton:** on API 31+ these render as native compound buttons. Below API 31 Glance uses a "Backport" layout, an ImageView-based emulation. [VERIFIED source translators/CheckBoxTranslator.kt: `if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ... else LayoutType.CheckBoxBackport`] [VERIFIED build-ui "Glance supports backwards compatibility for ... Switch, CheckBox, RadioButton"] The new checked state reaches the action through `ToggleableStateKey`. [VERIFIED CheckBox.kt KDoc] The platform docs say "The widget is still stateless. Your app must store the state". [VERIFIED https://developer.android.com/develop/ui/views/appwidgets]
- **SizeMode:** `Single` is the default. `Responsive` composes once per declared DpSize and the launcher picks the best fit on API 31+. `Exact` recomposes on every size change. [VERIFIED build-ui page quotes]

**Actions and latency**
- The action types are `actionStartActivity`, `actionStartService`, `actionSendBroadcast`, `actionRunCallback<ActionCallback>`, plus lambdas. [VERIFIED https://developer.android.com/develop/ui/compose/glance/user-interaction]
- `ActionCallback` runs in "a custom async BroadcastReceiver ... This allows extra execution time, but certain restrictions apply. Any long or consuming tasks should be offset into ... a Worker." Lambda actions "run in the context of a WorkManager worker that is run in a Service". [VERIFIED same page]
- You cannot start activities from lambdas or trampolines on Android 12+, so use `actionStartActivity`. [VERIFIED same page]
- Receivers are limited to 10 s. [VERIFIED https://developer.android.com/develop/ui/compose/glance/create-app-widget "strict 10-second background limits"] [VERIFIED https://developer.android.com/develop/ui/views/appwidgets/advanced "up to 10 seconds before ... ANR"]
- **Latency:** a tap sends a broadcast, which may cold-start the app process, then reads DataStore/Room, recomposes, serialises RemoteViews and pushes them to the launcher. Expect a visible delay of hundreds of ms on a cold process, longer if ColorOS has frozen the app. There is no official number. [UNVERIFIED/INFERRED] Docs suggest `Intent.FLAG_RECEIVER_FOREGROUND` for click broadcasts. [VERIFIED advanced page]

**Session, updates and state**
- `provideGlance` runs on the main thread. `provideContent` runs once per DpSize bucket. [VERIFIED create-app-widget page]
- The composition session runs in a WorkManager `SessionWorker`. Default timeouts are `initialTimeout = 45.seconds`, `additionalTime = 5.seconds` and `idleTimeout = 5.seconds`; the idle timeout applies when the device is in idle or low-power mode. [VERIFIED source https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/glance/glance/src/main/java/androidx/glance/session/SessionWorker.kt "val initialTimeout: Duration = 45.seconds"] This depends on WorkManager, so OEM job restrictions can delay first render or refresh. [UNVERIFIED/INFERRED]
- `update(context, glanceId)`, `updateAll(context)` and `updateIf<State>` are suspend functions and should be called off the main thread. [VERIFIED https://developer.android.com/develop/ui/compose/glance/glance-app-widget]
- `updateAppWidgetState` "doesn't automatically recompose your widget ... Always call update()/updateAll() after". [VERIFIED create-app-widget page]
- Glance-specific state (for example "a checkbox was selected in the widget") is meant for exactly this kind of mode flag. [VERIFIED glance-app-widget page]
- Periodic refresh: `updatePeriodMillis` fires at most every 30 min. For more frequent refresh use WorkManager (15 min minimum), and avoid per-minute updates. [VERIFIED glance-app-widget page]

**Testing**
- Artifacts: `androidx.glance:glance-testing` and `androidx.glance:glance-appwidget-testing`. The docs page shows version 1.1.1, which is stale relative to 1.2.0. [VERIFIED https://developer.android.com/develop/ui/compose/glance/testing]
- `runGlanceAppWidgetUnitTest { provideComposable { ... }; onNode(hasTestTag(..)).assertHasText(..) }` has a 1 s default timeout and a default size of 349 x 455 dp. Context comes from Robolectric. [VERIFIED same page]
- "The test doesn't render the composables under test, so it doesn't let you perform clicks". You can assert which action is wired to a node, but not tap timing, so double-tap logic must be tested on a device. [VERIFIED same page]

---

## 4. Bitmaps in widgets

- AOSP `AppWidgetServiceImpl.computeMaximumWidgetBitmapMemory()` sets the budget to **6 x w x h** bytes, i.e. 1.5 x a full-screen ARGB_8888 buffer:
  ```
  // Cap memory usage at 1.5 times the size of the display
  // 1.5 * 4 bytes/pixel * w * h ==> 6 * w * h
  mMaxWidgetBitmapMemory = 6 * size.x * size.y;
  ```
  When `widget.views.estimateMemoryUsage() > mMaxWidgetBitmapMemory`, the system throws `IllegalArgumentException("RemoteViews for widget update exceeds maximum bitmap memory usage ...")`. [VERIFIED https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/services/appwidget/java/com/android/server/appwidget/AppWidgetServiceImpl.java (lines ~443-449, ~2852)] **The same unconditional throw is in the `android16-release` branch** (`long memoryUsage`). [VERIFIED .../refs/heads/android16-release/... lines ~489, ~2894]
- Android 17 changes, which only matter if the app targets API 37: the limit is enforced (`IllegalArgumentException`) for apps targeting 37+. Apps targeting 36 or lower only log a warning. Bitmap-backed Icons are now counted. URI images skip the IPC count but are decoded at full resolution in the launcher. [VERIFIED https://developer.android.com/about/versions/17/behavior-changes-17 "For apps targeting API level 36 or lower, the system logs this memory violation as a warning"; "6 * width * height bytes"] **Note:** the Android 16 AOSP code above throws for all non-system callers, so on the Android 16 F29, assume an over-budget update fails no matter which SDK you target. [VERIFIED code; the interpretation is INFERRED]
- **Budget for the OPPO F29 (1080 x 2412):** 6 x 1080 x 2412 = **15,629,760 bytes (~14.9 MiB)** per widget update, summed over all size buckets in the RemoteViews. [Computed from VERIFIED inputs. If ColorOS reports a different real display size (e.g. a resolution-scaling setting), recompute from `WindowManager.currentWindowMetrics.bounds` as Google recommends.]
- Binder: "The Binder transaction buffer has a limited fixed size, currently 1MB, which is shared by all transactions in progress for the process." [VERIFIED AOSP https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/os/TransactionTooLargeException.java] Large bitmaps in parcels normally travel as ashmem/shared-memory blobs rather than inline binder data, so the effective limit for widget bitmaps is the 6·w·h budget. Keep the non-bitmap RemoteViews small all the same. [UNVERIFIED/INFERRED]
- Recommended practice [VERIFIED behavior-changes-17 and https://developer.android.com/develop/ui/views/appwidgets]:
  - Downsample bitmaps to the widget's max dp size.
  - Use RGB_565 for opaque images.
  - Decode once in `provideGlance` *before* `provideContent`, so every size bucket shares one Bitmap instance.
  - Use full updates, not `partiallyUpdateAppWidget`, for image swaps.
  - Override `onCompositionError` to show a bitmap-free fallback.
  - Inspect usage with `adb shell dumpsys appwidget`.
- **Contribution graph sizing (example):** a 4 x 2 widget is about 330 x 160 dp, which is about 990 x 480 px at ~3x density. As ARGB_8888 that is ~1.9 MB. With 3 Responsive buckets sharing one bitmap it stays ~1.9 MB; with separate bitmaps per bucket it is ~5.7 MB. Both fit well under 14.9 MiB. A tiny 20 x 7 px bitmap would scale blurrily, so draw cells at the target pixel size. [UNVERIFIED/INFERRED arithmetic]

---

## 5. Android 12–16 widget changes relevant here

- **Android 12 (API 31):**
  - `system_app_widget_background_radius` and `system_app_widget_inner_radius`; manufacturers can set the radius up to 16 dp. [VERIFIED https://developer.android.com/about/versions/12/features/widgets] The background radius is "never larger than 28 dp", and Glance applies it with `GlanceModifier.cornerRadius(...)` on S+. [VERIFIED create-app-widget page]
  - Stateful CheckBox/Switch/RadioButton. [VERIFIED 12 widgets page "Android 12 adds new support for stateful behavior ... CheckBox, Switch, RadioButton"]
  - Responsive and exact layouts, `targetCellWidth/Height`, `maxResizeWidth/Height`. [VERIFIED https://developer.android.com/develop/ui/views/appwidgets/layouts "Each widget must define a targetCellWidth and targetCellHeight for devices running Android 12 or higher"]
  - The launcher may not supply `OPTION_APPWIDGET_SIZES`. [VERIFIED layouts page "If the device has a launcher that doesn't support that field, the list of sizes might be empty or null."] **Relevant for ColorOS.**
  - `widgetFeatures="reconfigurable|configuration_optional"`. [VERIFIED https://developer.android.com/develop/ui/compose/glance/configuration]
- **Android 15 (API 35):**
  - Generated previews need compileSdk 35+, and `setWidgetPreview` is rate-limited to about 2 calls/hour. [UNVERIFIED search snippets of https://developer.android.com/develop/ui/views/appwidgets/previews] Glance 1.2 has `providePreview`. [VERIFIED Glance release notes]
  - Force-stop disables widgets: "the system temporarily disables all the app's widgets. The widgets are grayed out ... re-enables those widgets the next time the user launches the app", because all PendingIntents are cancelled. [VERIFIED https://developer.android.com/about/versions/15/behavior-changes-all] **Relevant: ColorOS task killers may act like a force-stop.** [INFERRED]
- **Android 16 (API 36):**
  - No widget-specific behaviour change appears on the all-apps or targeting-36 pages. [VERIFIED https://developer.android.com/about/versions/16/behavior-changes-all and .../behavior-changes-16 (no widget sections)]
  - JobScheduler/WorkManager runtime quotas are now enforced per standby bucket, including for active apps. [VERIFIED behavior-changes-all "active standby buckets will start being enforced by a generous runtime quota" ... "This change impacts tasks scheduled using WorkManager"]
  - Edge-to-edge opt-out is removed and predictive back is on by default when targeting 36. [VERIFIED behavior-changes-16] This matters for any quick-entry Activity.
  - Widget engagement metrics (`AppWidgetManager.queryAppWidgetEvents`, `RemoteViews.setAppWidgetEventTag`) need `@RequiresApi(Build.VERSION_CODES_FULL.BAKLAVA_1)` and compileSdk 36.1. That corresponds to the Android 16 QPR2 release, so **it may not be available on ColorOS 16 if it is based on 16.0**. [VERIFIED https://developer.android.com/develop/ui/compose/glance/metrics; the QPR2/ColorOS mapping is INFERRED]
- **updatePeriodMillis:** "will not be delivered more than once every 30 minutes". [VERIFIED AOSP AppWidgetProviderInfo.java] "updatePeriodMillis doesn't support values of less than 30 minutes ... specify 0" to disable. [VERIFIED advanced page]

---

## 6. ColorOS specifics

- **No OPPO developer documentation on widgets or background execution found.** OPPO's open-platform docs did not appear in searches. [UNVERIFIED: absence of evidence]
- OPPO support (official, but the page would not render for me): Settings > Apps > Auto launch lets you pick apps. The page warns you "may not be able to receive messages when the feature is disabled". [UNVERIFIED search snippet of https://support.oppo.com/au/answer/?aid=neu4332]
- Per-app path: Settings > Apps > [app] > Battery usage > "Allow background activity" (also "Allow auto launch" on ColorOS 11.1+). [UNVERIFIED third-party: AirDroid, DriveQuant, techbone.net]
- dontkillmyapp.com (unofficial):
  - "Background services are being killed ... each time the screen turns off".
  - Fixes: pin the app in Recents, enable it in the Startup manager / floating list (com.coloros.safecenter), "Turn off battery optimizations", allow Auto-launch, and set Battery usage to "Run in background".
  - Developer side: "No known solution on the dev end".
  - The page does not mention widgets, alarms or WorkManager.
  [VERIFIED-3P https://dontkillmyapp.com/oppo (UNOFFICIAL)]
- ColorOS 16 user reports of aggressive background killing (Reno13, firmware 16.0.x), with workarounds like removing `com.oplus.athena` via adb. [UNVERIFIED community snippet https://community.oppo.com/thread/2133887041509785608. Do not recommend removing system packages.]
- No OPPO-specific source found on widget staleness or delayed clicks on the ColorOS launcher. The general pattern is that widgets "show but do not refresh" because the providing app is restricted in the background. [UNVERIFIED third-party drfone/guidingtech snippets]
- **Practical checklist for the test phone:**
  1. Enable Auto launch.
  2. Turn on Allow background activity.
  3. Set battery to Unrestricted / no optimisation.
  4. Lock the app in Recents.
  5. Re-test after a reboot and after the screen has been off overnight.
  [INFERRED from the above]

---

## 7. Notifications and reminders (one daily 11 PM reminder)

- `POST_NOTIFICATIONS` is a runtime permission on Android 13+. "your app's notifications are off by default" for new installs, and apps targeting 13+ choose when to prompt. [VERIFIED https://developer.android.com/develop/ui/views/notifications/notification-permission]
- `SCHEDULE_EXACT_ALARM` is "no longer being pre-granted to most newly installed apps targeting Android 13 and higher" on Android 14+. Without it, `setExact()`, `setExactAndAllowWhileIdle()` and `setAlarmClock()` throw SecurityException. `USE_EXACT_ALARM` is for calendar and alarm-clock apps and is gated by Play policy. [VERIFIED https://developer.android.com/about/versions/14/changes/schedule-exact-alarms]
- Inexact alarm timing [VERIFIED https://developer.android.com/develop/background-work/services/alarms/schedule]:
  - `set()`, `setInexactRepeating()` and `setAndAllowWhileIdle()` never fire early. On 12+ they fire "within one hour of the supplied trigger time", unless Doze or battery saver delays them.
  - `setWindow()` windows are clipped to at least 10 minutes.
  - "By default, all alarms are canceled when a device shuts down", so re-register on BOOT_COMPLETED.
  - Revoking SCHEDULE_EXACT_ALARM stops the app and cancels its exact alarms.
- Android 15: force-stop cancels all PendingIntents, alarms included. They are restored after the user relaunches the app (BOOT_COMPLETED is delivered then). [VERIFIED behavior-changes-15-all]
- **Recommendation for 11 PM:**
  - For a habit nudge, use `setAndAllowWhileIdle` or `setWindow(23:00, 10–15 min)`, or a WorkManager OneTimeWorkRequest re-enqueued daily. This needs no special permission beyond POST_NOTIFICATIONS.
  - If "exactly 23:00" is a hard requirement for a personal sideloaded build, `SCHEDULE_EXACT_ALARM` can be requested through `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`. `USE_EXACT_ALARM` is policy-limited on Play, though irrelevant when sideloading.
  - On ColorOS, any alarm can still be lost if the app is frozen or killed. Auto-launch and background permissions matter more than the alarm API.
  [INFERRED]

---

## 8. Documented-to-work alternatives to double-tap

1. **Explicit mode toggle inside the widget.** Add a small "+"/"Log" or pencil button whose `actionRunCallback` flips a Glance state key (`updateAppWidgetState` then `update()`), so the widget re-renders in quick-entry layout. Add a "Done"/"x" button or an auto-revert (see Gotchas) to return. Every other area gets no click modifier, or opens the app if the product wants that. Single taps on the graph then cannot navigate. [Built from VERIFIED Glance state and update docs in §3]
2. **Native checkboxes in quick-entry mode (API 31+).** CheckBox with `onCheckedChange = actionRunCallback<ToggleHabit>()` writes to Room/DataStore without opening the app. [VERIFIED Glance CheckBox KDoc and §2/§5 refs] Keep habit rows to at most 10 per Column; use LazyColumn if there are more. [VERIFIED limits §3] +/- buttons are ordinary clickable Buttons or Images, each running its own callback.
3. **Separate quick-entry widget.** A second `GlanceAppWidgetReceiver` with a compact always-on checklist, next to the graph widget. Users place one or both. [INFERRED; standard multi-provider pattern]
4. **Tap opens a translucent quick-entry Activity** (`actionStartActivity<QuickLogActivity>()` with `Theme.Translucent`/dialog). It gives full Compose UI, real gestures and IME, and is reliable against process death. The cost is an activity launch animation from the widget bounds. Avoid trampolines and lambdas. [VERIFIED user-interaction page trampoline note; Android 12 transition note https://developer.android.com/develop/ui/views/appwidgets/enhance "launchers provide a smoother transition when a user launches your app from a widget"]
5. **Configuration activity** (`widgetFeatures="reconfigurable|configuration_optional"`) for choosing which habits and how many weeks to show, reachable via long-press > Reconfigure on Android 12+. Not for daily logging. [VERIFIED configuration page] Whether ColorOS's launcher shows a "Reconfigure" item still needs testing on the device [UNVERIFIED].

UX caveats [UNVERIFIED/INFERRED]:
- Each tap shows the launcher's ripple/pressed state only on views with a click action, so the graph area should have no action.
- State changes go through broadcast, recompose and a launcher re-inflate, so expect ~0.2–1+ s lag. Show an optimistic checked state (native CheckBox toggles locally on API 31+ before the callback).
- After process death or a ColorOS freeze, the first tap can be slow or dropped.

---

## 9. Sideloading a debug APK on ColorOS / Android 16

- Since Android 8 the global "Unknown sources" setting is gone. The per-source "Install unknown apps" permission replaces it. [VERIFIED https://developer.android.com/about/versions/oreo/android-8.0-changes "the Install unknown apps permission manages unknown app installs from unknown sources"]
- ColorOS menu path: Settings > Security (or Password & security > System security) > "Unknown source installations"/Install sources, granted per app (Files, Chrome, etc.). [UNVERIFIED third-party https://www.techbone.net/oppo/smartphone/install-unknown-apps]
- A ColorOS 16 March 2026 update thread mentions a "Virus Scan" feature, and OPPO's installer may scan the APK or warn about it. [UNVERIFIED thread title https://community.oppo.com/thread/2078502973029220361]
- USB/ADB installs need Developer options plus USB debugging. ColorOS may also require "Disable permission monitoring"/"Install via USB" toggles. [UNVERIFIED third-party snippets] Check on the device.
- **Google developer verification (current status, read today):**
  - Timeline: in **August 2026** limited-distribution accounts and the "advanced flow" launched. On **30 Sep 2026** protections began "for all users who install apps from participating stores in **Brazil, Indonesia, Singapore, and Thailand** on certified devices running Android 7+". Global expansion follows in **2027**. [VERIFIED https://developer.android.com/developer-verification]
  - Participating stores: Google Play, HONOR App Market, **OPlus (OPPO App Market)**, Galaxy Store, Palm Store, V-Appstore and GetApps. [VERIFIED https://developer.android.com/developer-verification/guides]
  - **India is not listed** for the 2026 phase. [VERIFIED by absence on the three pages]
  - **ADB is exempt.** "As a developer, you are free to install apps without verification with ADB" and "The waiting period does not apply to ADB installs". [VERIFIED https://developer.android.com/developer-verification/guides/faq]
  - Direct sideloads: "If you distribute your app through other stores, or if users sideload your app directly, these new verification requirements won't apply to your app yet." [VERIFIED FAQ]
  - Limited distribution accounts are free, need no ID and allow up to 20 devices, for students and hobbyists. [VERIFIED FAQ]
  - Advanced flow: a one-time setup with developer mode, an anti-coaching check, a restart, and a **one-day wait**, after which you allow installs for 7 days or indefinitely. [VERIFIED FAQ; https://android-developers.googleblog.com/2026/03/android-developer-verification.html]
  - **Net effect for this project:** installing a debug APK on an Indian OPPO F29 via `adb install` or Android Studio is unaffected today and stays exempt per the FAQ. Browser or file-manager sideloads in India are unaffected until the 2027 global rollout. After that, a free limited-distribution account or the advanced flow covers it.

---

## Implications for the double-tap prototype

1. **True double-tap is not supported by the platform.** Widgets get clicks only (plus vertical scroll in collections), with no gesture detector and no custom views. Long-press belongs to the launcher. Any "double-tap" would be a timing hack over two click broadcasts.
2. **The hack violates the requirement that "single tap must not unexpectedly navigate".** Tap 1 always arrives as a click. To be safe, the graph's click action must do nothing visible. Then a lone tap feels broken, and the double-tap window depends on broadcast and process-start latency (cold start, ColorOS freezing), so it will be flaky. Recommendation: **do not ship double-tap.** If the product insists, prototype it behind a flag only to demonstrate the problem: an ActionCallback stores `lastTapAt` in Glance state, and a second tap within ~400 ms of it switches mode.
3. **Recommended design:** keep the graph area non-clickable. Add one explicit small "Log"/"+" toggle button in a corner that flips a Glance state key to the quick-entry layout. That layout uses native CheckBoxes (API 31+) or +/- buttons for at most 10 habits per column, with a "Done" control. Optionally offer a second quick-entry widget and a tap target that opens a translucent quick-log Activity.
4. **Render the contribution graph as a single bitmap** drawn at widget pixel size, decoded once in `provideGlance`. 7 x 20 Boxes would hit Glance's 10-children-per-container limit and inflate a ~150-view RemoteViews tree. A ~2 MB bitmap is far below the F29's ~14.9 MiB widget budget.
5. **Plan for ColorOS background restrictions.** The widget must render correctly from persisted state after process death. Ask the user to allow Auto-launch and background activity. Treat the 11 PM reminder as best-effort with an inexact/windowed alarm plus BOOT_COMPLETED re-registration.

## Gotchas

- **10-child cap:** Glance silently truncates Row/Column/Box children beyond 10, with no error, and fails hard at 500 views.
- **Responsive buckets:** `provideContent` runs per size bucket. Create bitmaps outside it or memory multiplies.
- **Bitmap limit:** over-budget updates throw `IllegalArgumentException` on Android 16 (AOSP) for all apps. Use `dumpsys appwidget` to check.
- **Mode state can stick:** a "quick-entry" state stored in Glance state survives until reset. Auto-revert with a WorkManager one-shot or on next `onUpdate`, or show a clear "Done" control, so the widget doesn't stay in entry mode for days.
- **Update after state writes:** `updateAppWidgetState` does not refresh the widget. Always call `update()` afterwards.
- **WorkManager dependency:** the Glance session runs on WorkManager (45 s initial timeout, 5 s in idle). Android 16 job quotas and ColorOS restrictions can delay renders.
- **Force-stop:** force-stop (and possibly ColorOS "clean up") greys out widgets on Android 15+ until the app is opened again, and cancels alarms.
- **Launcher sizes:** the ColorOS launcher may not report `OPTION_APPWIDGET_SIZES`, so Responsive/Exact may fall back to the smallest size. Test resizing on the device.
- **Corner radius:** OEMs can clamp it to 16 dp. Use the system dimen.
- **No click on graph cells:** don't give individual graph cells click actions, because that reintroduces accidental navigation and adds PendingIntents.
- **Engagement metrics:** the Android 16 engagement metrics API needs QPR2 (BAKLAVA_1). Don't depend on it for ColorOS 16.
- **Stale Glance testing docs:** they show 1.1.1. Align test artifacts with 1.2.0. Unit tests cannot simulate clicks or timing.
- **F29 Android 16 status is unconfirmed officially.** Record the exact build. If it's a beta build, widget/launcher bugs may be beta-specific.
