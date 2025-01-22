package com.idrnd.idvoice.verification.results

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import androidx.fragment.app.viewModels
import com.idrnd.idvoice.R
import com.idrnd.idvoice.useCaseSelector.UseCaseSelectorFragment
import com.idrnd.idvoice.utils.extensions.replaceWithFragment

open class BiometricsResultFragment : Fragment(R.layout.biomertics_result_fragment) {

    private val viewModel: BiometricsResultViewModel by viewModels {
        BiometricsResultViewModel.BiometricsResultViewModelFactory(
            requireArguments().getFloat(BUNDLE_VERIFICATION_PROBABILITY),
            requireArguments().getFloat(BUNDLE_LIVENESS_PROBABILITY),
            requireArguments().getBoolean(BUNDLE_MULTIPLE_SPEAKERS_DETECTED_WARNING)
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        requireActivity().onBackPressedDispatcher.addCallback(this, true) {
            view?.findViewById<Button>(R.id.acceptButton)?.callOnClick()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postponeEnterTransition()

        // Get views
        val verificationValue = view.findViewById<TextView>(R.id.verificationValue)
        val warningLabel = view.findViewById<TextView>(R.id.warningLabel)
        val livenessValue = view.findViewById<TextView>(R.id.livenessValue)
        val acceptButton = view.findViewById<Button>(R.id.acceptButton)
        val backButton = view.findViewById<Button>(R.id.verifyBackButton)

        // Init view with verification result
        val verifyProbabilityToColor = viewModel.verificationValueToColor

        // Set multiple speakers detected label visibility
        warningLabel.isVisible = viewModel.showMultipleSpeakersWarning

        verificationValue.text = verifyProbabilityToColor.first
        verificationValue.setTextColor(verifyProbabilityToColor.second)

        // Init view with liveness result
        val livenessValueToColor = viewModel.livenessValueToColor

        livenessValue.text = livenessValueToColor.first
        livenessValue.setTextColor(livenessValueToColor.second)

        // Init listeners
        backButton.setOnClickListener {
            it.isClickable = false
            requireActivity().onBackPressed()
        }

        acceptButton.setOnClickListener {
            acceptButton.isClickable = false

            // Clear back stack
            parentFragmentManager.popBackStack(null, POP_BACK_STACK_INCLUSIVE)

            // Go to start fragment
            replaceWithFragment(UseCaseSelectorFragment::class.java, false)
        }

        startPostponedEnterTransition()
    }

    companion object {
        const val BUNDLE_VERIFICATION_PROBABILITY = "BUNDLE_VERIFICATION_PROBABILITY"
        const val BUNDLE_LIVENESS_PROBABILITY = "BUNDLE_LIVENESS_PROBABILITY"
        const val BUNDLE_MULTIPLE_SPEAKERS_DETECTED_WARNING = "BUNDLE_WARNING"
    }
}
