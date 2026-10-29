# Android widget findings (Phase 0)

**Status:** research and local tests are done. **Nothing in this file has been tested on your OPPO F29 yet.** The phone test is the next step: see [device-test-protocol.md](device-test-protocol.md).

## 1. Can a widget detect a double-tap?

**Not natively.** Android's widget documentation says: *"The only gestures available for widgets are touch and vertical swipe."* Widgets are drawn by the launcher (your home-screen app), not by our app. All we can attach is "when this area is clicked, run this action". There are no gesture detectors, no custom views, and no long-press, because long-press belongs to the launcher for moving and resizing widgets. Sources: [research/android-widget.md §2](research/android-widget.md).

**What we can do is simulate one.** Each tap arrives separately, so we remember when the last one arrived. If the second comes within a short window (e.g. 400 ms), we treat the pair as a double-tap. That logic is built and unit-tested (`DoubleTapDetector`, 7 tests).

**Why it may not feel reliable (to be measured on the phone):**
- **The first tap can't do anything visible.** The handoff says an ordinary tap must not navigate, so a single tap on the graph does nothing. That may feel like the widget is broken.
- **Taps travel through the system, and the app may need to start up.** If ColorOS has frozen our app, the first tap has to start it, which can take hundreds of milliseconds or more. The gap between two taps *as received* can then differ from the gap you actually tapped.
- **ColorOS may restrict background apps aggressively.** OPPO publishes no developer guidance for this. Third-party reports, which are unofficial, describe apps being killed when the screen turns off.

The test build records every graph tap: the gap since the previous tap, whether it counted as a double-tap, and whether it had to start the app. So we'll have numbers, not guesses.

## 2. Options built into the test app (switch between them in the app)

| Mode | How you get to quick entry | Status |
|---|---|---|
| **Double-tap graph (experimental)** | Tap the graph twice quickly | Built for measurement only. Not recommended unless the phone test is excellent. |
| **"Log" button toggles quick entry** (recommended fallback) | Tap a small **Log** button; the widget itself switches to quick-entry controls. **Done** switches back, and so does an automatic return after 90 s. Tapping the graph does nothing. | Built. Uses only documented widget features. |
| **"Log" button opens a quick-entry screen** | Tap **Log**: a small panel opens over the home screen (it doesn't open the full app). | Built. Most robust (full touch controls), but technically it opens a screen. |

**My recommendation, for your approval:** use the **Log toggle button** as the main way in. Keep the double-tap only if the phone test meets an agreed bar. I suggest: at least 19 of 20 double-taps recognised, 0 of 20 single taps causing a switch, works after the app was idle, and a delay you find acceptable. I will not switch to a fallback without your approval.

## 3. What quick entry shows (the checkbox vs +/- conflict)

Both variants are built so you can compare them on the phone:

- **Checkboxes only:** every habit is one tick. For amount habits (water, protein), ticking marks the day "done" **without inventing an amount**, as the handoff requires. The amount you actually logged is kept as it was.
- **+/- for amounts, checkbox for yes/no:** water and protein get − / + buttons using each habit's step (0.5 L, 10 g). Skincare gets a checkbox.

The second variant looks like it resolves the conflict, since it covers both earlier answers. It is still your call.

## 4. Contribution graph inside the widget

| Finding | Evidence |
|---|---|
| A grid of individual squares won't work in a widget. Glance keeps only the first 10 children of each row or column and silently drops the rest. | Glance source code ([research §3](research/android-widget.md)) |
| The graph is drawn as **one image** at the widget's exact pixel size. | `HeatmapRenderer`; rendered images in [evidence/](evidence/) |
| A 4×2 widget shows **23 weeks**. Tall widgets still show 20 or more weeks; a layout bug causing only 10 was found and fixed by a test. | `HeatmapLayoutTest`, `HeatmapRendererTest` |
| Image size for a 4×2 widget: about 1.0 MB at full colour. **The widget uses 16-bit colour (489 KB)**, which Google's guidance recommends for opaque images. Flat squares look the same. | `HeatmapRendererTest` prints sizes |
| Android's widget image limit on the F29 (1080×2412 screen) is 6 × 1080 × 2412 = **15.6 MB**, about 30 times what we use. | AOSP source ([research §4](research/android-widget.md)); our arithmetic is tested |
| The graph's colours come from the **shared scoring engine**, so the widget, app and web can't disagree about a day. | `HeatmapLayoutTest.levelsComeFromTheSharedScoringEngine` |

## 5. Things to expect on ColorOS / Android 15–16

- **Background restrictions:** if the widget stops updating, allow *Auto launch* and *background activity* and set battery to *Unrestricted* (Settings → Apps → Brain Rot P0). This is from third-party sources; OPPO has no developer documentation on it.
- **Force-stopping greys out widgets** (an Android 15+ rule). They come back when you open the app.
- **Widget sizes:** the launcher might not report exact widget sizes. If resizing doesn't redraw the graph to fit, that's this.
- **Midnight:** the test widget refreshes when data changes, not at midnight. A proper midnight refresh is Phase 3 work. Android's own timer can't fire more often than every 30 minutes.
- **Android 16 on the F29:** OPPO hasn't officially confirmed a stable Android 16 build for the non-Pro F29; a beta was reported in Nov 2025. Please record the exact build number (Settings → About device).

## 6. Reminders (for later phases)

The 11 PM catch-up reminder needs the notification permission (off by default on new installs since Android 13). Exact alarms aren't granted by default on Android 14+. For a gentle reminder, a normal alarm that fires within about 10–15 minutes of 23:00 needs no special permission. On ColorOS, the background settings above matter more than the alarm type.
