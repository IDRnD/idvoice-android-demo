package com.idrnd.idvoice.fragments.enroll.textDependent

import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.transition.TransitionManager
import com.idrnd.idvoice.R
import com.idrnd.idvoice.fragments.StartFragment
import com.idrnd.idvoice.utils.extensions.replaceWithFragment
import com.idrnd.idvoice.utils.views.PhraseEnrollmentView
import com.idrnd.idvoice.utils.views.PhraseEnrollmentView.State.*
import com.idrnd.idvoice.viewModels.SharedViewModel
import com.idrnd.idvoice.viewModels.enroll.textDependent.PhraseEnrollerViewModel
import kotlinx.coroutines.*
import java.util.*

class PhraseEnrollerFragment : Fragment(R.layout.phrase_enroller_fragmnet) {

    private lateinit var phraseEnrollmentView: PhraseEnrollmentView
    private lateinit var backButton: Button

    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val viewModel: PhraseEnrollerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!viewModel.isInitialized) {
            viewModel.init(
                requireContext(),
                sharedViewModel.voiceTemplateFactory,
                sharedViewModel.speechSummaryEngine,
                sharedViewModel.templateFileCreator
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get views
        phraseEnrollmentView = view.findViewById(R.id.phraseEnrollmentView)
        backButton = view.findViewById(R.id.myTextDependentEnrollBackButton)

        // Set listeners/observers
        viewModel.warningMessageForUser.observe(viewLifecycleOwner) { message ->
            message ?: return@observe

            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        viewModel.needResetVisualization.observe(viewLifecycleOwner) { resetVisualization ->
            resetVisualization ?: return@observe

            if (resetVisualization) {
                phraseEnrollmentView.resetVisualization()
            }
        }

        viewModel.dataForVisualization.observe(viewLifecycleOwner) { data ->
            data ?: return@observe

            phraseEnrollmentView.visualizeData(data)
        }

        viewModel.numberRecordedPhrases.observe(viewLifecycleOwner) { number ->
            number ?: return@observe

            for (i in 0 until number) {
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

            if (state == Process) {
                // Hide button
                backButton.visibility = GONE
            }

            if (state == ProcessIsFinished) {

                // Clear back stack
                parentFragmentManager.popBackStack(null, POP_BACK_STACK_INCLUSIVE)

                // Go to start fragment
                replaceWithFragment(StartFragment(), false)
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
        for (i in 0 until 3) {
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
}
