# OPPO F29 device test (Phase 0)

About 45–60 minutes, plus one overnight check. **Keep Airplane mode ON for the whole test.** The test build has no internet permission at all, so this also proves everything works offline.

Fill in the results table at the bottom (or just send me notes and screenshots), then use **Share tap log** in the app and send me that text too.

## Before you start

1. **Record your phone details** (Settings → About device):
   - Model, ColorOS version, Android version, build number, security patch date.
   - Your home-screen app: Settings → Apps → Default apps → Home screen app. Note its name.
   - Settings → Display → Display size: note the setting.
2. **Install the test app** (`android-debug.apk`, about 15 MB):
   - Copy it to the phone and open it from Files.
   - If asked, allow **Install unknown apps** for Files (or whichever app you used).
   - ColorOS or Google Play Protect may warn that the app is from an unknown developer. That's expected for a personal test build: choose *Install anyway*.
   - Installing over USB with `adb install android-debug.apk` also works. Google's new developer-verification rules don't apply to ADB installs.
3. Open **Brain Rot P0** once. Five sample habits appear: Water, Skincare (morning), Skincare (night), Protein, Focused work.

## Tests

**T1. Logging and separate metrics (in the app)**
- Tap **+0.5** on Water 3 times and tick Skincare (morning).
- Expected: Water shows 1.5 / 3 L. "Completion" and "Daily score" are shown separately. Timeline entries say `app`.
- Tap **−** on Water 4 times. Expected: it stops at 0 with "Not saved: total would go below zero".

**T2. Survives being killed and rebooted**
- Swipe the app away from Recents, then Settings → Apps → Brain Rot P0 → **Force stop**, then reopen. Expected: all entries are still there.
- Reboot the phone and reopen. Expected: still there.
- Tap **Retry-same-tap test**. Expected message: `first=Saved, retry=Duplicate`.

**T3. Backdating**
- Tap **+1 L water, yesterday 21:00**. Expected: the timeline shows it at yesterday 21:00, marked "backdated". Today's water total doesn't change.

**T4. Add the widget**
- Long-press an empty spot on the home screen → Widgets → **Brain Rot graph (Phase 0)** → place it at 4×2 if you can.
- Expected: a dark card with "Today x/y done · score n", a **Log** button, and the green contribution graph.
- Resize it (long-press → resize) to about 2×2, 4×3 and 5×2. **Screenshot each size.** Does the graph redraw to fit each size?

**T5. A normal tap on the graph does nothing** (mode: *Log button toggles quick entry*, the default)
- Tap the graph 10 times, slowly. Expected: nothing happens and the app doesn't open. Note any flash or ripple.

**T6. Log button → quick entry on the widget**
- Tap **Log**. Note roughly how long until quick entry appears: instant, under half a second, about 1 s, or slower.
- Tap **+** on Water 3 times. Does the amount update each time? Note any delay.
- Tap **Done**. The graph returns.
- Tap **Log** again and wait 2 minutes without touching it. Expected: it switches back to the graph by itself.
- Open the app. Widget entries show source `widget`.

**T7. Checkbox variant**
- In the app, under *What quick entry shows*, choose **Checkboxes only**. Go to the widget → **Log**.
- Tick Water. Expected: it shows as done, and the app still shows the real amount you logged (no invented amount).
- Untick it. **Which variant do you prefer?**

**T8. Double-tap probe (the key test)**
- In the app, choose **Double-tap graph (experimental)** and window **450 ms**. Then tap **Clear** under the tap log.
- On the home screen:
  1. **20 single taps** on the graph, about 2 seconds apart. Ideal: the widget never switches.
  2. **20 double-taps.** After each one that works, tap **Done**. Count how many switched.
  3. Lock the phone for 10 minutes, unlock, and immediately do **10 double-taps**. Count them. This is the "app was asleep" case.
  4. Optional: repeat step 2 with **350 ms** and **600 ms**.
- In the app, tap **Share tap log** and send me the text.
- Suggested pass bar (your call): ≥19/20 in step 2, 0/20 false switches in step 1, ≥9/10 in step 3, and the delay feels fine to you.

**T9. Quick-entry screen fallback**
- Choose **Log button opens quick-entry screen**. Tap **Log** on the widget.
- Expected: a small panel opens over the home screen. Log something and tap **Done**. The widget updates.
- Compare how it feels with T6.

**T10. Export (still offline)**
- Tap **Export JSON** and **Export CSV** and save both to Downloads.
- Open the CSV (e.g. in Google Sheets later). Rows should match the timeline. Deleted entries are left out of the CSV but kept in the JSON.

**T11. Overnight / ColorOS background behaviour**
- Leave the phone overnight, unplugged, with the widget on the home screen.
- In the morning: is the graph still drawn? Does **Log** respond? How fast?
- If something failed: Settings → Apps → Brain Rot P0 → allow **Auto launch** and **background activity**, set battery to **Unrestricted**, then repeat the next night.

**T12. Force-stop and the widget**
- Force stop the app (as in T2) and look at the widget. Does it grey out or stop responding? Open the app once: does the widget recover?

## Results

| Test | Result (pass / fail / notes) |
|---|---|
| Device details | Model: · ColorOS: · Android: · Build: · Home app: · Display size: |
| T1 logging / separate metrics | |
| T2 kill, reboot, duplicate check | |
| T3 backdating | |
| T4 widget renders; sizes (screenshots) | |
| T5 graph tap does nothing | |
| T6 Log toggle: delay, + updates, Done, auto-return | |
| T7 checkbox variant; preferred variant | |
| T8 double-tap: singles x/20, doubles x/20, asleep x/10 | |
| T9 quick-entry screen | |
| T10 export | |
| T11 overnight | |
| T12 force-stop | |
