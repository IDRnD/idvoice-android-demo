package com.idrnd.idvoice.fragments.verify

import android.os.Bundle
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.idrnd.idvoice.R
import com.idrnd.idvoice.utils.extensions.replaceWithFragment
import com.idrnd.idvoice.utils.views.RecordAndProcessPhraseView
import com.idrnd.idvoice.utils.views.RecordAndProcessPhraseView.State.Record
import com.idrnd.idvoice.viewModels.SharedViewModel
import com.idrnd.idvoice.viewModels.verify.VerifierViewModel

class VerifierFragment : Fragment(R.layout.verifier_fragment) {

    private lateinit var backButton: Button
    private lateinit var recordAndProcessPhraseView: RecordAndProcessPhraseView

    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val viewModel: VerifierViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!viewModel.isInitialized) {
            viewModel.init(
                requireActivity(),
                sharedViewModel.speechSummaryEngine,
                sharedViewModel.voiceTemplateMatcher,
                sharedViewModel.voiceTemplateFactory,
                sharedViewModel.antispoofEngine
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get views
        backButton = view.findViewById(R.id.myVerificationBackButton)
        recordAndProcessPhraseView = view.findViewById(R.id.recordAndProcessPhraseView)

        // Set listeners/observers
        viewModel.needResetVisualization.observe(viewLifecycleOwner) { reset ->
            reset ?: return@observe

            if (reset) {
                recordAndProcessPhraseView.resetVisualization()
            }
        }

        viewModel.dataForVisualization.observe(viewLifecycleOwner) { data ->
            data ?: return@observe

            recordAndProcessPhraseView.visualizeData(data)
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            state ?: return@observe

            // To animate view changes
            TransitionManager.beginDelayedTransition(requireView() as ViewGroup)

            recordAndProcessPhraseView.state = state
            backButton.isVisible = (state == Record)
        }

        viewModel.phraseForPronouncing.observe(viewLifecycleOwner) { phrase ->
            phrase ?: return@observe

            recordAndProcessPhraseView.phraseForPronouncing = phrase
        }

        viewModel.messageAboutPhraseForPronouncing.observe(viewLifecycleOwner) { message ->
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
