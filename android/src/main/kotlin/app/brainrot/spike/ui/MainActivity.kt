package app.brainrot.spike.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.brainrot.core.export.CsvExport
import app.brainrot.core.export.JsonExport
import app.brainrot.core.scoring.EventType
import app.brainrot.core.widget.HeatmapLayout
import app.brainrot.spike.data.AppDatabase
import app.brainrot.spike.data.HabitEntity
import app.brainrot.spike.data.LogRepository
import app.brainrot.spike.data.LogResult
import app.brainrot.spike.data.TapLogEntity
import app.brainrot.spike.widget.HeatmapRenderer
import app.brainrot.spike.widget.InteractionMode
import app.brainrot.spike.widget.ProbeSettings
import app.brainrot.spike.widget.QuickVariant
import app.brainrot.spike.widget.refreshWidgets
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.roundToInt

internal val Background = Color(0xFF0B0D0E)
internal val Surface = Color(0xFF16191B)
internal val Accent = Color(0xFF1BAF8F)
internal val Muted = Color(0xFF9AA4AA)
internal val Warning = Color(0xFFF5B54A)

internal val BrainRotColors =
    darkColorScheme(
        background = Background,
        surface = Surface,
        primary = Accent,
        secondary = Color(0xFF8E7CC3),
        onBackground = Color(0xFFECEFF1),
        onSurface = Color(0xFFECEFF1),
    )

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = BrainRotColors) { SpikeScreen() } }
    }
}

private val timeFmt = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm")

@Composable
private fun SpikeScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val repo = remember { LogRepository(db) }
    val dao = db.dao()
    val scope = rememberCoroutineScope()
    val habits by dao.activeHabitsFlow().collectAsState(emptyList())
    val allEvents by dao.allEventsFlow().collectAsState(emptyList())
    val timeline by dao.timelineFlow(30).collectAsState(emptyList())
    val pending by dao.pendingSyncCountFlow().collectAsState(0)
    val taps by dao.tapLogFlow().collectAsState(emptyList())
    var message by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { repo.seedSampleHabitsIfEmpty() }

    val engine = remember(habits, allEvents) { LogRepository.engine(habits, allEvents) }
    val today = LocalDate.now()
    val summary = remember(engine) { engine.evaluate(today) }

    fun after(result: LogResult) {
        message =
            when (result) {
                is LogResult.Saved -> "Saved on this phone (sync queue: pending)"
                LogResult.Duplicate -> "Duplicate ignored: this exact mutation was already saved"
                is LogResult.Rejected -> "Not saved: ${result.reason}"
            }
        scope.launch { refreshWidgets(context) }
    }

    val exportJson =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            scope.launch { message = writeTo(context, uri, JsonExport.write(repo.exportBundle(APP_VERSION))) }
        }
    val exportCsv =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            scope.launch { message = writeTo(context, uri, CsvExport.write(repo.exportBundle(APP_VERSION))) }
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Background).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Spacer(Modifier.height(24.dp))
            Text("Brain Rot 2.0 · Phase 0 test build", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(
                "Not the product UI. Everything here is stored only on this phone; nothing is sent to any server.",
                color = Muted,
                fontSize = 13.sp,
            )
            if (message.isNotEmpty()) Text(message, color = Warning, fontSize = 13.sp)
        }

        item { Section("Today: kept separate on purpose") }
        item {
            val c = summary.dailyCompletion
            Text("Completion: ${c.completed} of ${c.eligible} scheduled habits", color = Color.White)
            Text(
                "Daily score: ${summary.dailyScore?.roundToInt() ?: "none (nothing scheduled)"}" +
                    if (summary.scoreIsProvisional) " · provisional until the day is reviewed" else "",
                color = Color.White,
            )
            Text(
                "Heatmap level today: ${summary.heatmapLevel} of 4 · scoring ${summary.scoringVersion}",
                color = Muted,
                fontSize = 12.sp,
            )
        }
        items(habits, key = { it.id }) { habit ->
            val day = summary.habits[habit.id]
            HabitRow(
                habit = habit,
                recorded = day?.recorded ?: 0.0,
                progress = day?.progress ?: 0.0,
                status = day?.status?.name ?: "",
                streak = day?.streak ?: 0,
                pendingReview = day?.streakPendingReview ?: false,
                onAdjust = { delta ->
                    scope.launch {
                        val type = if (habit.measurement == "DURATION") EventType.DURATION else EventType.QUANTITY
                        after(repo.log(habit.id, type, delta, source = "app"))
                    }
                },
                onCheck = { checked -> scope.launch { after(repo.setCompletedToday(habit.id, checked, "app")) } },
            )
        }

        item { Section("Contribution graph (same bitmap code as the widget)") }
        item {
            val bitmap =
                remember(engine) {
                    val g = HeatmapLayout.fit(availableWidthPx = 900, availableHeightPx = 210)
                    HeatmapRenderer.render(g, HeatmapLayout.levels(engine, today, g.columns))
                }
            Image(bitmap.asImageBitmap(), contentDescription = "Contribution graph", modifier = Modifier.fillMaxWidth())
        }

        item { Section("Timeline (newest first)") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    // Backdating: event time is yesterday 21:00, creation time is now.
                    val yesterday9pm =
                        LocalDate
                            .now()
                            .minusDays(
                                1,
                            ).atTime(21, 0)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                    scope.launch {
                        after(
                            repo.log(
                                "water",
                                EventType.QUANTITY,
                                1.0,
                                "app",
                                eventTime = yesterday9pm,
                                note = "backdated test",
                            ),
                        )
                    }
                }) { Text("+1 L water, yesterday 21:00") }
            }
        }
        items(timeline, key = { it.id }) { e ->
            val name = habits.firstOrNull { it.id == e.habitId }?.name ?: e.habitId
            val backdated = e.createdAtUtcMs - e.eventTimeUtcMs > 3_600_000L
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "$name · ${e.type.lowercase()} ${if (e.type == "COMPLETION") "" else e.value}",
                        color = Color.White,
                        fontSize = 14.sp,
                    )
                    Text(
                        Instant.ofEpochMilli(e.eventTimeUtcMs).atZone(ZoneId.of(e.zone)).format(timeFmt) +
                            " · ${e.source}" + if (backdated) " · backdated" else "",
                        color = Muted,
                        fontSize = 12.sp,
                    )
                }
                TextButton(onClick = {
                    scope.launch {
                        repo.delete(e.id)
                        refreshWidgets(context)
                    }
                }) { Text("Delete") }
            }
        }

        item { Section("Widget probe (for the OPPO F29 device test)") }
        item { ProbeControls(onChanged = { scope.launch { refreshWidgets(context) } }) }
        item { TapStats(taps) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    scope.launch { shareText(context, "Brain Rot widget tap log", tapReport(dao.allTaps())) }
                }) {
                    Text("Share tap log")
                }
                OutlinedButton(onClick = { scope.launch { dao.clearTaps() } }) { Text("Clear") }
            }
        }

        item { Section("Offline and sync self-checks") }
        item {
            Text(
                "Pending sync operations: $pending (no server is configured in Phase 0, so they stay pending)",
                color = Color.White,
                fontSize = 14.sp,
            )
            Text("Events stored (incl. deleted): ${allEvents.size}", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = {
                scope.launch {
                    val id = UUID.randomUUID().toString()
                    val first = repo.log("protein", EventType.QUANTITY, 10.0, "app", mutationId = id)
                    val retry = repo.log("protein", EventType.QUANTITY, 10.0, "app", mutationId = id)
                    message =
                        "Retry test: first=${first::class.simpleName}, retry=${retry::class.simpleName} (expected Saved, Duplicate)"
                    refreshWidgets(context)
                }
            }) { Text("Retry-same-tap test (+10 g protein once)") }
        }

        item { Section("Export (works offline, no account)") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    exportJson.launch("brainrot-backup-${LocalDate.now()}.json")
                }) { Text("Export JSON") }
                Button(onClick = { exportCsv.launch("brainrot-logs-${LocalDate.now()}.csv") }) { Text("Export CSV") }
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun Section(title: String) {
    Column {
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Surface)
        Spacer(Modifier.height(8.dp))
        Text(title, color = Accent, fontSize = 15.sp)
    }
}

@Composable
private fun HabitRow(
    habit: HabitEntity,
    recorded: Double,
    progress: Double,
    status: String,
    streak: Int,
    pendingReview: Boolean,
    onAdjust: (Double) -> Unit,
    onCheck: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(Surface).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(habit.name, color = Color.White)
                val amount =
                    if (habit.measurement ==
                        "BINARY"
                    ) {
                        ""
                    } else {
                        "${fmt(recorded)} / ${fmt(habit.target)} ${habit.unit ?: ""} · "
                    }
                Text(
                    amount + "${status.lowercase()} · streak $streak" +
                        if (pendingReview) " (earlier day unreviewed)" else "",
                    color = Muted,
                    fontSize = 12.sp,
                )
            }
            if (habit.measurement == "BINARY") {
                Checkbox(checked = status == "COMPLETED", onCheckedChange = onCheck)
            } else {
                OutlinedButton(onClick = { onAdjust(-habit.step) }) { Text("−") }
                Spacer(Modifier.width(6.dp))
                Button(onClick = { onAdjust(habit.step) }) { Text("+${fmt(habit.step)}") }
            }
        }
        if (habit.measurement != "BINARY") {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(progress = { progress.toFloat() }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ProbeControls(onChanged: () -> Unit) {
    val context = LocalContext.current
    val probe = remember { ProbeSettings(context) }
    var mode by remember { mutableStateOf(probe.interactionMode) }
    var variant by remember { mutableStateOf(probe.quickVariant) }
    var window by remember { mutableStateOf(probe.doubleTapWindowMs) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("How the widget enters quick entry:", color = Color.White, fontSize = 14.sp)
        InteractionMode.entries.forEach { m ->
            FilterChip(selected = mode == m, onClick = {
                mode = m
                probe.interactionMode = m
                onChanged()
            }, label = { Text(m.label) })
        }
        if (mode == InteractionMode.DOUBLE_TAP_PROBE) {
            Text("Double-tap window:", color = Color.White, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ProbeSettings.WINDOW_CHOICES.forEach { w ->
                    FilterChip(selected = window == w, onClick = {
                        window = w
                        probe.doubleTapWindowMs = w
                    }, label = { Text("$w ms") })
                }
            }
        }
        Text("What quick entry shows:", color = Color.White, fontSize = 14.sp)
        QuickVariant.entries.forEach { v ->
            FilterChip(selected = variant == v, onClick = {
                variant = v
                probe.quickVariant = v
                onChanged()
            }, label = { Text(v.label) })
        }
    }
}

@Composable
private fun TapStats(taps: List<TapLogEntity>) {
    val doubles = taps.filter { it.outcome == "DOUBLE_TAP" }
    val cold = taps.count { it.processAgeMs < 2_000 }
    val gaps = taps.mapNotNull { it.sincePreviousMs }.filter { it in 1..2_000 }.sorted()
    val median = gaps.getOrNull(gaps.size / 2)
    Text(
        "Taps logged: ${taps.size} · recognised double-taps: ${doubles.size} · taps that cold-started the app: $cold" +
            (median?.let { " · median gap between taps as received: $it ms" } ?: ""),
        color = Muted,
        fontSize = 12.sp,
    )
}

private fun tapReport(taps: List<TapLogEntity>): String =
    buildString {
        appendLine("wall_time_utc,elapsed_ms,since_previous_ms,outcome,mode,window_ms,process_age_ms")
        taps.forEach {
            appendLine(
                "${Instant.ofEpochMilli(
                    it.wallTimeUtcMs,
                )},${it.elapsedRealtimeMs},${it.sincePreviousMs ?: ""},${it.outcome},${it.interactionMode},${it.windowMs},${it.processAgeMs}",
            )
        }
    }

private fun shareText(
    context: Context,
    subject: String,
    text: String,
) {
    val send =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
    context.startActivity(Intent.createChooser(send, subject))
}

private fun writeTo(
    context: Context,
    uri: Uri,
    text: String,
): String =
    runCatching {
        context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } ?: error("could not open file")
        "Exported ${text.length} characters"
    }.getOrElse { "Export failed: ${it.message}" }

private fun fmt(v: Double) = if (v == Math.floor(v)) v.toLong().toString() else "%.1f".format(v)

const val APP_VERSION = "0.0.1-phase0"
