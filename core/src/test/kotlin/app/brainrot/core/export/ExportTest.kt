package app.brainrot.core.export

import app.brainrot.core.scoring.DayMark
import app.brainrot.core.scoring.DayMarkType
import app.brainrot.core.scoring.EventType
import app.brainrot.core.scoring.Habit
import app.brainrot.core.scoring.LogEvent
import app.brainrot.core.scoring.Measurement
import app.brainrot.core.scoring.SCORING_VERSION
import app.brainrot.core.scoring.Schedule
import app.brainrot.core.scoring.ScoringEngine
import app.brainrot.core.scoring.ScoringInput
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ExportTest {
    private val habits =
        listOf(
            Habit("water", "Drink water", Measurement.QUANTITY, 3.0, "L", 2.0, Schedule("DAILY"), "2026-10-01"),
            Habit(
                "skin",
                "Skincare, night",
                Measurement.BINARY,
                1.0,
                null,
                1.0,
                Schedule("DAYS_OF_WEEK", listOf(1, 3, 5)),
                "2026-10-01",
            ),
        )
    private val events =
        listOf(
            LogEvent(
                "e1",
                "water",
                EventType.QUANTITY,
                1.5,
                "2026-10-28T03:00:00Z",
                "Asia/Kolkata",
                "2026-10-28T03:00:02Z",
            ),
            LogEvent(
                "e2",
                "water",
                EventType.QUANTITY,
                1.0,
                "2026-10-28T06:00:00Z",
                "Asia/Kolkata",
                "2026-10-28T06:00:01Z",
                deleted = true,
            ),
            LogEvent(
                "e3",
                "skin",
                EventType.COMPLETION,
                1.0,
                "2026-10-27T16:30:00Z",
                "Asia/Kolkata",
                "2026-10-28T08:00:00Z",
            ),
        )
    private val bundle =
        ExportBundle(
            exportedAt = "2026-10-29T10:00:00Z",
            appVersion = "0.0.1-phase0",
            scoringVersion = SCORING_VERSION,
            habits = habits,
            events = events,
            dayMarks = listOf(DayMark("water", "2026-10-25", DayMarkType.REST)),
            reviewedDates = listOf("2026-10-27"),
            eventDetails =
                mapOf(
                    "e1" to EventDetail(note = "after gym, \"big\" bottle,\nfelt good", source = "widget"),
                    "e3" to EventDetail(note = "=HYPERLINK(\"http://evil\")", source = "app"),
                ),
        )

    @Test
    fun jsonRoundTripIsLossless() {
        val text = JsonExport.write(bundle)
        assertEquals(bundle, JsonExport.read(text))
    }

    @Test
    fun jsonRoundTripGivesIdenticalScores() {
        val restored = JsonExport.read(JsonExport.write(bundle))

        fun score(b: ExportBundle) =
            ScoringEngine(ScoringInput(b.habits, b.events, b.dayMarks, b.reviewedDates))
                .evaluate(LocalDate.parse("2026-10-28"))
        assertEquals(score(bundle), score(restored))
    }

    @Test
    fun deletedEventsSurviveAsTombstonesInJson() {
        val restored = JsonExport.read(JsonExport.write(bundle))
        assertTrue(restored.events.single { it.id == "e2" }.deleted)
    }

    @Test
    fun rejectsForeignOrNewerFiles() {
        assertFailsWith<IllegalArgumentException> { JsonExport.read(JsonExport.write(bundle.copy(format = "other"))) }
        assertFailsWith<IllegalArgumentException> { JsonExport.read(JsonExport.write(bundle.copy(schemaVersion = 99))) }
    }

    @Test
    fun csvQuotesAndRoundTripsThroughAParser() {
        val rows = CsvExport.parse(CsvExport.write(bundle))
        assertEquals(CsvExport.HEADER, rows[0])
        assertEquals(3, rows.size, "header + 2 live events (deleted e2 excluded by default)")
        val e1 = rows.first { it[0] == "e1" }
        assertEquals("Drink water", e1[2])
        assertEquals("1.5", e1[4])
        assertEquals("after gym, \"big\" bottle,\nfelt good", e1[11])
    }

    @Test
    fun csvNeutralisesFormulaInjection() {
        val e3 = CsvExport.parse(CsvExport.write(bundle)).first { it[0] == "e3" }
        assertEquals("'=HYPERLINK(\"http://evil\")", e3[11])
    }

    @Test
    fun csvCanIncludeTombstonesWhenAsked() {
        assertEquals(4, CsvExport.parse(CsvExport.write(bundle, includeDeleted = true)).size)
    }
}
