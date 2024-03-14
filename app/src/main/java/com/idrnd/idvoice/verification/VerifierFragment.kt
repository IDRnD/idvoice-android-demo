package com.idrnd.idvoice.verification

import android.os.Bundle
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.idrnd.idvoice.R
import com.idrnd.idvoice.preferences.BiometricsType
import com.idrnd.idvoice.preferences.GlobalPrefs
import com.idrnd.idvoice.utils.extensions.replaceWithFragment
import com.idrnd.idvoice.utils.views.RecordAndProcessPhraseView
import com.idrnd.idvoice.utils.views.RecordAndProcessPhraseView.State.ProcessIsFinished
import com.idrnd.idvoice.utils.views.RecordAndProcessPhraseView.State.Record
import com.idrnd.idvoice.verification.VerifierViewModel.Companion.VerifierViewModelFactory

class VerifierFragment : Fragment(R.layout.verifier_fragment) {

    private lateinit var backButton: Button
    private lateinit var recordAndProcessPhraseView: RecordAndProcessPhraseView
    private lateinit var progressBar: ProgressBar

    private val viewModel: VerifierViewModel by viewModels { VerifierViewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get views
        backButton = view.findViewById(R.id.myVerificationBackButton)
        recordAndProcessPhraseView = view.findViewById(R.id.recordAndProcessPhraseView)
        progressBar = view.findViewById(R.id.verifySpeechProgressBar)
        progressBar.isVisible = (GlobalPrefs.biometricsType == BiometricsType.TextIndependent)

        viewModel.isSpeechRecorded.observe(viewLifecycleOwner) { isSpeechRecorded ->
            isSpeechRecorded ?: return@observe
            recordAndProcessPhraseView.visualize()
        }

        viewModel.messageId.observe(viewLifecycleOwner) { message ->
            message ?: return@observe
            Toast.makeText(requireActivity().applicationContext, message, Toast.LENGTH_LONG).show()
        }

        viewModel.progress.observe(viewLifecycleOwner) { progress ->
            progress ?: return@observe
            progressBar.progress = progress
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            state ?: return@observe

            // To animate view changes
            TransitionManager.beginDelayedTransition(requireView() as ViewGroup)

            recordAndProcessPhraseView.state = state
            backButton.isVisible = (state == Record)

            if (GlobalPrefs.biometricsType == BiometricsType.TextIndependent) {
                progressBar.isVisible = when (state) {
                    Record -> true
                    RecordAndProcessPhraseView.State.Process -> false
                    ProcessIsFinished -> false
                }
            }
        }

        viewModel.phraseForPronouncing.observe(viewLifecycleOwner) { phrase ->
            phrase ?: return@observe

            recordAndProcessPhraseView.phraseForPronouncing = phrase
        }

        viewModel.title.observe(viewLifecycleOwner) { message ->
            message ?: return@observe

            recordAndProcessPhraseView.messageAboutPhrase = message
        }

        viewModel.onResultFragment.observe(viewLifecycleOwner) { fragment ->
            replaceWithFragment(fragment, true)
        }

        backButton.setOnClickListener {
            it.isClickable = false
            requireActivity().onBackPressed()
        }

        // Init lifecycle in record view
        recordAndProcessPhraseView.lifecycle = lifecycle
    }

    override fun onStart() {
        super.onStart()
        viewModel.startRecord()
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopRecord()
    }
}
