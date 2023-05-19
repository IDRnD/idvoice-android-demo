package com.idrnd.idvoice.utils.views.lightCircle

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import com.idrnd.idvoice.R

/**
 * Circle for visualization.
 */
class LightCircle(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ConstraintLayout(context, attrs, defStyleAttr) {

    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, 0)

    init {
        LayoutInflater.from(context).inflate(R.layout.light_circle, this)
    }

    val isLightOn: Boolean
        get() = findViewById<View>(R.id.greenCircle).isVisible

    fun lightOn() {
        TransitionManager.beginDelayedTransition(this)
        findViewById<View>(R.id.greenCircle).isVisible = true
    }

    fun lightOff() {
        TransitionManager.beginDelayedTransition(this)
        findViewById<View>(R.id.greenCircle).isVisible = false
    }
}
