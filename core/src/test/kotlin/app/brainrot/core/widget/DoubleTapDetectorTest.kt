package app.brainrot.core.widget

import app.brainrot.core.widget.DoubleTapDetector.Outcome
import app.brainrot.core.widget.DoubleTapDetector.State
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DoubleTapDetectorTest {
    private val detector = DoubleTapDetector(windowMs = 400)

    @Test
    fun singleTapIsOnlyAFirstTap() {
        val r = detector.onTap(nowMs = 10_000, state = State(null))
        assertEquals(Outcome.FIRST_TAP, r.outcome)
        assertEquals(10_000L, r.newState.lastTapAtMs)
    }

    @Test
    fun secondTapInsideWindowIsDoubleTapAndResets() {
        val first = detector.onTap(10_000, State(null))
        val second = detector.onTap(10_350, first.newState)
        assertEquals(Outcome.DOUBLE_TAP, second.outcome)
        assertEquals(350L, second.sincePreviousMs)
        assertNull(second.newState.lastTapAtMs, "pair is consumed")
    }

    @Test
    fun windowBoundaryIsInclusive() {
        assertEquals(Outcome.DOUBLE_TAP, detector.onTap(10_400, State(10_000)).outcome)
        assertEquals(Outcome.FIRST_TAP, detector.onTap(10_401, State(10_000)).outcome)
    }

    @Test
    fun slowSecondTapStartsANewPair() {
        val r = detector.onTap(11_000, State(10_000))
        assertEquals(Outcome.FIRST_TAP, r.outcome)
        assertEquals(11_000L, r.newState.lastTapAtMs)
    }

    @Test
    fun thirdQuickTapDoesNotToggleBack() {
        var s = State(null)
        val outcomes =
            listOf(0L, 200L, 400L).map { t ->
                detector.onTap(t, s).also { s = it.newState }.outcome
            }
        assertEquals(listOf(Outcome.FIRST_TAP, Outcome.DOUBLE_TAP, Outcome.FIRST_TAP), outcomes)
    }

    @Test
    fun clockGoingBackwardsIsNotADoubleTap() {
        // e.g. stored timestamp from before a reboot (elapsedRealtime restarts at 0)
        assertEquals(Outcome.FIRST_TAP, detector.onTap(500, State(90_000)).outcome)
    }

    @Test
    fun rejectsSillyWindows() {
        assertFailsWith<IllegalArgumentException> { DoubleTapDetector(50) }
    }
}
