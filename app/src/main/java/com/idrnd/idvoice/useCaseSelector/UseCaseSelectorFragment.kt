package com.idrnd.idvoice.useCaseSelector

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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.transition.TransitionManager
import com.google.android.material.tabs.TabLayout
import com.idrnd.idvoice.R
import com.idrnd.idvoice.preferences.GlobalPrefs
import com.idrnd.idvoice.useCaseSelector.UseCaseSelectorViewModel.Companion.UseCaseSelectorViewModelFactory
import com.idrnd.idvoice.utils.extensions.addOnTabSelectedListener
import com.idrnd.idvoice.utils.extensions.replaceWithFragment
import net.idrnd.voicesdk.core.BuildInfo

class UseCaseSelectorFragment : Fragment(R.layout.use_case_selector_fragment) {

    private lateinit var biometricAnalysisTypeTabs: TabLayout
    private lateinit var biometricsSelectorContainer: ViewGroup
    private lateinit var enrollButton: Button
    private lateinit var verifyButton: View
    private lateinit var voiceSdkVersion: TextView

    private val viewModel: UseCaseSelectorViewModel by viewModels { UseCaseSelectorViewModelFactory }

    private val enrollPermissionRequester =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.onEnrollButtonClick()
            } else {
                viewModel.postWarningMessageAboutPermissions()
            }
        }

    private val verifyPermissionRequester =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.onVerifyButtonClick()
            } else {
                viewModel.postWarningMessageAboutPermissions()
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
            },
        )

        enrollButton.setOnClickListener {
            enrollButton.isClickable = false

            // Check self permission and request it
            if (ContextCompat.checkSelfPermission(requireContext(), RECORD_AUDIO) != PERMISSION_GRANTED) {
                enrollPermissionRequester.launch(RECORD_AUDIO)
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
                verifyPermissionRequester.launch(RECORD_AUDIO)
                verifyButton.isClickable = true
                return@setOnClickListener
            }

            // If the permission is granted then call a view model function
            viewModel.onVerifyButtonClick()

            verifyButton.isClickable = true
        }

        // Set a voice sdk version on the screen
        voiceSdkVersion.text = "VoiceSDK ${BuildInfo.get().version}"

        // Subscribe on view model
        viewModel.messageId.observe(viewLifecycleOwner) { messageId ->
            messageId ?: return@observe

            Toast.makeText(requireContext(), messageId, LENGTH_LONG).show()
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
    }

    override fun onStart() {
        super.onStart()
        // Select valid biometrics tab
        biometricAnalysisTypeTabs.getTabAt(GlobalPrefs.biometricsType.ordinal)?.select()
    }

    companion object {
        private val TAG = UseCaseSelectorFragment::class.simpleName
    }
}
