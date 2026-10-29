package app.brainrot.spike.data

import app.brainrot.core.export.EventDetail
import app.brainrot.core.export.ExportBundle
import app.brainrot.core.scoring.EventType
import app.brainrot.core.scoring.Habit
import app.brainrot.core.scoring.LogEvent
import app.brainrot.core.scoring.Measurement
import app.brainrot.core.scoring.SCORING_VERSION
import app.brainrot.core.scoring.Schedule
import app.brainrot.core.scoring.ScoringEngine
import app.brainrot.core.scoring.ScoringInput
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

sealed interface LogResult {
    data class Saved(
        val eventId: String,
    ) : LogResult

    /** The same clientMutationId was already stored (e.g. a retried tap). Nothing was added. */
    data object Duplicate : LogResult

    data class Rejected(
        val reason: String,
    ) : LogResult
}

/**
 * The only way the UI and the widget write data. A call returns Saved only after SQLite has committed both
 * the event and its sync-queue entry, so "saved" on screen always means saved on disk, offline or not.
 */
class LogRepository(
    private val db: AppDatabase,
    private val now: () -> Instant = Instant::now,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    private val dao = db.dao()

    suspend fun seedSampleHabitsIfEmpty() {
        if (dao.allHabits().isNotEmpty()) return
        val t = now().toEpochMilli()
        val start = LocalDate.now(zone()).minusDays(60).toString()

        fun h(
            id: String,
            name: String,
            m: String,
            target: Double,
            unit: String?,
            step: Double,
            order: Int,
        ) = HabitEntity(id, name, m, target, unit, step, 1.0, "DAILY", "", start, null, "ACTIVE", order, t, t)
        dao.insertHabits(
            listOf(
                h("water", "Water", "QUANTITY", 3.0, "L", 0.5, 0),
                h("skin_am", "Skincare (morning)", "BINARY", 1.0, null, 1.0, 1),
                h("skin_pm", "Skincare (night)", "BINARY", 1.0, null, 1.0, 2),
                h("protein", "Protein", "QUANTITY", 100.0, "g", 10.0, 3),
                h("focus", "Focused work", "DURATION", 60.0, "min", 15.0, 4),
            ),
        )
    }

    suspend fun log(
        habitId: String,
        type: EventType,
        value: Double,
        source: String,
        eventTime: Instant = now(),
        note: String? = null,
        mutationId: String = UUID.randomUUID().toString(),
    ): LogResult {
        if (!value.isFinite()) return LogResult.Rejected("value must be a number")
        if (type != EventType.COMPLETION && value < 0) {
            // A decrement may not push the day's total below zero ("prevent negative totals").
            val total = dayTotal(habitId, eventTime)
            if (total + value < -1e-9) return LogResult.Rejected("total would go below zero")
        }
        val t = now().toEpochMilli()
        val event =
            LogEventEntity(
                id = UUID.randomUUID().toString(),
                habitId = habitId,
                type = type.name,
                value = value,
                eventTimeUtcMs = eventTime.toEpochMilli(),
                zone = zone().id,
                createdAtUtcMs = t,
                updatedAtUtcMs = t,
                source = source,
                note = note,
                clientMutationId = mutationId,
                deleted = false,
                deletedAtUtcMs = null,
            )
        val op =
            SyncOpEntity(
                mutationId = mutationId,
                entityType = "log_event",
                entityId = event.id,
                op = "UPSERT",
                payloadJson = payload(event),
                state = "PENDING",
                attempts = 0,
                lastError = null,
                createdAtUtcMs = t,
            )
        return if (dao.recordEvent(event, op)) LogResult.Saved(event.id) else LogResult.Duplicate
    }

    suspend fun delete(eventId: String): Boolean {
        val t = now().toEpochMilli()
        val mutationId = UUID.randomUUID().toString()
        val op =
            SyncOpEntity(
                mutationId = mutationId,
                entityType = "log_event",
                entityId = eventId,
                op = "DELETE",
                payloadJson = """{"id":"$eventId","deletedAtUtcMs":$t}""",
                state = "PENDING",
                attempts = 0,
                lastError = null,
                createdAtUtcMs = t,
            )
        return dao.tombstone(eventId, t, op)
    }

    /** Toggle a binary habit for today: add a completion, or tombstone today's completions. */
    suspend fun setCompletedToday(
        habitId: String,
        completed: Boolean,
        source: String,
    ): LogResult {
        val today = todayEvents(habitId).filter { it.type == EventType.COMPLETION.name }
        return if (completed) {
            if (today.isNotEmpty()) LogResult.Duplicate else log(habitId, EventType.COMPLETION, 1.0, source)
        } else {
            today.forEach { delete(it.id) }
            LogResult.Saved(today.firstOrNull()?.id ?: "none")
        }
    }

    private suspend fun todayEvents(habitId: String): List<LogEventEntity> {
        val z = zone()
        val start =
            LocalDate
                .now(z)
                .atStartOfDay(z)
                .toInstant()
                .toEpochMilli()
        return dao.liveEventsBetween(habitId, start, start + 86_400_000L - 1)
    }

    private suspend fun dayTotal(
        habitId: String,
        at: Instant,
    ): Double {
        val z = zone()
        val start =
            at
                .atZone(z)
                .toLocalDate()
                .atStartOfDay(z)
                .toInstant()
                .toEpochMilli()
        return dao
            .liveEventsBetween(habitId, start, start + 86_400_000L - 1)
            .filter { it.type != EventType.COMPLETION.name }
            .sumOf { it.value }
    }

    suspend fun scoringInput(): ScoringInput = toScoringInput(dao.allHabits(), dao.allEvents())

    suspend fun exportBundle(appVersion: String): ExportBundle {
        val habits = dao.allHabits()
        val events = dao.allEvents()
        val input = toScoringInput(habits, events)
        return ExportBundle(
            exportedAt = now().toString(),
            appVersion = appVersion,
            scoringVersion = SCORING_VERSION,
            habits = input.habits,
            events = input.events,
            eventDetails = events.associate { it.id to EventDetail(note = it.note, source = it.source) },
        )
    }

    companion object {
        fun toScoringInput(
            habits: List<HabitEntity>,
            events: List<LogEventEntity>,
        ) = ScoringInput(
            habits = habits.map(::toHabit),
            events = events.map(::toEvent),
        )

        fun engine(
            habits: List<HabitEntity>,
            events: List<LogEventEntity>,
        ) = ScoringEngine(toScoringInput(habits, events))

        fun toHabit(h: HabitEntity) =
            Habit(
                id = h.id,
                name = h.name,
                measurement = Measurement.valueOf(h.measurement),
                target = h.target,
                unit = h.unit,
                importance = h.importance,
                schedule =
                    Schedule(
                        h.scheduleType,
                        h.scheduleDays
                            .split(',')
                            .filter {
                                it.isNotBlank()
                            }.map { it.trim().toInt() },
                    ),
                startDate = h.startDate,
                endDate = h.endDate,
            )

        fun toEvent(e: LogEventEntity) =
            LogEvent(
                id = e.id,
                habitId = e.habitId,
                type = EventType.valueOf(e.type),
                value = e.value,
                eventTime = Instant.ofEpochMilli(e.eventTimeUtcMs).toString(),
                zone = e.zone,
                createdAt = Instant.ofEpochMilli(e.createdAtUtcMs).toString(),
                deleted = e.deleted,
            )

        private fun payload(e: LogEventEntity): String =
            Json.encodeToString(
                kotlinx.serialization.json.JsonObject
                    .serializer(),
                buildJsonObject {
                    put("id", e.id)
                    put("habitId", e.habitId)
                    put("type", e.type)
                    put("value", e.value)
                    put("eventTimeUtcMs", e.eventTimeUtcMs)
                    put("zone", e.zone)
                    put("createdAtUtcMs", e.createdAtUtcMs)
                    put("source", e.source)
                    put("note", e.note)
                },
            )
    }
}
