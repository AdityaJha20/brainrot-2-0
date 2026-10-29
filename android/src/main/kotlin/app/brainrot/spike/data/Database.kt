package app.brainrot.spike.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

// Phase 0 local data model. The phone's SQLite database is the source of truth; the cloud (later) only
// receives copies via the sync queue. Events are append-only: corrections are new events or tombstones,
// never overwritten daily totals, so totals can always be recalculated.

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val measurement: String, // BINARY | QUANTITY | DURATION
    val target: Double,
    val unit: String?,
    val step: Double, // +/- increment for quantity controls
    val importance: Double,
    val scheduleType: String, // DAILY | DAYS_OF_WEEK
    val scheduleDays: String, // comma-separated ISO weekdays, empty for DAILY
    val startDate: String,
    val endDate: String?,
    val status: String, // ACTIVE | PAUSED | COMPLETED | ARCHIVED
    val sortOrder: Int,
    val createdAtUtcMs: Long,
    val updatedAtUtcMs: Long,
)

@Entity(
    tableName = "log_events",
    indices = [Index("habitId"), Index("eventTimeUtcMs"), Index(value = ["clientMutationId"], unique = true)],
)
data class LogEventEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val type: String, // QUANTITY | DURATION | COMPLETION
    val value: Double,
    val eventTimeUtcMs: Long, // when it happened (editable, can be backdated)
    val zone: String, // IANA zone at the time it happened
    val createdAtUtcMs: Long, // when it was typed
    val updatedAtUtcMs: Long,
    val source: String, // app | widget | quick_screen | import | sync
    val note: String?,
    val clientMutationId: String, // idempotency key: replaying the same mutation never duplicates
    val deleted: Boolean,
    val deletedAtUtcMs: Long?,
)

@Entity(tableName = "sync_queue", indices = [Index(value = ["mutationId"], unique = true)])
data class SyncOpEntity(
    @PrimaryKey(autoGenerate = true) val seq: Long = 0,
    val mutationId: String,
    val entityType: String,
    val entityId: String,
    val op: String, // UPSERT | DELETE
    val payloadJson: String,
    val state: String, // PENDING | SYNCED | FAILED | CONFLICT
    val attempts: Int,
    val lastError: String?,
    val createdAtUtcMs: Long,
)

/** Evidence log for the widget tap/double-tap probe. Exported from the app and pasted into the device report. */
@Entity(tableName = "widget_tap_log")
data class TapLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wallTimeUtcMs: Long,
    val elapsedRealtimeMs: Long,
    val sincePreviousMs: Long?,
    val outcome: String,
    val interactionMode: String,
    val windowMs: Long,
    val processAgeMs: Long, // small value = the tap had to cold-start the app process
    val note: String?,
)

@Dao
abstract class BrainRotDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertHabits(habits: List<HabitEntity>)

    @Query("SELECT * FROM habits WHERE status = 'ACTIVE' ORDER BY sortOrder")
    abstract fun activeHabitsFlow(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY sortOrder")
    abstract suspend fun allHabits(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertEventOrIgnore(event: LogEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertSyncOpOrIgnore(op: SyncOpEntity): Long

    /** Event + its sync operation are committed together or not at all. Returns false for a replayed mutation. */
    @Transaction
    open suspend fun recordEvent(
        event: LogEventEntity,
        op: SyncOpEntity,
    ): Boolean {
        if (insertEventOrIgnore(event) == -1L) return false
        insertSyncOpOrIgnore(op)
        return true
    }

    @Query(
        "UPDATE log_events SET deleted = 1, deletedAtUtcMs = :atMs, updatedAtUtcMs = :atMs " +
            "WHERE id = :eventId AND deleted = 0",
    )
    protected abstract suspend fun markDeleted(
        eventId: String,
        atMs: Long,
    ): Int

    /** Soft delete (tombstone) so the deletion itself can sync to other devices. */
    @Transaction
    open suspend fun tombstone(
        eventId: String,
        atMs: Long,
        op: SyncOpEntity,
    ): Boolean {
        if (markDeleted(eventId, atMs) == 0) return false
        insertSyncOpOrIgnore(op)
        return true
    }

    @Query("SELECT * FROM log_events WHERE deleted = 0 ORDER BY eventTimeUtcMs DESC LIMIT :limit")
    abstract fun timelineFlow(limit: Int): Flow<List<LogEventEntity>>

    /** All events including tombstones (scoring and export decide what to do with deleted ones). */
    @Query("SELECT * FROM log_events ORDER BY eventTimeUtcMs")
    abstract fun allEventsFlow(): Flow<List<LogEventEntity>>

    @Query("SELECT * FROM log_events ORDER BY eventTimeUtcMs")
    abstract suspend fun allEvents(): List<LogEventEntity>

    @Query(
        "SELECT * FROM log_events WHERE habitId = :habitId AND deleted = 0 AND eventTimeUtcMs BETWEEN :fromMs AND :toMs",
    )
    abstract suspend fun liveEventsBetween(
        habitId: String,
        fromMs: Long,
        toMs: Long,
    ): List<LogEventEntity>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE state = 'PENDING'")
    abstract fun pendingSyncCountFlow(): Flow<Int>

    @Query("SELECT * FROM sync_queue ORDER BY seq")
    abstract suspend fun allSyncOps(): List<SyncOpEntity>

    @Insert
    abstract suspend fun insertTap(tap: TapLogEntity)

    @Query("SELECT * FROM widget_tap_log ORDER BY id DESC LIMIT 200")
    abstract fun tapLogFlow(): Flow<List<TapLogEntity>>

    @Query("SELECT * FROM widget_tap_log ORDER BY id")
    abstract suspend fun allTaps(): List<TapLogEntity>

    @Query("DELETE FROM widget_tap_log")
    abstract suspend fun clearTaps()
}

@Database(
    entities = [HabitEntity::class, LogEventEntity::class, SyncOpEntity::class, TapLogEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): BrainRotDao

    companion object {
        const val FILE_NAME = "brainrot-phase0.db"

        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext, FILE_NAME).also { instance = it }
            }

        fun build(
            context: Context,
            fileName: String,
        ): AppDatabase =
            Room
                .databaseBuilder(context, AppDatabase::class.java, fileName)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .addCallback(
                    object : Callback() {
                        override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            // WAL's default (NORMAL) already survives an app crash or process kill; FULL also
                            // survives sudden power loss, at a tiny cost per write. Logs are small and rare.
                            db.query("PRAGMA synchronous = FULL").close()
                        }
                    },
                ).build()
    }
}
