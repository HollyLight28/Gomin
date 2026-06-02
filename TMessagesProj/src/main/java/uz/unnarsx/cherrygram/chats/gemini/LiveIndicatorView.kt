package uz.unnarsx.cherrygram.chats.gemini

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.Theme
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import android.graphics.Typeface

class LiveIndicatorView(context: Context) : LinearLayout(context) {

    private val dotView: View
    private val textView: TextView

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        dotView = View(context).apply {
            val size = AndroidUtilities.dp(8f)
            layoutParams = LayoutParams(size, size).apply {
                rightMargin = AndroidUtilities.dp(6f)
            }
            background = Theme.createRoundRectDrawable(size / 2, 0xFFFF0000.toInt())
        }
        addView(dotView)

        textView = TextView(context).apply {
            text = "LIVE"
            setTextColor(0xFFFF0000.toInt())
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
        }
        addView(textView)
    }

    fun startAnimation() {
        val anim = AlphaAnimation(1f, 0.2f).apply {
            duration = 800
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        dotView.startAnimation(anim)
    }

    fun stopAnimation() {
        dotView.clearAnimation()
    }
}
