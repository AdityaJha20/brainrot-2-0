package app.brainrot.spike

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasContentDescription
import androidx.glance.testing.unit.hasText
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.brainrot.spike.data.HabitEntity
import app.brainrot.spike.widget.BrainRotWidget
import app.brainrot.spike.widget.InteractionMode
import app.brainrot.spike.widget.QuickVariant
import app.brainrot.spike.widget.WidgetBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Checks what the widget composes in each mode. Glance unit tests cannot simulate a launcher, taps or timing,
 * so they prove the layout/state logic only; interaction must be tested on the phone.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetLayoutTest {
    private val habits =
        listOf(
            HabitEntity("water", "Water", "QUANTITY", 3.0, "L", 0.5, 1.0, "DAILY", "", "2026-10-01", null, "ACTIVE", 0, 0, 0),
            HabitEntity("skin_am", "Skincare (morning)", "BINARY", 1.0, null, 1.0, 1.0, "DAILY", "", "2026-10-01", null, "ACTIVE", 1, 0, 0),
        )
    private val size = DpSize(320.dp, 160.dp)

    private fun state(
        mode: String,
        interaction: InteractionMode,
        variant: QuickVariant = QuickVariant.STEPPERS,
    ) = BrainRotWidget.buildState(
        habits,
        emptyList(),
        mode,
        interaction,
        variant,
        size,
        density = 2.6f,
        today = LocalDate.parse("2026-10-28"),
    )

    @Test
    fun graphModeShowsGraphSummaryAndLogButton() =
        runGlanceAppWidgetUnitTest {
            setAppWidgetSize(size)
            val s = state("GRAPH", InteractionMode.TOGGLE_BUTTON)
            assertNotNull(s.graph)
            provideComposable { WidgetBody(s) }
            onNode(hasContentDescription("Habit contribution graph")).assertExists()
            onNode(hasText("Today 0/2 done · score 0")).assertExists()
            onNode(hasText("Log")).assertExists()
        }

    @Test
    fun doubleTapProbeHidesTheLogButton() =
        runGlanceAppWidgetUnitTest {
            setAppWidgetSize(size)
            provideComposable { WidgetBody(state("GRAPH", InteractionMode.DOUBLE_TAP_PROBE)) }
            onNode(hasText("Log")).assertDoesNotExist()
            onNode(hasContentDescription("Habit contribution graph")).assertExists()
        }

    @Test
    fun quickModeStepperVariantShowsAmountsAndDone() =
        runGlanceAppWidgetUnitTest {
            setAppWidgetSize(size)
            val s = state("QUICK", InteractionMode.TOGGLE_BUTTON, QuickVariant.STEPPERS)
            assertNull("no bitmap is built in quick mode", s.graph)
            provideComposable { WidgetBody(s) }
            onNode(hasText("Done")).assertExists()
            onNode(hasText("0/3 L")).assertExists()
            onNode(hasText("+")).assertExists()
        }

    @Test
    fun quickModeCheckboxVariantTurnsAmountsIntoCheckboxes() {
        val s = state("QUICK", InteractionMode.TOGGLE_BUTTON, QuickVariant.CHECKBOXES)
        assertEquals(2, s.rows.size)
        runGlanceAppWidgetUnitTest {
            setAppWidgetSize(size)
            provideComposable { WidgetBody(s) }
            onNode(hasText("Water  0/3 L")).assertExists()
            onNode(hasText("+")).assertDoesNotExist()
        }
    }
}
