/*
 * This file holds Functions to draw the multiline text.
 * The function is present three times depending on the users Android version.
 */
package app.ladefuchs.android.helper

import android.graphics.Canvas
import android.text.*
import androidx.core.graphics.withTranslation
import app.ladefuchs.android.ui.chargecards.ChargeCardFragment


fun StaticLayout.draw(canvas: Canvas, x: Float, y: Float) {
    canvas.withTranslation(x, y) {
        draw(this)
    }
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

    val staticLayout =
        ChargeCardFragment.StaticLayoutCache[cacheKey] ?: StaticLayout.Builder.obtain(
            text,
            start,
            end,
            textPaint,
            width
        )
            .setAlignment(alignment)
            .setLineSpacing(spacingAdd, spacingMult)
            .setIncludePad(includePad)
            .setEllipsizedWidth(ellipsizedWidth)
            .setEllipsize(ellipsize)
            .build()

    staticLayout.draw(this, x, y)
}