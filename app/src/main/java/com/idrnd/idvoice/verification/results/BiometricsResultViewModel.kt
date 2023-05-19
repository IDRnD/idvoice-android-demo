package com.idrnd.idvoice.verification.results

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.idrnd.idvoice.MainApplication
import com.idrnd.idvoice.R
import com.idrnd.idvoice.preferences.GlobalPrefs

class BiometricsResultViewModel(
    context: Context,
    verifyProbability: Float,
    livenessProbability: Float,
) : ViewModel() {

    val verificationValueToColor: Pair<String, Int>
    val livenessValueToColor: Pair<String, Int>

    init {
        // Init colors
        val positiveColor = ContextCompat.getColor(context, R.color.shamrock)
        val negativeColor = ContextCompat.getColor(context, R.color.mojo)

        // Init a verify value for view
        val verifyProbability = (verifyProbability * 100).toInt()
        val verifyColor = if (verifyProbability >= GlobalPrefs.verifyThreshold) positiveColor else negativeColor
        verificationValueToColor = PERCENT_FORMAT.format(verifyProbability) to verifyColor

        // Init a liveness value for view
        livenessValueToColor = if (livenessProbability >= GlobalPrefs.livenessThreshold) {
            context.getString(R.string.voice_is_genuine) to positiveColor
        } else {
            context.getString(R.string.voice_is_spoofed) to negativeColor
        }
    }

    @Suppress("UNCHECKED_CAST")
    class BiometricsResultViewModelFactory(
        private val verifyProbability: Float,
        private val livenessProbability: Float,
    ) : ViewModelProvider.Factory {
        @Throws(IllegalStateException::class)
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            if (!modelClass.isAssignableFrom(BiometricsResultViewModel::class.java)) {
                throw IllegalStateException("Unknown class name ${modelClass.name}")
            }

            val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MainApplication

            return BiometricsResultViewModel(app.applicationContext, verifyProbability, livenessProbability) as T
        }
    }

    companion object {
        private const val PERCENT_FORMAT = "%d %%"
    }
}
