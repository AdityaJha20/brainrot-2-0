package app.brainrot.spike

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.brainrot.core.widget.HeatmapLayout
import app.brainrot.core.widget.WidgetBitmapBudget
import app.brainrot.spike.widget.HeatmapPalette
import app.brainrot.spike.widget.HeatmapRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the real contribution-graph bitmap with Android's native graphics stack (Robolectric NATIVE mode) and
 * saves PNGs under docs/phase0/evidence so a human can look at them. This proves the drawing code and sizes; it
 * does NOT prove how the OPPO launcher displays it (that needs the phone).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HeatmapRendererTest {
    // Synthetic levels: a ramp plus empty future days at the end, so every colour appears.
    private fun sampleLevels(columns: Int): List<List<Int?>> =
        (0 until columns).map { c ->
            (0 until 7).map { r -> if (c == columns - 1 && r > 2) null else (c * 7 + r * 3) % 5 }
        }

    private fun save(
        bitmap: Bitmap,
        name: String,
    ) {
        val dir = File(System.getProperty("phase0.reportDir") ?: "build/phase0").apply { mkdirs() }
        File(dir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun rendersAtExactWidgetPixelSizeWithExpectedColours() {
        // 4x2 widget content area on a ~2.6x density 1080 px-wide screen (OPPO F29 class), minus header/padding.
        val g = HeatmapLayout.fit(availableWidthPx = 930, availableHeightPx = 280)
        val levels = sampleLevels(g.columns)
        val bmp = HeatmapRenderer.render(g, levels)
        assertEquals(g.widthPx, bmp.width)
        assertEquals(g.heightPx, bmp.height)

        // Centre of every drawn cell has exactly its level's colour; future cells stay transparent.
        levels.forEachIndexed { c, col ->
            col.forEachIndexed { r, level ->
                val x = c * (g.cellPx + g.gapPx) + g.cellPx / 2
                val y = r * (g.cellPx + g.gapPx) + g.cellPx / 2
                val expected = if (level == null) 0 else HeatmapPalette.TEAL.levels[level]
                assertEquals("cell c=$c r=$r", expected, bmp.getPixel(x, y))
            }
        }
        save(bmp, "heatmap-4x2-${bmp.width}x${bmp.height}.png")
        println("4x2 widget graph: ${g.columns} weeks, ${bmp.width}x${bmp.height}px, ${bmp.byteCount} bytes")
    }

    @Test
    fun bitmapSizeStaysFarBelowAndroidsWidgetLimitOnTheF29() {
        val budget = WidgetBitmapBudget.maxBitmapBytes(1080, 2412)
        listOf(930 to 280, 930 to 600, 460 to 280).forEach { (w, h) ->
            val g = HeatmapLayout.fit(w, h)
            val bmp = HeatmapRenderer.render(g, sampleLevels(g.columns))
            assertEquals(g.bitmapBytes, bmp.byteCount.toLong())
            assertTrue("${bmp.byteCount} B vs budget $budget", bmp.byteCount * 4L < budget)
            println(
                "area ${w}x$h -> ${g.columns} weeks, bitmap ${bmp.width}x${bmp.height} = ${bmp.byteCount} bytes (budget $budget)",
            )
        }
    }

    @Test
    fun widgetVariantIsOpaque16BitAndHalfTheSize() {
        val g = HeatmapLayout.fit(availableWidthPx = 930, availableHeightPx = 280)
        val bmp = HeatmapRenderer.render(g, sampleLevels(g.columns), opaqueBackground = 0xFF0B0D0E.toInt())
        assertEquals(Bitmap.Config.RGB_565, bmp.config)
        assertEquals(g.opaqueBitmapBytes, bmp.byteCount.toLong())
        assertTrue(
            "widget bitmap ${bmp.byteCount} B should be well under the ~1 MB binder buffer",
            bmp.byteCount < 600_000,
        )
        save(bmp, "heatmap-widget-rgb565-${bmp.width}x${bmp.height}.png")
        println("widget (RGB_565) graph: ${bmp.width}x${bmp.height}px, ${bmp.byteCount} bytes")
    }

    @Test
    fun smallWidgetRendering() {
        val g = HeatmapLayout.fit(availableWidthPx = 440, availableHeightPx = 200)
        save(HeatmapRenderer.render(g, sampleLevels(g.columns)), "heatmap-2x2-${g.widthPx}x${g.heightPx}.png")
        assertTrue(g.columns >= 8)
    }
}
