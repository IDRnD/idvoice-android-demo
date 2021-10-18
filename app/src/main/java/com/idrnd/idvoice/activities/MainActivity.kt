package com.idrnd.idvoice.activities

import android.content.Context
import android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.idrnd.idvoice.R
import com.idrnd.idvoice.fragments.StartFragment
import com.idrnd.idvoice.model.GlobalPrefs
import com.idrnd.idvoice.utils.extensions.enableContrastColorModeForStatusAndNavigationBars
import com.idrnd.idvoice.utils.extensions.getBaseContextWithFontScale
import com.idrnd.idvoice.utils.extensions.setStatusAndNavigationBarsBackgroundColor
import com.idrnd.idvoice.viewModels.SharedViewModel

class MainActivity : AppCompatActivity() {

    // View model that shares data between fragments
    private val sharedViewModel: SharedViewModel by viewModels()

    // Permission handlers
    lateinit var enrollRequestPermission: ActivityResultLauncher<String>
    lateinit var verifyRequestPermission: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {

        // Init shared view model before onCreate is called
        if (!sharedViewModel.isInitialized) {
            sharedViewModel.init(this)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        // Replace app theme for splash screen to simple app theme
        setTheme(R.style.IDVoice_AppTheme)

        // Keep screen on
        window.addFlags(FLAG_KEEP_SCREEN_ON)

        // Change navigation bar and background colors to the same
        window.setStatusAndNavigationBarsBackgroundColor(ContextCompat.getColor(this, R.color.black_haze))
        window.enableContrastColorModeForStatusAndNavigationBars()

        // Init permission handlers
        enrollRequestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // Post a result of permission request to all fragments
            sharedViewModel.onEnrollPermissionsAreGranted?.invoke(isGranted)
        }

        verifyRequestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // Post a result of permission request to all fragments
            sharedViewModel.onVerifyPermissionsAreGranted?.invoke(isGranted)
        }

        // Launch a start fragment
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.mainActivityContainer, StartFragment())
            .commit()
    }

    override fun attachBaseContext(newBase: Context?) {
        // Make font scale fixed and independent of phone setting
        super.attachBaseContext(newBase?.getBaseContextWithFontScale(1.0f))
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear resources
        enrollRequestPermission.unregister()
        verifyRequestPermission.unregister()
    }
}
