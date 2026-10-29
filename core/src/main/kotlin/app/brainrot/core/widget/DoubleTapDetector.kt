package app.brainrot.core.widget

/**
 * Android widgets cannot detect a double-tap gesture: each tap arrives separately as a click
 * (a PendingIntent / Glance ActionCallback). The only way to "fake" one is to remember when the
 * previous tap arrived and compare. This class is that rule, kept free of Android so it can be
 * unit-tested. The widget callback stores [State] in SharedPreferences because each tap may be
 * delivered to a freshly started app process.
 *
 * Timestamps must come from a monotonic clock that survives process death
 * (SystemClock.elapsedRealtime() on Android), never from the wall clock.
 */
class DoubleTapDetector(
    val windowMs: Long,
) {
    init {
        require(windowMs in 100..2000) { "window must be 100..2000 ms" }
    }

    data class State(
        val lastTapAtMs: Long?,
    )

    enum class Outcome {
        /** First tap of a possible pair. Must cause no visible change and no navigation. */
        FIRST_TAP,

        /** Second tap arrived within the window: switch the widget mode. */
        DOUBLE_TAP,
    }

    data class Result(
        val outcome: Outcome,
        val sincePreviousMs: Long?,
        val newState: State,
    )

    fun onTap(
        nowMs: Long,
        state: State,
    ): Result {
        val previous = state.lastTapAtMs
        val delta = previous?.let { nowMs - it }
        return if (delta != null && delta in 0..windowMs) {
            // Consume both taps so a third quick tap starts a new pair instead of toggling back.
            Result(Outcome.DOUBLE_TAP, delta, State(lastTapAtMs = null))
        } else {
            Result(Outcome.FIRST_TAP, delta, State(lastTapAtMs = nowMs))
        }
    }
}
