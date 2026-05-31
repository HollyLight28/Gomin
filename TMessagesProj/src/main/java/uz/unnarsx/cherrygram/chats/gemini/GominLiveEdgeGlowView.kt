package uz.unnarsx.cherrygram.chats.gemini

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

class GominLiveEdgeGlowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 32f
        maskFilter = BlurMaskFilter(24f, BlurMaskFilter.Blur.OUTER)
    }

    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    private val dimPaint = Paint().apply {
        color = Color.argb(180, 10, 10, 15) // Deep, high-contrast premium OLED dark background
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 48f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(150, 220, 220, 230)
        textSize = 34f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    }

    private val pulseCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2AABEE.toInt() // Gomin Premium Blue status
        style = Paint.Style.FILL
    }

    private var rotationAngle = 0f
    private val shaderMatrix = Matrix()
    private var baseGlowWidth = 24f
    private var currentGlowScale = 1.0f

    private var sweepShader: SweepGradient? = null
    
    // Gradient colors for different states
    private val idleColors = intArrayOf(
        0x00FFFFFF.toInt(),
        0x402AABEE.toInt(),
        0x802AABEE.toInt(),
        0x402AABEE.toInt(),
        0x00FFFFFF.toInt()
    )
    
    private val userSpeakingColors = intArrayOf(
        0xFF00C6FF.toInt(), // Premium bright cyan
        0xFF2AABEE.toInt(), // Gomin premium blue
        0xFF0072FF.toInt(), // Rich dark blue
        0xFF00C6FF.toInt()
    )

    private val aiSpeakingColors = intArrayOf(
        0xFFFF416C.toInt(), // Passion red
        0xFFFF4B2B.toInt(), // Vibrant orange
        0xFFFFFFFF.toInt(), // OLED contrast white
        0xFFFF416C.toInt()
    )

    private var currentColors = idleColors

    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 4000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { animation ->
            rotationAngle = animation.animatedValue as Float
            invalidate()
        }
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // Required for BlurMaskFilter compatibility
        animator.start()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recreateShader()
    }

    private fun recreateShader() {
        val cx = width / 2f
        val cy = height / 2f
        if (width > 0 && height > 0) {
            sweepShader = SweepGradient(cx, cy, currentColors, null)
            glowPaint.shader = sweepShader
            corePaint.shader = sweepShader
        }
    }

    fun setAmplitude(rms: Float, isUser: Boolean) {
        val targetColors = if (rms < 0.02f) {
            idleColors
        } else if (isUser) {
            userSpeakingColors
        } else {
            aiSpeakingColors
        }

        if (targetColors !== currentColors) {
            currentColors = targetColors
            recreateShader()
        }

        // Map RMS amplitude to glow scale (e.g. 1.0 to 4.0)
        val targetScale = 1.0f + (rms * 8.0f).coerceAtMost(3.0f)
        currentGlowScale = currentGlowScale * 0.7f + targetScale * 0.3f // Smooth transitions
        
        glowPaint.strokeWidth = baseGlowWidth * currentGlowScale
        corePaint.strokeWidth = 8f * (1.0f + rms * 2.0f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw Dim Background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)

        // Draw Siri-like Status Text & Pulse Indicator
        val textY = 200f
        canvas.drawText("ГОМІН LIVE", width / 2f, textY, textPaint)
        canvas.drawText("Натисніть на екран, щоб завершити розмову", width / 2f, height - 200f, subTextPaint)

        // Draw pulsing green status indicator next to text
        val textWidth = textPaint.measureText("ГОМІН LIVE")
        val circleX = (width / 2f) + (textWidth / 2f) + 40f
        val circleY = textY - 16f
        // Dynamic pulse radius using rotation angle sine for organic motion
        val pulseRadius = 12f + Math.sin(Math.toRadians(rotationAngle.toDouble())).toFloat() * 4f
        canvas.drawCircle(circleX, circleY, pulseRadius, pulseCirclePaint)

        val shader = sweepShader ?: return
        val cx = width / 2f
        val cy = height / 2f

        // Apply shader rotation
        shaderMatrix.reset()
        shaderMatrix.postRotate(rotationAngle, cx, cy)
        shader.setLocalMatrix(shaderMatrix)

        // Draw border path
        val inset = 12f
        canvas.drawRect(
            inset,
            inset,
            width.toFloat() - inset,
            height.toFloat() - inset,
            glowPaint
        )

        canvas.drawRect(
            inset,
            inset,
            width.toFloat() - inset,
            height.toFloat() - inset,
            corePaint
        )
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }
}
