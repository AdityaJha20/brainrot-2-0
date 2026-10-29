package app.brainrot.spike

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.brainrot.core.export.JsonExport
import app.brainrot.core.scoring.EventType
import app.brainrot.core.scoring.Status
import app.brainrot.spike.data.AppDatabase
import app.brainrot.spike.data.LogRepository
import app.brainrot.spike.data.LogResult
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Offline persistence evidence on a real on-disk SQLite file (Robolectric runs Android's SQLite natively).
 * Each "restart" closes the database and opens a brand-new instance on the same file, the way a killed and
 * relaunched app would. No network exists in this test, which is the point.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class RoomPersistenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val file = "persistence-test.db"
    private val zone = ZoneId.of("Asia/Kolkata")
    private val clock = Instant.parse("2026-10-28T10:00:00Z")
    private lateinit var db: AppDatabase

    private fun open() = AppDatabase.build(context, file).also { db = it }

    private fun repo() = LogRepository(db, now = { clock }, zone = { zone })

    @Before
    fun setUp() {
        context.deleteDatabase(file)
        open()
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(file)
    }

    private fun restart() {
        db.close()
        open()
    }

    @Test
    fun logsSurviveARestartAndScoreTheSame() =
        runTest {
            repo().seedSampleHabitsIfEmpty()
            repeat(3) { assertTrue(repo().log("water", EventType.QUANTITY, 1.0, "widget") is LogResult.Saved) }
            val before =
                LogRepository
                    .engine(
                        db.dao().allHabits(),
                        db.dao().allEvents(),
                    ).evaluate(LocalDate.parse("2026-10-28"))

            restart()

            val after =
                LogRepository
                    .engine(
                        db.dao().allHabits(),
                        db.dao().allEvents(),
                    ).evaluate(LocalDate.parse("2026-10-28"))
            assertEquals(3, db.dao().allEvents().size)
            assertEquals(3.0, after.habits.getValue("water").recorded, 1e-9)
            assertEquals(Status.COMPLETED, after.habits.getValue("water").status)
            assertEquals(before, after)
        }

    @Test
    fun everySavedLogHasADurableSyncOperation() =
        runTest {
            repo().seedSampleHabitsIfEmpty()
            repo().log("water", EventType.QUANTITY, 0.5, "app")
            repo().log("skin_am", EventType.COMPLETION, 1.0, "widget")
            restart()
            val ops = db.dao().allSyncOps()
            assertEquals(2, ops.size)
            assertTrue(ops.all { it.state == "PENDING" && it.op == "UPSERT" })
        }

    @Test
    fun replayedMutationIsNotDuplicated() =
        runTest {
            repo().seedSampleHabitsIfEmpty()
            val first = repo().log("protein", EventType.QUANTITY, 10.0, "widget", mutationId = "tap-123")
            restart() // e.g. the retry happens after the app was killed
            val retry = repo().log("protein", EventType.QUANTITY, 10.0, "widget", mutationId = "tap-123")
            assertTrue(first is LogResult.Saved)
            assertEquals(LogResult.Duplicate, retry)
            assertEquals(1, db.dao().allEvents().size)
            assertEquals(1, db.dao().allSyncOps().size)
        }

    @Test
    fun deleteIsATombstoneThatSyncsAndSurvivesRestart() =
        runTest {
            repo().seedSampleHabitsIfEmpty()
            val saved = repo().log("water", EventType.QUANTITY, 2.0, "app") as LogResult.Saved
            assertTrue(repo().delete(saved.eventId))
            restart()
            val event = db.dao().allEvents().single()
            assertTrue(event.deleted)
            assertEquals(listOf("UPSERT", "DELETE"), db.dao().allSyncOps().map { it.op })
            val day =
                LogRepository
                    .engine(
                        db.dao().allHabits(),
                        db.dao().allEvents(),
                    ).evaluate(LocalDate.parse("2026-10-28"))
            assertEquals(0.0, day.habits.getValue("water").recorded, 1e-9)
        }

    @Test
    fun decrementCannotMakeTheTotalNegative() =
        runTest {
            repo().seedSampleHabitsIfEmpty()
            repo().log("water", EventType.QUANTITY, 0.5, "widget")
            assertTrue(repo().log("water", EventType.QUANTITY, -1.0, "widget") is LogResult.Rejected)
            assertTrue(repo().log("water", EventType.QUANTITY, -0.5, "widget") is LogResult.Saved)
        }

    @Test
    fun backdatedEntryKeepsBothTimesAndLandsOnItsOwnDay() =
        runTest {
            repo().seedSampleHabitsIfEmpty()
            val yesterdayEvening = Instant.parse("2026-10-27T15:30:00Z") // 21:00 IST on 27 Oct
            repo().log("water", EventType.QUANTITY, 3.0, "app", eventTime = yesterdayEvening)
            restart()
            val e = db.dao().allEvents().single()
            assertEquals(yesterdayEvening.toEpochMilli(), e.eventTimeUtcMs)
            assertEquals(clock.toEpochMilli(), e.createdAtUtcMs)
            val engine = LogRepository.engine(db.dao().allHabits(), db.dao().allEvents())
            assertEquals(
                Status.COMPLETED,
                engine
                    .evaluate(LocalDate.parse("2026-10-27"))
                    .habits
                    .getValue("water")
                    .status,
            )
            assertEquals(
                0.0,
                engine
                    .evaluate(LocalDate.parse("2026-10-28"))
                    .habits
                    .getValue("water")
                    .recorded,
                1e-9,
            )
        }

    @Test
    fun exportFromTheDeviceDatabaseRoundTrips() =
        runTest {
            repo().seedSampleHabitsIfEmpty()
            repo().log("water", EventType.QUANTITY, 1.5, "widget", note = "with lunch, \"big\" glass")
            val deleted = repo().log("protein", EventType.QUANTITY, 20.0, "app") as LogResult.Saved
            repo().delete(deleted.eventId)
            val bundle = repo().exportBundle("test")
            assertEquals(bundle, JsonExport.read(JsonExport.write(bundle)))
            assertEquals(5, bundle.habits.size)
            assertEquals(2, bundle.events.size)
            assertTrue(bundle.events.single { it.id == deleted.eventId }.deleted)
        }
}
