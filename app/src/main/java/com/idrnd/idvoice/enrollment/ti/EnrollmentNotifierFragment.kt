package com.idrnd.idvoice.enrollment.ti

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.idrnd.idvoice.R
import com.idrnd.idvoice.utils.extensions.replaceWithFragment

class EnrollmentNotifierFragment : Fragment(R.layout.enrollment_notifier_fragment) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get views
        val backButton = view.findViewById<Button>(R.id.noticeOfTiEnrollBackButton)
        val recordButton = view.findViewById<Button>(R.id.startRecordButton)

        // Set listeners/observers
        backButton.setOnClickListener {
            it.isClickable = false
            requireActivity().onBackPressed()
        }

        recordButton.setOnClickListener {
            // Go to enroller fragment
            replaceWithFragment(EnrollerFragment::class.java, true)
        }
    }
}
