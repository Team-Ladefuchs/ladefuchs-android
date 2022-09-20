package app.ladefuchs.android.helper

import android.graphics.Outline
import android.graphics.Rect
import android.view.View
import android.view.ViewOutlineProvider

class OutlineProvider(
    var xShift: Int,
    var yShift: Int,
    private val rect: Rect = Rect()
    ) : ViewOutlineProvider() {

    override fun getOutline(view: View?, outline: Outline?) {
        view?.background?.copyBounds(rect)
        rect.offset(xShift, yShift)
        outline?.setRoundRect(rect, 15F)
    }
}