// Brain Rot 2.0 scoring engine, Kotlin version (Phase 0 proposal "scoring-v0-proposal").
// The rules live in shared-fixtures/scoring/README.md; web/scoring/src/scoring.ts is the TypeScript twin.
// Both must pass the same JSON fixtures. Keep the two files structurally parallel.
package app.brainrot.core.scoring

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

const val SCORING_VERSION = "scoring-v0-proposal"

private const val MAX_STREAK_LOOKBACK_DAYS = 3660L

@Serializable
enum class Measurement { BINARY, QUANTITY, DURATION }

@Serializable
enum class EventType { QUANTITY, DURATION, COMPLETION }

@Serializable
enum class DayMarkType { REST, SKIPPED }

enum class Status { NOT_SCHEDULED, REST, SKIPPED, COMPLETED, PARTIAL, MISSED, UNKNOWN }

@Serializable
data class Schedule(
    val type: String, // "DAILY" | "DAYS_OF_WEEK"
    val days: List<Int> = emptyList(), // ISO weekday, 1 = Monday ... 7 = Sunday
)

@Serializable
data class Habit(
    val id: String,
    val name: String? = null,
    val measurement: Measurement,
    val target: Double,
    val unit: String? = null,
    val importance: Double,
    val schedule: Schedule,
    val startDate: String,
    val endDate: String? = null,
)

@Serializable
data class LogEvent(
    val id: String,
    val habitId: String,
    val type: EventType,
    val value: Double,
    val eventTime: String,
    val zone: String,
    val createdAt: String,
    val deleted: Boolean = false,
)

@Serializable
data class DayMark(
    val habitId: String,
    val date: String,
    val mark: DayMarkType,
)

@Serializable
data class PolicyOverrides(
    val successThreshold: Double? = null,
    val dayStartHour: Int? = null,
    val heatmapBins: List<Double>? = null,
)

data class Policy(
    val successThreshold: Double = 1.0,
    val dayStartHour: Int = 0,
    val heatmapBins: Pair<Double, Double> = 34.0 to 67.0,
) {
    fun with(o: PolicyOverrides?): Policy =
        if (o == null) {
            this
        } else {
            Policy(
                successThreshold = o.successThreshold ?: successThreshold,
                dayStartHour = o.dayStartHour ?: dayStartHour,
                heatmapBins = o.heatmapBins?.let { it[0] to it[1] } ?: heatmapBins,
            )
        }
}

@Serializable
data class ScoringInput(
    val habits: List<Habit>,
    val events: List<LogEvent>,
    val dayMarks: List<DayMark> = emptyList(),
    val reviewedDates: List<String> = emptyList(),
    val policy: PolicyOverrides? = null,
)

data class HabitDay(
    val status: Status,
    val recorded: Double,
    val progress: Double,
    val streak: Int,
    val streakPendingReview: Boolean,
)

data class DailyCompletion(
    val completed: Int,
    val eligible: Int,
    val ratio: Double?,
)

data class DaySummary(
    val date: LocalDate,
    val habits: Map<String, HabitDay>,
    val dailyCompletion: DailyCompletion,
    val dailyScore: Double?,
    val scoreIsProvisional: Boolean,
    val heatmapLevel: Int,
    val scoringVersion: String = SCORING_VERSION,
)

/** The logical day an event belongs to: local time in the event's zone, shifted back by dayStartHour. */
fun logicalDate(
    event: LogEvent,
    policy: Policy,
): LocalDate {
    val local = Instant.parse(event.eventTime).atZone(ZoneId.of(event.zone))
    return if (local.hour < policy.dayStartHour) local.toLocalDate().minusDays(1) else local.toLocalDate()
}

class ScoringEngine(
    input: ScoringInput,
    basePolicy: Policy = Policy(),
) {
    val policy: Policy = basePolicy.with(input.policy)
    private val habits = input.habits
    private val reviewed: Set<LocalDate> = input.reviewedDates.map(LocalDate::parse).toSet()
    private val marks: Map<Pair<String, LocalDate>, DayMarkType> =
        input.dayMarks.associate { (it.habitId to LocalDate.parse(it.date)) to it.mark }
    private val eventsByHabitDay: Map<Pair<String, LocalDate>, List<LogEvent>>
    private val activeDays: Set<LocalDate>

    init {
        val live = input.events.filterNot { it.deleted }
        eventsByHabitDay = live.groupBy { it.habitId to logicalDate(it, policy) }
        activeDays = eventsByHabitDay.keys.map { it.second }.toSet()
    }

    fun isScheduled(
        habit: Habit,
        date: LocalDate,
    ): Boolean {
        if (date < LocalDate.parse(habit.startDate)) return false
        if (habit.endDate != null && date > LocalDate.parse(habit.endDate)) return false
        if (habit.schedule.type == "DAILY") return true
        return date.dayOfWeek.value in habit.schedule.days
    }

    private data class Measured(
        val recorded: Double,
        val progress: Double,
        val hasEvents: Boolean,
    )

    private fun measure(
        habit: Habit,
        date: LocalDate,
    ): Measured {
        val events = eventsByHabitDay[habit.id to date].orEmpty()
        var sum = 0.0
        var completed = false
        for (e in events) {
            if (e.type == EventType.COMPLETION) completed = true else sum += e.value
        }
        val recorded = maxOf(sum, 0.0)
        val progress =
            when {
                completed -> 1.0
                habit.measurement == Measurement.BINARY -> 0.0
                habit.target > 0 -> minOf(maxOf(recorded / habit.target, 0.0), 1.0)
                else -> 0.0
            }
        return Measured(recorded, progress, events.isNotEmpty())
    }

    private fun status(
        habit: Habit,
        date: LocalDate,
        progress: Double,
        hasEvents: Boolean,
    ): Status {
        if (!isScheduled(habit, date)) return Status.NOT_SCHEDULED
        marks[habit.id to date]?.let { return Status.valueOf(it.name) }
        if (progress >= policy.successThreshold) return Status.COMPLETED
        if (date in reviewed) return Status.MISSED
        return if (hasEvents) Status.PARTIAL else Status.UNKNOWN
    }

    fun statusOn(
        habit: Habit,
        date: LocalDate,
    ): Status {
        val m = measure(habit, date)
        return status(habit, date, m.progress, m.hasEvents)
    }

    /** Returns (streak, pendingReview). */
    fun streak(
        habit: Habit,
        date: LocalDate,
    ): Pair<Int, Boolean> {
        var count = 0
        val floor = maxOf(LocalDate.parse(habit.startDate), date.minusDays(MAX_STREAK_LOOKBACK_DAYS))
        var d = date
        while (d >= floor) {
            when (statusOn(habit, d)) {
                Status.COMPLETED -> count++
                Status.NOT_SCHEDULED, Status.REST, Status.SKIPPED -> Unit
                Status.MISSED -> return count to false
                Status.UNKNOWN, Status.PARTIAL ->
                    // Today, still unreviewed, is in progress: it neither counts nor breaks the streak.
                    if (!(d == date && d !in reviewed)) return count to true
            }
            d = d.minusDays(1)
        }
        return count to false
    }

    fun evaluate(date: LocalDate): DaySummary {
        val result = LinkedHashMap<String, HabitDay>()
        var completed = 0
        var eligible = 0
        var weightSum = 0.0
        var weighted = 0.0
        for (habit in habits) {
            val m = measure(habit, date)
            val status = status(habit, date, m.progress, m.hasEvents)
            val (streak, pending) = streak(habit, date)
            result[habit.id] = HabitDay(status, m.recorded, m.progress, streak, pending)
            if (status == Status.NOT_SCHEDULED || status == Status.REST || status == Status.SKIPPED) continue
            eligible++
            if (status == Status.COMPLETED) completed++
            weightSum += habit.importance
            weighted += habit.importance * m.progress
        }
        val score = if (eligible == 0 || weightSum <= 0.0) null else 100 * weighted / weightSum
        return DaySummary(
            date = date,
            habits = result,
            dailyCompletion =
                DailyCompletion(
                    completed,
                    eligible,
                    if (eligible == 0) null else completed.toDouble() / eligible,
                ),
            dailyScore = score,
            scoreIsProvisional = date !in reviewed,
            heatmapLevel = heatmapLevel(date, eligible, completed, score),
        )
    }

    private fun heatmapLevel(
        date: LocalDate,
        eligible: Int,
        completed: Int,
        score: Double?,
    ): Int {
        if (eligible == 0 || score == null || date !in activeDays) return 0
        if (completed == eligible) return 4
        val (low, high) = policy.heatmapBins
        return when {
            score < low -> 1
            score < high -> 2
            else -> 3
        }
    }
}
