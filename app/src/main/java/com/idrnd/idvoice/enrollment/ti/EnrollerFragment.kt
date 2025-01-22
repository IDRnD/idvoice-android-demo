package com.idrnd.idvoice.enrollment.ti

import android.os.Bundle
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import androidx.fragment.app.viewModels
import com.idrnd.idvoice.R
import com.idrnd.idvoice.enrollment.ti.EnrollerViewModel.Companion.EnrollerViewModelFactory
import com.idrnd.idvoice.useCaseSelector.UseCaseSelectorFragment
import com.idrnd.idvoice.utils.extensions.replaceWithFragment
import com.idrnd.idvoice.utils.views.EnrollerView
import com.idrnd.idvoice.utils.views.EnrollerView.State.ProcessIsFinished
import com.idrnd.idvoice.utils.views.EnrollerView.State.Record

class EnrollerFragment : Fragment(R.layout.ti_enroller_fragment) {

    private val viewModel: EnrollerViewModel by viewModels { EnrollerViewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get views
        val enrollerView = view.findViewById<EnrollerView>(R.id.enrollmentView)
        val backButton = view.findViewById<Button>(R.id.tiEnrollBackButton)

        // Set listeners/observers
        viewModel.messageId.observe(viewLifecycleOwner) { message ->
            message ?: return@observe

            Toast.makeText(requireActivity().applicationContext, message, LENGTH_LONG).show()
        }

        viewModel.enrollmentProgress.observe(viewLifecycleOwner) { progress ->
            enrollerView.setProgress(progress)
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            state ?: return@observe

            // To animate view changes
            TransitionManager.beginDelayedTransition(requireView() as ViewGroup)

            enrollerView.state = state
            backButton.isVisible = (state == Record)

            if (state == ProcessIsFinished) {
                // Clear back stack
                parentFragmentManager.popBackStack(null, POP_BACK_STACK_INCLUSIVE)

                // Go to start fragment
                replaceWithFragment(UseCaseSelectorFragment::class.java, false)
            }
        }

        viewModel.isSpeechRecorded.observe(viewLifecycleOwner) { isSpeechRecorded ->
            isSpeechRecorded ?: return@observe
            enrollerView.visualize()
        }

        backButton.setOnClickListener {
            it.isClickable = false
            requireActivity().onBackPressed()
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
        private val TAG = EnrollerFragment::class.simpleName
    }
}
