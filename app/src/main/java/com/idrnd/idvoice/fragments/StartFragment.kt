package com.idrnd.idvoice.fragments

import android.Manifest.permission.RECORD_AUDIO
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.transition.TransitionManager
import com.google.android.material.tabs.TabLayout
import com.idrnd.idvoice.R
import com.idrnd.idvoice.activities.MainActivity
import com.idrnd.idvoice.model.GlobalPrefs
import com.idrnd.idvoice.utils.extensions.addOnTabSelectedListener
import com.idrnd.idvoice.utils.extensions.replaceWithFragment
import com.idrnd.idvoice.viewModels.SharedViewModel
import com.idrnd.idvoice.viewModels.StartViewModel
import net.idrnd.voicesdk.BuildConfig

class StartFragment : Fragment(R.layout.start_fragment) {

    private lateinit var biometricAnalysisTypeTabs: TabLayout
    private lateinit var biometricsSelectorContainer: ViewGroup
    private lateinit var enrollButton: Button
    private lateinit var verifyButton: View
    private lateinit var voiceSdkVersion: TextView

    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val viewModel: StartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init a view model
        if (!viewModel.isInitialized) {
            viewModel.init(requireContext())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get views
        biometricAnalysisTypeTabs = view.findViewById(R.id.biometricAnalysisTypeTabs)
        biometricsSelectorContainer = view.findViewById(R.id.biometricsSelectorContainer)
        enrollButton = view.findViewById(R.id.enrollButton)
        verifyButton = view.findViewById(R.id.verifyButton)
        voiceSdkVersion = view.findViewById(R.id.voiceSdkVersion)

        // Set listeners on views
        biometricAnalysisTypeTabs.addOnTabSelectedListener(
            onTabSelected = { biometricAnalysisTab ->
                biometricAnalysisTypeTabs.isEnabled = false

                if (biometricAnalysisTab == null) {
                    Log.w(TAG, "User selected an biometric analysis tab that is null.")
                    biometricAnalysisTypeTabs.isEnabled = true
                    return@addOnTabSelectedListener
                }

                viewModel.onBiometricsTypeTabClick(biometricAnalysisTab.position)

                biometricAnalysisTypeTabs.isEnabled = true
            }
        )

        enrollButton.setOnClickListener {
            enrollButton.isClickable = false

            // Check self permission and request it
            if (ContextCompat.checkSelfPermission(requireContext(), RECORD_AUDIO) != PERMISSION_GRANTED) {
                (requireActivity() as MainActivity).enrollRequestPermission.launch(RECORD_AUDIO)
                enrollButton.isClickable = true
                return@setOnClickListener
            }

            // If the permission is granted then call a view model function
            viewModel.onEnrollButtonClick()

            enrollButton.isClickable = true
        }

        verifyButton.setOnClickListener {
            verifyButton.isClickable = false

            // Check self permission and request it
            if (ContextCompat.checkSelfPermission(requireContext(), RECORD_AUDIO) != PERMISSION_GRANTED) {
                (requireActivity() as MainActivity).verifyRequestPermission.launch(RECORD_AUDIO)
                verifyButton.isClickable = true
                return@setOnClickListener
            }

            // If the permission is granted then call a view model function
            viewModel.onVerifyButtonClick()

            verifyButton.isClickable = true
        }

        // Set a voice sdk version on the screen
        voiceSdkVersion.text = "VoiceSDK ${BuildConfig.VERSION_NAME}"

        // Subscribe on view model
        viewModel.warningMessageForUser.observe(viewLifecycleOwner) { message ->
            message ?: return@observe

            Toast.makeText(requireContext(), message, LENGTH_LONG).show()
        }

        viewModel.enrollmentButtonTitle.observe(viewLifecycleOwner) { title ->
            title ?: return@observe

            // To animate view changes
            TransitionManager.beginDelayedTransition(biometricsSelectorContainer)

            enrollButton.text = title
        }

        viewModel.isVerificationButtonEnabled.observe(viewLifecycleOwner) { isEnabled ->
            isEnabled ?: return@observe

            // To animate view changes
            TransitionManager.beginDelayedTransition(biometricsSelectorContainer)

            verifyButton.isEnabled = isEnabled
        }

        viewModel.onFragmentToLaunch.observe(viewLifecycleOwner) { fragment ->
            replaceWithFragment(fragment, true)
        }

        // Subscribe on shared view model
        sharedViewModel.onEnrollPermissionsAreGranted = { areGranted ->
            if (areGranted) {
                viewModel.onEnrollButtonClick()
            } else {
                viewModel.postWarningMessageAboutPermissions()
            }
        }

        sharedViewModel.onVerifyPermissionsAreGranted = { areGranted ->
            if (areGranted) {
                viewModel.onVerifyButtonClick()
            } else {
                viewModel.postWarningMessageAboutPermissions()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Select valid biometrics tab
        biometricAnalysisTypeTabs.getTabAt(GlobalPrefs.biometricsType.ordinal)?.select()
    }

    companion object {
        private val TAG = StartFragment::class.simpleName
    }
}
