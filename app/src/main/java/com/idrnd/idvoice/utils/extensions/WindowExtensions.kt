package com.idrnd.idvoice.utils.extensions

import android.os.Build
import android.view.View
import android.view.Window

/**
 * Function that sets a background color to status and navigation bars.
 * @param color new background color.
 */
fun Window.setStatusAndNavigationBarsBackgroundColor(color: Int) {
    statusBarColor = color
    navigationBarColor = color
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        navigationBarDividerColor = color
    }
}

/**
 * Function that enables a contrast color mode in status and navigation bars.
 */
fun Window.enableContrastColorModeForStatusAndNavigationBars() {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }
}
