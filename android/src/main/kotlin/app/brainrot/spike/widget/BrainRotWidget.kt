package app.brainrot.spike.widget

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.brainrot.core.scoring.Status
import app.brainrot.core.widget.HeatmapLayout
import app.brainrot.spike.BrainRotApp
import app.brainrot.spike.data.AppDatabase
import app.brainrot.spike.data.HabitEntity
import app.brainrot.spike.data.LogEventEntity
import app.brainrot.spike.data.LogRepository
import app.brainrot.spike.data.TapLogEntity
import app.brainrot.spike.ui.QuickEntryActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

internal val WIDGET_MODE_KEY = stringPreferencesKey("widget_mode")
internal const val MODE_GRAPH = "GRAPH"
internal const val MODE_QUICK = "QUICK"

internal val HABIT_ID = ActionParameters.Key<String>("habitId")
internal val DELTA = ActionParameters.Key<Double>("delta")

private const val WIDGET_BG_ARGB = 0xFF0B0D0E.toInt()
private val BG = Color(WIDGET_BG_ARGB)
private val SURFACE = Color(0xFF16191B)
private val TEXT = Color(0xFFECEFF1)
private val MUTED = Color(0xFF9AA4AA)
private val ACCENT = Color(0xFF1BAF8F)

/** Everything the widget shows, computed from the local database only (works offline, no AI). */
data class WidgetUiState(
    val mode: String,
    val interaction: InteractionMode,
    val variant: QuickVariant,
    val summaryLine: String,
    val rows: List<QuickRow>,
    val graph: Bitmap?,
)

data class QuickRow(
    val habitId: String,
    val name: String,
    val isBinary: Boolean,
    val done: Boolean,
    val amountText: String,
    val step: Double,
)

class BrainRotWidget : GlanceAppWidget() {
    // Exact: compose once per actual widget size, so the bitmap is drawn at the real pixel size.
    override val sizeMode = SizeMode.Exact
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val dao = AppDatabase.get(context).dao()
        val probe = ProbeSettings(context)
        val firstHabits = dao.allHabits().filter { it.status == "ACTIVE" }
        val firstEvents = dao.allEvents()
        provideContent {
            // Room Flows: the widget re-renders whenever the app, quick screen or widget writes a log.
            val habits by dao.activeHabitsFlow().collectAsState(firstHabits)
            val events by dao.allEventsFlow().collectAsState(firstEvents)
            val mode = currentState<Preferences>()[WIDGET_MODE_KEY] ?: MODE_GRAPH
            val size = LocalSize.current
            val density = LocalContext.current.resources.displayMetrics.density
            // Read on every recomposition so a probe-setting change in the app shows up after updateAll().
            val interaction = probe.interactionMode
            val variant = probe.quickVariant
            val state =
                remember(habits, events, mode, size, interaction, variant) {
                    buildState(habits, events, mode, interaction, variant, size, density)
                }
            WidgetBody(state)
        }
    }

    /** Keep Glance's default error layout, but record the failure so the device test can see it in the probe log. */
    override fun onCompositionError(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int,
        throwable: Throwable,
    ) {
        Log.e("BrainRotWidget", "widget composition failed", throwable)
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.get(context).dao().insertTap(
                TapLogEntity(
                    wallTimeUtcMs = System.currentTimeMillis(),
                    elapsedRealtimeMs = SystemClock.elapsedRealtime(),
                    sincePreviousMs = null,
                    outcome = "WIDGET_ERROR",
                    interactionMode = ProbeSettings(context).interactionMode.name,
                    windowMs = 0,
                    processAgeMs = SystemClock.elapsedRealtime() - BrainRotApp.processStartElapsedMs,
                    note = "${throwable::class.simpleName}: ${throwable.message}".take(500),
                ),
            )
        }
        super.onCompositionError(context, glanceId, appWidgetId, throwable)
    }

    companion object {
        fun buildState(
            habits: List<HabitEntity>,
            events: List<LogEventEntity>,
            mode: String,
            interaction: InteractionMode,
            variant: QuickVariant,
            size: DpSize,
            density: Float,
            today: LocalDate = LocalDate.now(),
        ): WidgetUiState {
            val engine = LogRepository.engine(habits, events)
            val summary = engine.evaluate(today)
            val c = summary.dailyCompletion
            val score = summary.dailyScore?.roundToInt()?.toString() ?: "–"
            val line = "Today ${c.completed}/${c.eligible} done · score $score"
            val rows =
                habits.map { h ->
                    val day = summary.habits.getValue(h.id)
                    val unit = h.unit?.let { " $it" } ?: ""
                    QuickRow(
                        habitId = h.id,
                        name = h.name,
                        isBinary = h.measurement == "BINARY",
                        done = day.status == Status.COMPLETED,
                        amountText = if (h.measurement == "BINARY") "" else "${fmt(day.recorded)}/${fmt(h.target)}$unit",
                        step = h.step,
                    )
                }
            val graph =
                if (mode == MODE_GRAPH) {
                    // Leave room for the summary row (~32 dp) and padding (~16 dp).
                    val wPx = ((size.width.value - 16f) * density).toInt().coerceAtLeast(40)
                    val hPx = ((size.height.value - 48f) * density).toInt().coerceAtLeast(40)
                    val geometry = HeatmapLayout.fit(wPx, hPx)
                    HeatmapRenderer.render(
                        geometry,
                        HeatmapLayout.levels(engine, today, geometry.columns),
                        opaqueBackground = WIDGET_BG_ARGB,
                    )
                } else {
                    null
                }
            return WidgetUiState(mode, interaction, variant, line, rows, graph)
        }

        private fun fmt(v: Double) = if (v == Math.floor(v)) v.toLong().toString() else "%.1f".format(v)
    }
}

class BrainRotWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BrainRotWidget()
}

private fun text(
    color: Color = TEXT,
    size: Int = 13,
    bold: Boolean = false,
) = TextStyle(
    color = ColorProvider(color),
    fontSize = size.sp,
    fontWeight = if (bold) FontWeight.Medium else FontWeight.Normal,
)

@Composable
fun WidgetBody(state: WidgetUiState) {
    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(BG)
                .cornerRadius(16.dp)
                .padding(8.dp),
    ) {
        if (state.mode == MODE_QUICK) QuickContent(state) else GraphContent(state)
    }
}

@Composable
private fun GraphContent(state: WidgetUiState) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(state.summaryLine, style = text(MUTED, 12), modifier = GlanceModifier.defaultWeight())
        when (state.interaction) {
            InteractionMode.TOGGLE_BUTTON -> Button("Log", onClick = actionRunCallback<ToggleModeAction>())
            InteractionMode.OPEN_SCREEN -> Button("Log", onClick = actionStartActivity<QuickEntryActivity>())
            InteractionMode.DOUBLE_TAP_PROBE -> Unit // no button: the graph itself is the (experimental) target
        }
    }
    val graph = state.graph ?: return
    val image: @Composable () -> Unit = {
        Image(
            provider = ImageProvider(graph),
            contentDescription = "Habit contribution graph",
            contentScale = ContentScale.Fit,
            modifier = GlanceModifier.fillMaxSize(),
        )
    }
    if (state.interaction == InteractionMode.DOUBLE_TAP_PROBE) {
        // Each tap is a separate click delivered to GraphTapAction; it decides whether two taps were a "double-tap".
        Box(modifier = GlanceModifier.fillMaxSize().clickable(actionRunCallback<GraphTapAction>())) { image() }
    } else {
        // Ordinary taps on the graph do nothing (handoff: graph taps must not navigate).
        Box(modifier = GlanceModifier.fillMaxSize()) { image() }
    }
}

@Composable
private fun QuickContent(state: WidgetUiState) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Quick log", style = text(TEXT, 14, bold = true), modifier = GlanceModifier.defaultWeight())
        Button("Done", onClick = actionRunCallback<ToggleModeAction>())
    }
    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
        items(state.rows, itemId = { it.habitId.hashCode().toLong() }) { row -> QuickRowView(row, state.variant) }
    }
}

@Composable
private fun QuickRowView(
    row: QuickRow,
    variant: QuickVariant,
) {
    val params = actionParametersOf(HABIT_ID to row.habitId)
    Row(
        modifier =
            GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .background(SURFACE)
                .cornerRadius(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (row.isBinary || variant == QuickVariant.CHECKBOXES) {
            val label = if (row.amountText.isEmpty()) row.name else "${row.name}  ${row.amountText}"
            CheckBox(
                checked = row.done,
                onCheckedChange = actionRunCallback<ToggleCompletionAction>(params),
                text = label,
                style = text(),
                modifier = GlanceModifier.fillMaxWidth(),
            )
        } else {
            Text(row.name, style = text(), modifier = GlanceModifier.defaultWeight().padding(start = 8.dp))
            Text(row.amountText, style = text(if (row.done) ACCENT else MUTED, 12))
            Spacer(GlanceModifier.width(6.dp))
            Button("−", onClick = adjust(row, -row.step))
            Button("+", onClick = adjust(row, row.step))
        }
    }
}

private fun adjust(
    row: QuickRow,
    delta: Double,
): Action = actionRunCallback<AdjustAmountAction>(actionParametersOf(HABIT_ID to row.habitId, DELTA to delta))
