package com.idrnd.idvoice.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.idrnd.idvoice.MainApplication
import com.idrnd.idvoice.R
import com.idrnd.idvoice.useCaseSelector.UseCaseSelectorFragment
import com.idrnd.idvoice.utils.extensions.enableContrastColorModeForStatusAndNavigationBars
import com.idrnd.idvoice.utils.extensions.getBaseContextWithFontScale
import com.idrnd.idvoice.utils.extensions.setStatusAndNavigationBarsBackgroundColor
import com.idrnd.idvoice.utils.license.LicenseStatus
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when ((application as MainApplication).voiceSdkLicense.licenseStatus) {
            LicenseStatus.Invalid -> {
                showDialogAboutInvalidLicense()
                return
            }
            LicenseStatus.Expired -> {
                showDialogAboutExpiredLicense()
                return
            }
            else -> {
                // Nothing
            }
        }

        setContentView(R.layout.main_activity)

        // Replace app theme for splash screen to simple app theme
        setTheme(R.style.IDVoice_AppTheme)

        // Keep screen on
        window.addFlags(FLAG_KEEP_SCREEN_ON)

        // Change navigation bar and background colors to the same
        window.setStatusAndNavigationBarsBackgroundColor(ContextCompat.getColor(this, R.color.black_haze))
        window.enableContrastColorModeForStatusAndNavigationBars()

        // Launch a start fragment
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.mainActivityContainer, UseCaseSelectorFragment())
            .commit()
    }

    override fun attachBaseContext(newBase: Context?) {
        // Make font scale fixed and independent of phone setting
        super.attachBaseContext(newBase?.getBaseContextWithFontScale(1.0f))
    }

    private fun showDialogAboutInvalidLicense() {
        showDialogAboutLicense(R.string.your_voicesdk_license_is_invalid)
    }

    private fun showDialogAboutExpiredLicense() {
        showDialogAboutLicense(R.string.your_voicesdk_license_has_expired)
    }

    private fun showDialogAboutLicense(@StringRes message: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.license_issue))
            .setMessage(getString(message))
            .setPositiveButton(getString(R.string.renew_license)) { _, _ ->
                // Open web site
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(IDRND_CONTACT_US_URL)))
                // Exit from the app
                exitProcess(0)
            }
            .setCancelable(false)
            .show()
    }

    companion object {
        private const val IDRND_CONTACT_US_URL = "https://www.idrnd.ai/contact-us"
    }
}
