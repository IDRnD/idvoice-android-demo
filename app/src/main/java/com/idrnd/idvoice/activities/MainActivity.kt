package com.idrnd.idvoice.activities

import android.content.Context
import android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.idrnd.idvoice.R
import com.idrnd.idvoice.useCaseSelector.UseCaseSelectorFragment
import com.idrnd.idvoice.utils.extensions.enableContrastColorModeForStatusAndNavigationBars
import com.idrnd.idvoice.utils.extensions.getBaseContextWithFontScale
import com.idrnd.idvoice.utils.extensions.setStatusAndNavigationBarsBackgroundColor

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}
