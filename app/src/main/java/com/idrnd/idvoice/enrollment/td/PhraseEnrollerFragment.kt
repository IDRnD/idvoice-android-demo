package com.idrnd.idvoice.enrollment.td

import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import androidx.fragment.app.viewModels
import androidx.transition.TransitionManager
import com.idrnd.idvoice.R
import com.idrnd.idvoice.enrollment.td.PhraseEnrollerViewModel.Companion.PhraseEnrollerViewModelFactory
import com.idrnd.idvoice.useCaseSelector.UseCaseSelectorFragment
import com.idrnd.idvoice.utils.extensions.replaceWithFragment
import com.idrnd.idvoice.utils.views.PhraseEnrollmentView
import com.idrnd.idvoice.utils.views.PhraseEnrollmentView.State.LivenessChecking
import com.idrnd.idvoice.utils.views.PhraseEnrollmentView.State.Process
import com.idrnd.idvoice.utils.views.PhraseEnrollmentView.State.ProcessIsFinished
import com.idrnd.idvoice.utils.views.PhraseEnrollmentView.State.Record

class PhraseEnrollerFragment : Fragment(R.layout.phrase_enroller_fragmnet) {

    private lateinit var phraseEnrollmentView: PhraseEnrollmentView
    private lateinit var backButton: Button
    private lateinit var livenessCheckProgressBar: ProgressBar

    private val viewModel: PhraseEnrollerViewModel by viewModels { PhraseEnrollerViewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get views
        phraseEnrollmentView = view.findViewById(R.id.phraseEnrollmentView)
        backButton = view.findViewById(R.id.myTextDependentEnrollBackButton)
        livenessCheckProgressBar = view.findViewById(R.id.livenessCheckProgressBar)

        // Set listeners/observers
        viewModel.messageId.observe(viewLifecycleOwner) { messageId ->
            messageId ?: return@observe

            Toast.makeText(
                requireActivity().applicationContext,
                getString(messageId),
                Toast.LENGTH_SHORT
            ).show()
        }

        viewModel.needResetVisualization.observe(viewLifecycleOwner) { resetVisualization ->
            resetVisualization ?: return@observe

            if (resetVisualization) phraseEnrollmentView.stopVisualization()
        }

        viewModel.isSpeechRecorded.observe(viewLifecycleOwner) { isSpeechRecorded ->
            isSpeechRecorded ?: return@observe
            phraseEnrollmentView.visualize()
        }

        viewModel.recordRecordingIndex.observe(viewLifecycleOwner) { index ->
            index ?: return@observe

            for (i in 0 until index) {
                phraseEnrollmentView.setCheckedRecordIndicatorByIndex(i, true)
            }
        }

        viewModel.phraseForPronouncing.observe(viewLifecycleOwner) { phrase ->
            phrase ?: return@observe

            phraseEnrollmentView.phraseForPronouncing = phrase
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            state ?: return@observe

            // To animate view changes
            TransitionManager.beginDelayedTransition(requireView() as ViewGroup)

            // Set state
            phraseEnrollmentView.state = state

            when (state) {
                Record -> {
                    livenessCheckProgressBar.isVisible = false
                }
                LivenessChecking -> {
                    livenessCheckProgressBar.isVisible = true
                }
                Process -> {
                    livenessCheckProgressBar.isVisible = false
                    backButton.visibility = GONE
                }
                ProcessIsFinished -> {
                    // Clear back stack
                    parentFragmentManager.popBackStack(null, POP_BACK_STACK_INCLUSIVE)

                    // Go to start fragment
                    replaceWithFragment(UseCaseSelectorFragment(), false)
                }
            }
        }

        backButton.setOnClickListener {
            it.isClickable = false
            requireActivity().onBackPressed()
        }

        // Init phrase enrollment view
        phraseEnrollmentView.lifecycle = lifecycle
        phraseEnrollmentView.messageAboutPhrase = getString(R.string.please_pronounce_phrase)

        // Init indicator views
        for (i in 0 until NUMBER_RECORDS) {
            phraseEnrollmentView.setCheckedRecordIndicatorByIndex(i, false)
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.startRecord()
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopRecord()
    }

    companion object {
        private const val NUMBER_RECORDS = 3
    }
}
