package com.idrnd.idvoice.utils.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Paint.Style.FILL
import android.util.AttributeSet
import android.view.View

/**
 * Simple circle for visualization.
 */
class SimpleCircle(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, 0)

    private val paint: Paint = Paint(ANTI_ALIAS_FLAG).apply {
        style = FILL
        color = Color.parseColor("#50c99c") // shamrock color
    }

    private var prevRadius = 0f
    private var radiusQueue = ArrayDeque<Float>()

    private var circlePrevAlpha = 0
    private var circleAlphaQueue = ArrayDeque<Int>()

    private var minNormRadius = 0.05f
    private var minNormCircleAlpha = 0.5f

    /**
     * Set normalized radius.
     *
     * @param normRadius normalized radius from 0 to 1.
     * @param radiusGain coefficient of amplification of the normalized radius.
     * @param alphaGain coefficient of amplification of the circle alpha.
     */
    fun setNormalizedRadius(normRadius: Float, radiusGain: Float = 2.5f, alphaGain: Float = 5f) {

        assert(normRadius <= 1.0f) { "Normalized radius must be below or equal 1.0" }
        assert(normRadius >= 0.0f) { "Normalized radius must be above or equal 0.0" }
        assert(radiusGain >= 0.0f) { "Radius gain must be above or equal 0.0" }
        assert(alphaGain >= 0.0f) { "Alpha gain must be above or equal 0.0" }

        // Calculate radius step
        val validNormRadius = when {
            (normRadius <= minNormRadius) -> minNormRadius
            (normRadius * radiusGain >= 1.0f) -> 1.0f
            else -> normRadius * radiusGain
        }

        val radius = (width.coerceAtMost(height) / 2) * validNormRadius
        val radiusStep = (radius - prevRadius) / FPS

        // Calculate alpha step
        val normCircleAlpha = normRadius * alphaGain

        val circleAlpha = when {
            (normCircleAlpha <= minNormCircleAlpha) -> (MAX_ALPHA * minNormCircleAlpha).toInt()
            (normCircleAlpha >= 1.0f) -> MAX_ALPHA
            else -> (MAX_ALPHA * normCircleAlpha).toInt()
        }

        val circleAlphaStep = (circleAlpha - circlePrevAlpha) / FPS

        // Calculate intermediate values for radius and alpha
        for (iter in 1..FPS) {
            val newRadius = prevRadius + radiusStep * iter
            radiusQueue.addLast(newRadius)

            val newAlpha = circlePrevAlpha + circleAlphaStep * iter
            circleAlphaQueue.addLast(newAlpha)
        }

        // Save prev alpha and radius
        circlePrevAlpha = circleAlpha
        prevRadius = radius

        // Draw the view
        invalidate()
    }

    var centerX = 0f
    var centerY = 0f

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        // Find a center
        val width = right - left
        val height = bottom - top

        centerX = width / 2f
        centerY = height / 2f

        // Write a first radius value
        radiusQueue.addLast(centerX.coerceAtMost(centerY) * minNormRadius)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (radiusQueue.isEmpty()) {
            return
        }

        if (circleAlphaQueue.isNotEmpty()) {
            paint.alpha = circleAlphaQueue.removeFirst()
        }

        // Draw circle
        canvas.drawCircle(centerX, centerY, radiusQueue.removeFirst(), paint)

        if (radiusQueue.isNotEmpty()) {
            // Continue drawing
            invalidate()
        }
    }

    companion object {
        private const val FPS = 7
        private const val MAX_ALPHA = 255
    }
}
