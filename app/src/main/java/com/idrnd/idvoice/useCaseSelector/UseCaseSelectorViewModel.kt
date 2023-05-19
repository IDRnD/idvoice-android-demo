package com.idrnd.idvoice.useCaseSelector

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hadilq.liveevent.LiveEvent
import com.idrnd.idvoice.R
import com.idrnd.idvoice.enrollment.td.PhraseEnrollerFragment
import com.idrnd.idvoice.enrollment.ti.EnrollmentNotifierFragment
import com.idrnd.idvoice.preferences.BiometricsType
import com.idrnd.idvoice.preferences.BiometricsType.TextDependent
import com.idrnd.idvoice.preferences.BiometricsType.TextIndependent
import com.idrnd.idvoice.preferences.GlobalPrefs
import com.idrnd.idvoice.verification.VerifierFragment
import java.io.File

class UseCaseSelectorViewModel(context: Context) : ViewModel() {

    var onFragmentToLaunch = LiveEvent<Fragment>()

    val enrollmentButtonTitle = MutableLiveData<String>()
    val isVerificationButtonEnabled = MutableLiveData<Boolean>()
    val messageId = LiveEvent<Int>()

    private var templateExistenceToEnrollmentButtonTitle: Map<Boolean, String>

    init {
        // Get enrollment titles
        templateExistenceToEnrollmentButtonTitle = mapOf(
            true to context.getString(R.string.update_enrollment),
            false to context.getString(R.string.enroll),
        )

        // Init ui state by a saved biometrics type
        updateStateByBiometricsType()
    }

    fun postWarningMessageAboutPermissions() {
        messageId.postValue(R.string.need_permissions)
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

    companion object {
        val UseCaseSelectorViewModelFactory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                UseCaseSelectorViewModel(app.applicationContext)
            }
        }
    }
}
