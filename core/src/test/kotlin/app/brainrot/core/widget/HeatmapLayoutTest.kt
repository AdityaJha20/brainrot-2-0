package app.brainrot.core.widget

import app.brainrot.core.scoring.EventType
import app.brainrot.core.scoring.Habit
import app.brainrot.core.scoring.LogEvent
import app.brainrot.core.scoring.Measurement
import app.brainrot.core.scoring.Schedule
import app.brainrot.core.scoring.ScoringEngine
import app.brainrot.core.scoring.ScoringInput
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HeatmapLayoutTest {
    @Test
    fun fitsWholeWeeksInsideTheArea() {
        // Roughly a 4x2 widget on a 1080 px wide, ~2.6 density screen (content area ~ 930 x 300 px).
        val g = HeatmapLayout.fit(availableWidthPx = 930, availableHeightPx = 300)
        assertTrue(g.widthPx <= 930 && g.heightPx <= 300, "geometry $g overflows")
        assertEquals(7, g.rows)
        assertTrue(g.columns >= 20, "expected at least 20 weeks, got ${g.columns}")
    }

    @Test
    fun tallWidgetDoesNotShowFewerWeeks() {
        val wide = HeatmapLayout.fit(930, 280)
        val tall = HeatmapLayout.fit(930, 600)
        assertTrue(tall.columns >= 20, "tall widget shows only ${tall.columns} weeks")
        assertTrue(tall.heightPx <= 600 && tall.widthPx <= 930)
        assertTrue(wide.columns >= 20)
    }

    @Test
    fun smallWidgetStillShowsSomething() {
        val g = HeatmapLayout.fit(availableWidthPx = 200, availableHeightPx = 120)
        assertTrue(g.columns >= 5, "expected >= 5 weeks, got $g")
    }

    @Test
    fun oppoF29BudgetComfortablyFitsAWidgetSizedBitmap() {
        // OPPO F29 panel: 1080 x 2412 (OPPO spec page, checked 2026-10-29).
        val budget = WidgetBitmapBudget.maxBitmapBytes(1080, 2412)
        assertEquals(15_629_760L, budget)
        val g = HeatmapLayout.fit(930, 300)
        assertTrue(g.bitmapBytes * 4 < budget, "even 4 widget instances fit: ${g.bitmapBytes} B each")
    }

    @Test
    fun levelsComeFromTheSharedScoringEngine() {
        val habit =
            Habit(
                id = "water",
                measurement = Measurement.QUANTITY,
                target = 3.0,
                importance = 1.0,
                schedule = Schedule("DAILY"),
                startDate = "2026-10-01",
            )

        fun water(
            day: String,
            v: Double,
            id: String,
        ) = LogEvent(
            id,
            "water",
            EventType.QUANTITY,
            v,
            "${day}T05:00:00Z",
            "Asia/Kolkata",
            "${day}T05:00:00Z",
        )
        val engine =
            ScoringEngine(
                ScoringInput(
                    habits = listOf(habit),
                    events = listOf(water("2026-10-26", 3.0, "a"), water("2026-10-27", 1.0, "b")),
                ),
            )
        // Wed 2026-10-28 is the last day; 2 columns = week of 19 Oct and week of 26 Oct.
        val grid = HeatmapLayout.levels(engine, LocalDate.parse("2026-10-28"), columns = 2)
        assertEquals(4, grid[1][0], "Mon 26: perfect day")
        assertEquals(1, grid[1][1], "Tue 27: 1 of 3 L")
        assertEquals(0, grid[1][2], "Wed 28: nothing logged yet")
        assertNull(grid[1][3], "Thu 29 is in the future")
        assertEquals(0, grid[0][0], "Mon 19: no activity")
    }
}
