package com.idrnd.idvoice.viewModels

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hadilq.liveevent.LiveEvent
import com.idrnd.idvoice.R
import com.idrnd.idvoice.fragments.enroll.textDependent.PhraseEnrollerFragment
import com.idrnd.idvoice.fragments.enroll.textIndependent.EnrollmentNotifierFragment
import com.idrnd.idvoice.fragments.verify.VerifierFragment
import com.idrnd.idvoice.model.BiometricsType
import com.idrnd.idvoice.model.BiometricsType.TextDependent
import com.idrnd.idvoice.model.BiometricsType.TextIndependent
import com.idrnd.idvoice.model.GlobalPrefs
import java.io.File

class StartViewModel : ViewModel() {

    /**
     * Indicates whether this view model is initialized or not.
     */
    var isInitialized = false
        private set

    var onFragmentToLaunch = LiveEvent<Fragment>()

    val enrollmentButtonTitle = MutableLiveData<String>()
    val isVerificationButtonEnabled = MutableLiveData<Boolean>()
    val warningMessageForUser = MutableLiveData<String>()

    private lateinit var templateExistenceToEnrollmentButtonTitle: Map<Boolean, String>
    private lateinit var messageAboutPermissions: String

    // Init a view model
    fun init(context: Context) {

        // Get enrollment titles
        templateExistenceToEnrollmentButtonTitle = mapOf(
            true to context.getString(R.string.update_enrollment),
            false to context.getString(R.string.enroll)
        )

        // Init ui state by a saved biometrics type
        updateStateByBiometricsType()

        // Init warning message for user
        messageAboutPermissions = context.getString(R.string.need_permissions)

        // Set that view model is initialized
        isInitialized = true
    }

    fun postWarningMessageAboutPermissions() {
        warningMessageForUser.postValue(messageAboutPermissions)
    }

    fun onEnrollButtonClick() {
        val fragment = when (GlobalPrefs.biometricsType) {
            TextDependent -> PhraseEnrollerFragment()
            TextIndependent -> EnrollmentNotifierFragment()
        }

        onFragmentToLaunch.postValue(fragment)
    }

    fun onVerifyButtonClick() {
        onFragmentToLaunch.postValue(VerifierFragment())
    }

    fun onBiometricsTypeTabClick(indexTab: Int) {
        // Convert an index to a biometrics type
        val biometricsType = BiometricsType.values()[indexTab]

        // Update a biometrics type in the model
        GlobalPrefs.biometricsType = biometricsType

        // Update ui state
        updateStateByBiometricsType()
    }

    private fun updateStateByBiometricsType() {
        // Get a template file
        val templateFilepath = GlobalPrefs.templateFilepath ?: ""

        val templateExists = File(templateFilepath).exists()

        // Enable/disable a verification button by a template file existence
        isVerificationButtonEnabled.postValue(templateExists)

        // Update an enrollment button title
        enrollmentButtonTitle.postValue(templateExistenceToEnrollmentButtonTitle[templateExists])
    }
}
