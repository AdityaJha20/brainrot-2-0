package app.brainrot.core.widget

import app.brainrot.core.scoring.ScoringEngine
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.max

/**
 * Pure geometry and data for the GitHub-style contribution graph, shared by the in-app graph and the
 * widget bitmap. Drawing itself happens on Android (android.graphics), but every number that decides
 * what is drawn is computed and tested here.
 *
 * Why a bitmap: Glance keeps only the first 10 children of each Row/Column/Box and at most ~500
 * views per widget, so a 7 × 20 grid of individual boxes (140 cells) does not fit. One Image does.
 */
data class HeatmapGeometry(
    val columns: Int,
    val rows: Int,
    val cellPx: Int,
    val gapPx: Int,
    val widthPx: Int,
    val heightPx: Int,
) {
    /** ARGB_8888 = 4 bytes per pixel (transparent background). */
    val bitmapBytes: Long get() = widthPx.toLong() * heightPx * 4

    /** RGB_565 = 2 bytes per pixel (opaque background), what the widget uses to halve the payload. */
    val opaqueBitmapBytes: Long get() = widthPx.toLong() * heightPx * 2
}

object HeatmapLayout {
    const val ROWS = 7

    /**
     * Fit whole weeks into the available widget area. Cells are square. The cell is as large as the height allows,
     * but never so large that fewer than [minWeeks] weeks fit across (tall widgets would otherwise show fewer weeks).
     * [availableWidthPx]/[availableHeightPx] are the widget's content size in pixels.
     */
    fun fit(
        availableWidthPx: Int,
        availableHeightPx: Int,
        minWeeks: Int = 20,
        maxColumns: Int = 53,
        minCellPx: Int = 6,
        gapRatio: Double = 0.2,
    ): HeatmapGeometry {
        require(availableWidthPx > 0 && availableHeightPx > 0)
        val cellFromHeight = floor(availableHeightPx / (ROWS + (ROWS - 1) * gapRatio)).toInt()
        val cellForMinWeeks = floor(availableWidthPx / (minWeeks + (minWeeks - 1) * gapRatio)).toInt()
        val cell = max(minOf(cellFromHeight, cellForMinWeeks), minCellPx)
        val gap = max(1, (cell * gapRatio).toInt())
        val columns = ((availableWidthPx + gap) / (cell + gap)).coerceIn(1, maxColumns)
        return HeatmapGeometry(
            columns = columns,
            rows = ROWS,
            cellPx = cell,
            gapPx = gap,
            widthPx = columns * cell + (columns - 1) * gap,
            heightPx = ROWS * cell + (ROWS - 1) * gap,
        )
    }

    /**
     * Levels (0..4) laid out column by column, Monday at the top, ending with the week containing
     * [endDate]. Days after [endDate] are null (drawn empty). Uses the shared scoring engine, so the
     * widget can never disagree with the app or the web companion about a day's level.
     */
    fun levels(
        engine: ScoringEngine,
        endDate: LocalDate,
        columns: Int,
    ): List<List<Int?>> {
        val lastMonday = endDate.with(DayOfWeek.MONDAY)
        val firstMonday = lastMonday.minusWeeks((columns - 1).toLong())
        return (0 until columns).map { c ->
            (0 until ROWS).map { r ->
                val day = firstMonday.plusWeeks(c.toLong()).plusDays(r.toLong())
                if (day > endDate) null else engine.evaluate(day).heatmapLevel
            }
        }
    }
}

/**
 * Android caps the total bitmap memory one widget update may carry at 1.5 × screen width × height × 4 bytes
 * (AppWidgetManager / RemoteViews source; Android 16 throws if exceeded). Separately, updates are sent over
 * binder, whose buffer is ~1 MB shared per process; large bitmaps are usually moved via shared memory, but
 * that is exactly what the on-device test must confirm. Size bitmaps to the widget, never to the screen.
 */
object WidgetBitmapBudget {
    fun maxBitmapBytes(
        screenWidthPx: Int,
        screenHeightPx: Int,
    ): Long = (screenWidthPx.toLong() * screenHeightPx * 4 * 3) / 2

    const val BINDER_BUFFER_BYTES = 1_048_576L
}
