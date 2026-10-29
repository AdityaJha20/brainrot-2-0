package app.brainrot.spike.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import app.brainrot.core.widget.HeatmapGeometry

/** Five intensity levels: 0 = no activity ... 4 = perfect day (brightest, reserved). Placeholder palette. */
data class HeatmapPalette(
    val levels: List<Int>,
) {
    init {
        require(levels.size == 5)
    }

    companion object {
        // Dark-first green/teal ramp from the handoff's design tokens (to be contrast-checked in Phase 1).
        val TEAL =
            HeatmapPalette(
                listOf(0xFF22272A.toInt(), 0xFF0E4D45.toInt(), 0xFF127A68.toInt(), 0xFF1BAF8F.toInt(), 0xFF5EF2C6.toInt()),
            )
    }
}

/**
 * Draws the contribution graph into a single bitmap (one Glance Image instead of 140+ views, which Glance
 * cannot hold). All geometry comes from core's HeatmapLayout so the app, widget and tests agree.
 */
object HeatmapRenderer {
    /**
     * [opaqueBackground] = null: ARGB_8888 with transparent gaps (4 bytes/px, exact colours; used in the app).
     * [opaqueBackground] = a colour: RGB_565 painted on that colour (2 bytes/px). The widget uses this because a 4x2
     * ARGB graph is ~1 MB, right at the binder buffer size; flat squares lose nothing visible in 16-bit colour.
     */
    fun render(
        geometry: HeatmapGeometry,
        levels: List<List<Int?>>,
        palette: HeatmapPalette = HeatmapPalette.TEAL,
        opaqueBackground: Int? = null,
    ): Bitmap {
        val config = if (opaqueBackground == null) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        val bitmap = Bitmap.createBitmap(geometry.widthPx, geometry.heightPx, config)
        val canvas = Canvas(bitmap)
        opaqueBackground?.let { canvas.drawColor(it) }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val radius = geometry.cellPx * 0.2f
        val rect = RectF()
        levels.forEachIndexed { c, column ->
            column.forEachIndexed { r, level ->
                if (level == null) return@forEachIndexed // future day: leave empty
                val left = (c * (geometry.cellPx + geometry.gapPx)).toFloat()
                val top = (r * (geometry.cellPx + geometry.gapPx)).toFloat()
                rect.set(left, top, left + geometry.cellPx, top + geometry.cellPx)
                paint.color = palette.levels[level.coerceIn(0, 4)]
                canvas.drawRoundRect(rect, radius, radius, paint)
            }
        }
        return bitmap
    }
}
