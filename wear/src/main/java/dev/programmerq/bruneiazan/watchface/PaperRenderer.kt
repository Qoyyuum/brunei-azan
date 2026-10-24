package dev.programmerq.bruneiazan.watchface

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import dev.programmerq.bruneiazan.shared.schedule.nextPrayer
import dev.programmerq.bruneiazan.shared.settings.AppFont
import dev.programmerq.bruneiazan.shared.settings.DarkMode
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private const val PAPER = 0xFFFAFAF7.toInt()
private const val INK = 0xFF111111.toInt()
private const val PAPER_DARK = 0xFF0D0D0D.toInt()
private const val INK_LIGHT = 0xFFEDEDE8.toInt()
private const val MUTED = 0xFF6B6B66.toInt()
private const val MUTED_DARK = 0xFF9A9A93.toInt()

class PaperRenderer(
    surfaceHolder: SurfaceHolder,
    currentUserStyleRepository: CurrentUserStyleRepository,
    watchState: WatchState,
    private val faceState: FaceState,
) : Renderer.CanvasRenderer2<PaperRenderer.Shared>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    CanvasType.HARDWARE,
    30_000L,
    false,
) {

    class Shared : Renderer.SharedAssets {
        override fun onDestroy() = Unit
    }

    override suspend fun createSharedAssets(): Shared = Shared()

    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime, sharedAssets: Shared) {
        val ambient = renderParameters.drawMode == DrawMode.AMBIENT
        val settings = faceState.settings.get()
        val dark = ambient || settings.darkMode == DarkMode.DARK

        val bg = if (dark) PAPER_DARK else PAPER
        val ink = if (dark) INK_LIGHT else INK
        val muted = if (dark) MUTED_DARK else MUTED
        val typeface = when (settings.font) {
            AppFont.SANS -> Typeface.DEFAULT
            AppFont.SERIF -> Typeface.SERIF
            AppFont.MONO -> Typeface.MONOSPACE
        }

        canvas.drawColor(bg)

        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        val cx = w / 2f

        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            this.typeface = Typeface.create(typeface, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            textSize = if (ambient) h * 0.20f else h * 0.24f
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
            textSize = h * 0.075f
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
            textSize = h * 0.06f
        }

        // Time
        canvas.drawText(zonedDateTime.format(timeFmt), cx, h * 0.46f, timePaint)

        // Next prayer
        val today = faceState.today.get()
        if (today != null) {
            val now = zonedDateTime.withZoneSameInstant(BRUNEI_ZONE)
            val next = nextPrayer(today, faceState.tomorrow.get(), now.toLocalTime())
            if (next != null) {
                canvas.drawText(
                    "${next.item.name.uppercase()}  ${next.item.time24}",
                    cx, h * 0.60f, labelPaint,
                )
                if (!ambient) {
                    val target = LocalTime.parse(next.item.time24)
                    var d = Duration.between(now.toLocalTime(), target)
                    if (next.isTomorrow || d.isNegative) d = d.plusHours(24)
                    val cd = if (d.toHours() > 0) "in ${d.toHours()}h ${d.toMinutesPart()}m" else "in ${d.toMinutesPart()}m"
                    canvas.drawText(if (next.isTomorrow) "tomorrow · $cd" else cd, cx, h * 0.70f, subPaint)
                    canvas.drawText(today.dateMalay, cx, h * 0.80f, subPaint)
                    canvas.drawText(today.hijri, cx, h * 0.87f, subPaint)
                }
            }
        } else if (!ambient) {
            canvas.drawText("open the app to load times", cx, h * 0.62f, subPaint)
        }
    }

    override fun renderHighlightLayer(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime, sharedAssets: Shared) {
        // No highlight content.
    }
}
