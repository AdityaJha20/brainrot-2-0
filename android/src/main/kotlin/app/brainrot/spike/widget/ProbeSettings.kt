package app.brainrot.spike.widget

import android.content.Context
import androidx.core.content.edit

/** How the widget switches from the graph to quick entry. Chosen in the app's probe screen for device testing. */
enum class InteractionMode(
    val label: String,
) {
    /** Candidate fallback: graph has no tap action; a small "Log" button flips the widget into quick entry. */
    TOGGLE_BUTTON("Log button toggles quick entry"),

    /** The handoff's requested behaviour, simulated by timing two separate taps on the graph. Under test. */
    DOUBLE_TAP_PROBE("Double-tap graph (experimental)"),

    /** Candidate fallback: "Log" button opens a small quick-entry screen over the home screen. */
    OPEN_SCREEN("Log button opens quick-entry screen"),
}

/** The open product question: what quick entry shows. Both are built so they can be compared on the phone. */
enum class QuickVariant(
    val label: String,
) {
    CHECKBOXES("Checkboxes only (one tap = done)"),
    STEPPERS("+/- for amounts, checkbox for yes/no"),
}

/**
 * Probe configuration and the double-tap detector's memory. SharedPreferences (not Glance state) because a widget
 * tap can arrive in a brand-new process and must still see the previous tap's timestamp.
 */
class ProbeSettings(
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences("widget_probe", Context.MODE_PRIVATE)

    var interactionMode: InteractionMode
        get() =
            prefs.getString(KEY_MODE, null)?.let { runCatching { InteractionMode.valueOf(it) }.getOrNull() }
                ?: InteractionMode.TOGGLE_BUTTON
        set(value) = prefs.edit { putString(KEY_MODE, value.name) }

    var quickVariant: QuickVariant
        get() =
            prefs.getString(KEY_VARIANT, null)?.let { runCatching { QuickVariant.valueOf(it) }.getOrNull() }
                ?: QuickVariant.STEPPERS
        set(value) = prefs.edit { putString(KEY_VARIANT, value.name) }

    var doubleTapWindowMs: Long
        get() = prefs.getLong(KEY_WINDOW, 400L)
        set(value) = prefs.edit { putLong(KEY_WINDOW, value) }

    var lastTapElapsedMs: Long?
        get() = if (prefs.contains(KEY_LAST_TAP)) prefs.getLong(KEY_LAST_TAP, 0L) else null
        set(value) =
            prefs.edit(commit = true) {
                if (value == null) remove(KEY_LAST_TAP) else putLong(KEY_LAST_TAP, value)
            }

    companion object {
        private const val KEY_MODE = "interaction_mode"
        private const val KEY_VARIANT = "quick_variant"
        private const val KEY_WINDOW = "double_tap_window_ms"
        private const val KEY_LAST_TAP = "last_tap_elapsed_ms"
        val WINDOW_CHOICES = listOf(250L, 350L, 450L, 600L)
    }
}
