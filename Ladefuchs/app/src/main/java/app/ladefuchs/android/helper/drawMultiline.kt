/*
 * This file holds Functions to draw the multiline text.
 * The function is present three times depending on the users Android version.
 */
package app.ladefuchs.android.helper

import android.graphics.Canvas
import android.graphics.text.LineBreaker
import android.os.Build
import android.text.*
import androidx.annotation.RequiresApi
import androidx.core.graphics.withTranslation
import app.ladefuchs.android.ui.chargecards.ChargeCardFragment



fun StaticLayout.draw(canvas: Canvas, x: Float, y: Float) {
    canvas.withTranslation(x, y) {
        draw(this)
    }
}
// this is blatantly stolen from https://medium.com/over-engineering/drawing-multiline-text-to-canvas-on-android-9b98f0bfa16a (credit @ricknout)
@RequiresApi(Build.VERSION_CODES.O)
fun Canvas.drawMultilineText(
    text: CharSequence,
    textPaint: TextPaint,
    width: Int,
    x: Float,
    y: Float,
    start: Int = 0,
    end: Int = text.length,
    alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    textDir: TextDirectionHeuristic = TextDirectionHeuristics.FIRSTSTRONG_LTR,
    spacingMult: Float = 1f,
    spacingAdd: Float = 0f,
    includePad: Boolean = true,
    ellipsizedWidth: Int = width,
    ellipsize: TextUtils.TruncateAt? = null,
    maxLines: Int = Int.MAX_VALUE,
    breakStrategy: Int = LineBreaker.BREAK_STRATEGY_SIMPLE,
    hyphenationFrequency: Int = Layout.HYPHENATION_FREQUENCY_NONE,
    justificationMode: Int = LineBreaker.JUSTIFICATION_MODE_NONE
) {

    val cacheKey = "$text-$start-$end-$textPaint-$width-$alignment-$textDir-" +
            "$spacingMult-$spacingAdd-$includePad-$ellipsizedWidth-$ellipsize-" +
            "$maxLines-$breakStrategy-$hyphenationFrequency-$justificationMode"

    val staticLayout = ChargeCardFragment.StaticLayoutCache[cacheKey] ?: StaticLayout.Builder.obtain(
        text,
        start,
        end,
        textPaint,
        width
    )
        .setAlignment(alignment)
        .setTextDirection(textDir)
        .setLineSpacing(spacingAdd, spacingMult)
        .setIncludePad(includePad)
        .setEllipsizedWidth(ellipsizedWidth)
        .setEllipsize(ellipsize)
        .setMaxLines(maxLines)
        .setBreakStrategy(breakStrategy)
        .setHyphenationFrequency(hyphenationFrequency)
        .setJustificationMode(justificationMode)
        .build().apply { ChargeCardFragment.StaticLayoutCache[cacheKey] = this }

    staticLayout.draw(this, x, y)
}

@RequiresApi(Build.VERSION_CODES.M)
fun Canvas.drawMultilineText(
    text: CharSequence,
    textPaint: TextPaint,
    width: Int,
    x: Float,
    y: Float,
    start: Int = 0,
    end: Int = text.length,
    alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    textDir: TextDirectionHeuristic = TextDirectionHeuristics.FIRSTSTRONG_LTR,
    spacingMult: Float = 1f,
    spacingAdd: Float = 0f,
    includePad: Boolean = true,
    ellipsizedWidth: Int = width,
    ellipsize: TextUtils.TruncateAt? = null,
    maxLines: Int = Int.MAX_VALUE,
    breakStrategy: Int = LineBreaker.BREAK_STRATEGY_SIMPLE,
    hyphenationFrequency: Int = Layout.HYPHENATION_FREQUENCY_NONE
) {

    val cacheKey = "$text-$start-$end-$textPaint-$width-$alignment-$textDir-" +
            "$spacingMult-$spacingAdd-$includePad-$ellipsizedWidth-$ellipsize-" +
            "$maxLines-$breakStrategy-$hyphenationFrequency"

    val staticLayout = ChargeCardFragment.StaticLayoutCache[cacheKey] ?: StaticLayout.Builder.obtain(
        text,
        start,
        end,
        textPaint,
        width
    )
        .setAlignment(alignment)
        .setTextDirection(textDir)
        .setLineSpacing(spacingAdd, spacingMult)
        .setIncludePad(includePad)
        .setEllipsizedWidth(ellipsizedWidth)
        .setEllipsize(ellipsize)
        .setMaxLines(maxLines)
        .setBreakStrategy(breakStrategy)
        .setHyphenationFrequency(hyphenationFrequency)
        .build().apply { ChargeCardFragment.StaticLayoutCache[cacheKey] = this }

    staticLayout.draw(this, x, y)
}

fun Canvas.drawMultilineText(
    text: CharSequence,
    textPaint: TextPaint,
    width: Int,
    x: Float,
    y: Float,
    start: Int = 0,
    end: Int = text.length,
    alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    spacingMult: Float = 1f,
    spacingAdd: Float = 0f,
    includePad: Boolean = true,
    ellipsizedWidth: Int = width,
    ellipsize: TextUtils.TruncateAt? = null
) {

    val cacheKey = "$text-$start-$end-$textPaint-$width-$alignment-" +
            "$spacingMult-$spacingAdd-$includePad-$ellipsizedWidth-$ellipsize"

    // The public constructor was deprecated in API level 28,
    // but the builder is only available from API level 23 onwards
    val staticLayout =
        ChargeCardFragment.StaticLayoutCache[cacheKey] ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, start, end, textPaint, width)
                .setAlignment(alignment)
                .setLineSpacing(spacingAdd, spacingMult)
                .setIncludePad(includePad)
                .setEllipsizedWidth(ellipsizedWidth)
                .setEllipsize(ellipsize)
                .build()
        } else {
            StaticLayout(
                text, start, end, textPaint, width, alignment,
                spacingMult, spacingAdd, includePad, ellipsize, ellipsizedWidth
            )
                .apply { ChargeCardFragment.StaticLayoutCache[cacheKey] = this }
        }

    staticLayout.draw(this, x, y)
}