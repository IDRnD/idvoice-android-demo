package com.idrnd.idvoice.fragments.verify.results

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import androidx.fragment.app.viewModels
import com.idrnd.idvoice.R
import com.idrnd.idvoice.fragments.StartFragment
import com.idrnd.idvoice.utils.extensions.replaceWithFragment
import com.idrnd.idvoice.viewModels.verify.results.BiometricsResultViewModel

open class BiometricsResultFragment : Fragment(R.layout.biomertics_result_fragment) {

    private lateinit var verificationValue: TextView
    private lateinit var livenessValue: TextView

    private lateinit var acceptButton: Button
    private lateinit var backButton: Button

    private val viewModel: BiometricsResultViewModel by viewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)

        requireActivity().onBackPressedDispatcher.addCallback(this, true) {
            acceptButton.callOnClick()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!viewModel.isInitialized) {
            // Grab passed args
            val normVerifyProbability = requireArguments().getFloat(BUNDLE_VERIFICATION_PROBABILITY)
            val livenessScore = requireArguments().getFloat(BUNDLE_LIVENESS_SCORE)

            // Init a view model
            viewModel.init(
                requireContext(),
                normVerifyProbability,
                livenessScore,
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postponeEnterTransition()

        // Get views
        verificationValue = view.findViewById(R.id.verificationValue)
        livenessValue = view.findViewById(R.id.livenessValue)
        acceptButton = view.findViewById(R.id.acceptButton)
        backButton = view.findViewById(R.id.verifyBackButton)

        // Init view with verification result
        val verifyProbabilityToColor = viewModel.verificationValueToColor

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
            replaceWithFragment(StartFragment(), false)
        }

        startPostponedEnterTransition()
    }

    companion object {
        const val BUNDLE_VERIFICATION_PROBABILITY = "BUNDLE_VERIFICATION_PROBABILITY"
        const val BUNDLE_LIVENESS_SCORE = "BUNDLE_LIVENESS_SCORE"
    }
}
