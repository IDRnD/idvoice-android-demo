package com.idrnd.idvoice.enrollment.ti

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.idrnd.idvoice.R
import com.idrnd.idvoice.utils.extensions.replaceWithFragment

class EnrollmentNotifierFragment : Fragment(R.layout.enrollment_notifier_fragment) {

    private lateinit var backButton: Button
    private lateinit var recordButton: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get views
        backButton = view.findViewById(R.id.noticeOfTiEnrollBackButton)
        recordButton = view.findViewById(R.id.startRecordButton)

        // Set listeners/observers
        backButton.setOnClickListener {
            it.isClickable = false
            requireActivity().onBackPressed()
        }

        recordButton.setOnClickListener {
            // Go to enroller fragment
            replaceWithFragment(EnrollerFragment(), true)
        }
    }
}
