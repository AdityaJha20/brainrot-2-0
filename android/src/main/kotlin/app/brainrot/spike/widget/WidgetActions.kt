package app.brainrot.spike.widget

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.ToggleableStateKey
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.brainrot.core.scoring.EventType
import app.brainrot.core.widget.DoubleTapDetector
import app.brainrot.spike.BrainRotApp
import app.brainrot.spike.data.AppDatabase
import app.brainrot.spike.data.LogRepository
import app.brainrot.spike.data.TapLogEntity
import java.util.concurrent.TimeUnit

private const val TAG = "BrainRotWidget"

/** Quick entry switches back to the graph on its own after this long, so it never gets "stuck". */
const val QUICK_MODE_TIMEOUT_SECONDS = 90L

suspend fun setWidgetMode(
    context: Context,
    glanceId: GlanceId,
    mode: String,
) {
    updateAppWidgetState(context, glanceId) { it[WIDGET_MODE_KEY] = mode }
    BrainRotWidget().update(context, glanceId)
    val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
    val work = WorkManager.getInstance(context)
    val name = "quick-mode-timeout-$appWidgetId"
    if (mode == MODE_QUICK) {
        work.enqueueUniqueWork(
            name,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<QuickModeTimeoutWorker>()
                .setInitialDelay(
                    QUICK_MODE_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS,
                ).build(),
        )
    } else {
        work.cancelUniqueWork(name)
    }
}

class ToggleModeAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val prefs = getAppWidgetState<Preferences>(context, PreferencesGlanceStateDefinition, glanceId)
        val current = prefs[WIDGET_MODE_KEY] ?: MODE_GRAPH
        setWidgetMode(context, glanceId, if (current == MODE_QUICK) MODE_GRAPH else MODE_QUICK)
    }
}

/**
 * Experimental double-tap: every tap on the graph lands here as an independent click. The first tap must change
 * nothing on screen; a second tap within the window switches to quick entry. Every tap is written to the probe
 * log with its timing and whether it had to start the app process, which is the evidence the device test needs.
 */
class GraphTapAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val now = SystemClock.elapsedRealtime()
        val probe = ProbeSettings(context)
        val detector = DoubleTapDetector(probe.doubleTapWindowMs)
        val result = detector.onTap(now, DoubleTapDetector.State(probe.lastTapElapsedMs))
        probe.lastTapElapsedMs = result.newState.lastTapAtMs
        AppDatabase.get(context).dao().insertTap(
            TapLogEntity(
                wallTimeUtcMs = System.currentTimeMillis(),
                elapsedRealtimeMs = now,
                sincePreviousMs = result.sincePreviousMs,
                outcome = result.outcome.name,
                interactionMode = probe.interactionMode.name,
                windowMs = probe.doubleTapWindowMs,
                processAgeMs = now - BrainRotApp.processStartElapsedMs,
                note = null,
            ),
        )
        Log.i(TAG, "graph tap: ${result.outcome} since=${result.sincePreviousMs}ms window=${detector.windowMs}ms")
        if (result.outcome == DoubleTapDetector.Outcome.DOUBLE_TAP) setWidgetMode(context, glanceId, MODE_QUICK)
    }
}

class AdjustAmountAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val habitId = parameters[HABIT_ID] ?: return
        val delta = parameters[DELTA] ?: return
        val repo = LogRepository(AppDatabase.get(context))
        val habit =
            AppDatabase
                .get(context)
                .dao()
                .allHabits()
                .firstOrNull { it.id == habitId } ?: return
        val type = if (habit.measurement == "DURATION") EventType.DURATION else EventType.QUANTITY
        val result = repo.log(habitId, type, delta, source = "widget")
        Log.i(TAG, "widget adjust $habitId $delta -> $result")
        refreshWidgets(context)
    }
}

class ToggleCompletionAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val habitId = parameters[HABIT_ID] ?: return
        // On Android 12+ the platform passes the checkbox's new state; fall back to "mark done" if absent.
        val checked = parameters[ToggleableStateKey] ?: true
        val result = LogRepository(AppDatabase.get(context)).setCompletedToday(habitId, checked, source = "widget")
        Log.i(TAG, "widget toggle $habitId checked=$checked -> $result")
        refreshWidgets(context)
    }
}

class QuickModeTimeoutWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(applicationContext)
        manager.getGlanceIds(BrainRotWidget::class.java).forEach { id ->
            updateAppWidgetState(applicationContext, id) { it[WIDGET_MODE_KEY] = MODE_GRAPH }
        }
        BrainRotWidget().updateAll(applicationContext)
        return Result.success()
    }
}

/** Called after any write from outside the widget's own session (app, quick screen). */
suspend fun refreshWidgets(context: Context) {
    runCatching { BrainRotWidget().updateAll(context) }
        .onFailure { Log.w(TAG, "widget refresh failed", it) }
}
