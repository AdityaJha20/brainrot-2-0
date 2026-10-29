package app.brainrot.core.export

import app.brainrot.core.scoring.DayMark
import app.brainrot.core.scoring.Habit
import app.brainrot.core.scoring.LogEvent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val EXPORT_FORMAT = "brainrot-export"
const val EXPORT_SCHEMA_VERSION = 1

/**
 * Full-fidelity backup. Uses the same record shapes as the shared scoring fixtures, so any export can be
 * re-imported on Android or the web and re-scored. Deleted events are kept as tombstones (deleted = true)
 * so a restore cannot resurrect something the user deleted.
 */
@Serializable
data class ExportBundle(
    val format: String = EXPORT_FORMAT,
    val schemaVersion: Int = EXPORT_SCHEMA_VERSION,
    val exportedAt: String,
    val appVersion: String,
    val scoringVersion: String,
    val habits: List<Habit>,
    val events: List<LogEvent>,
    val dayMarks: List<DayMark> = emptyList(),
    val reviewedDates: List<String> = emptyList(),
    /** Extra per-event context that scoring does not need (notes, source). Keyed by event id. */
    val eventDetails: Map<String, EventDetail> = emptyMap(),
)

@Serializable
data class EventDetail(
    val note: String? = null,
    val source: String? = null,
)

object JsonExport {
    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
            ignoreUnknownKeys = false
        }

    fun write(bundle: ExportBundle): String = json.encodeToString(ExportBundle.serializer(), bundle)

    fun read(text: String): ExportBundle {
        val bundle = json.decodeFromString(ExportBundle.serializer(), text)
        require(bundle.format == EXPORT_FORMAT) { "Not a Brain Rot export (format=${bundle.format})" }
        require(bundle.schemaVersion <= EXPORT_SCHEMA_VERSION) {
            "Export schema ${bundle.schemaVersion} is newer than this app understands ($EXPORT_SCHEMA_VERSION)"
        }
        return bundle
    }
}

/**
 * CSV of log events for spreadsheets (RFC 4180 quoting). Lossy by design: JSON is the backup format.
 * Text cells that a spreadsheet would treat as a formula (=, +, -, @, tab, CR) are prefixed with an
 * apostrophe to block CSV formula injection; numeric columns are written as plain numbers.
 */
object CsvExport {
    val HEADER =
        listOf(
            "event_id",
            "habit_id",
            "habit_name",
            "type",
            "value",
            "unit",
            "event_time_utc",
            "zone",
            "created_at_utc",
            "deleted",
            "source",
            "note",
        )

    fun write(
        bundle: ExportBundle,
        includeDeleted: Boolean = false,
    ): String {
        val habits = bundle.habits.associateBy { it.id }
        val sb = StringBuilder()
        sb.append(HEADER.joinToString(",")).append("\r\n")
        bundle.events
            .filter { includeDeleted || !it.deleted }
            .sortedBy { it.eventTime }
            .forEach { e ->
                val h = habits[e.habitId]
                val d = bundle.eventDetails[e.id]
                val row =
                    listOf(
                        text(e.id),
                        text(e.habitId),
                        text(h?.name),
                        text(e.type.name),
                        number(e.value),
                        text(h?.unit),
                        text(e.eventTime),
                        text(e.zone),
                        text(e.createdAt),
                        e.deleted.toString(),
                        text(d?.source),
                        text(d?.note),
                    )
                sb.append(row.joinToString(",")).append("\r\n")
            }
        return sb.toString()
    }

    private fun number(v: Double): String =
        if (v == Math.floor(v) &&
            !v.isInfinite()
        ) {
            v.toLong().toString()
        } else {
            v.toString()
        }

    private fun text(v: String?): String {
        if (v == null) return ""
        val safe = if (v.isNotEmpty() && v[0] in "=+-@\t\r") "'$v" else v
        val needsQuotes = safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuotes) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }

    /** Minimal RFC 4180 parser, used by tests to prove the output round-trips through a CSV reader. */
    fun parse(csv: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var i = 0
        var quoted = false
        while (i < csv.length) {
            val c = csv[i]
            if (quoted) {
                if (c == '"' && i + 1 < csv.length && csv[i + 1] == '"') {
                    cell.append('"')
                    i++
                } else if (c == '"') {
                    quoted = false
                } else {
                    cell.append(c)
                }
            } else {
                when (c) {
                    '"' -> quoted = true
                    ',' -> {
                        row.add(cell.toString())
                        cell.clear()
                    }
                    '\r' -> Unit
                    '\n' -> {
                        row.add(cell.toString())
                        cell.clear()
                        rows.add(row)
                        row = mutableListOf()
                    }
                    else -> cell.append(c)
                }
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row.add(cell.toString())
            rows.add(row)
        }
        return rows
    }
}
