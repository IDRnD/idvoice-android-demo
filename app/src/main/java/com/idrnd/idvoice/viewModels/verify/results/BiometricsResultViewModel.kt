package com.idrnd.idvoice.viewModels.verify.results

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.idrnd.idvoice.R
import com.idrnd.idvoice.model.GlobalPrefs

class BiometricsResultViewModel : ViewModel() {

    /**
     * Indicates whether this view model is initialized or not.
     */
    var isInitialized = false
        private set

    lateinit var verificationValueToColor: Pair<String, Int>
    lateinit var livenessValueToColor: Pair<String, Int>

    fun init(
        context: Context,
        normVerifyProbability: Float,
        livenessScore: Float,
    ) {

        // Init colors
        val positiveColor = ContextCompat.getColor(context, R.color.shamrock)
        val negativeColor = ContextCompat.getColor(context, R.color.mojo)

        // Init a verify value for view
        val verifyProbability = (normVerifyProbability * 100).toInt()
        val verifyColor = if (normVerifyProbability >= GlobalPrefs.verifyThreshold) positiveColor else negativeColor
        verificationValueToColor = PERCENT_FORMAT.format(verifyProbability) to verifyColor

        // Init a liveness value for view
        livenessValueToColor = if (livenessScore >= GlobalPrefs.livenessThreshold) {
            context.getString(R.string.voice_is_genuine) to positiveColor
        } else {
            context.getString(R.string.voice_is_spoofed) to negativeColor
        }

        // Set that view model is initialized
        isInitialized = true
    }

    companion object {
        private const val PERCENT_FORMAT = "%d %%"
    }
}
