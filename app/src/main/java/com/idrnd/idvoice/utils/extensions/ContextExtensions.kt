package com.idrnd.idvoice.utils.extensions

import android.content.Context
import android.content.res.Configuration

/**
 * This function should be called in the attachBaseContext function, which is located in Activity,
 * in order to scale the font the application.
 * An example is below:
 * ```
 * @KotlinAnnotation
 * override fun attachBaseContext(newBase: Context?) {
 *     super.attachBaseContext(newBase?.getBaseContextWithFontScale(1.0f))
 * }
 * ```
 * @param fontScale wish value of font scale
 * @return new base context
 */
fun Context.getBaseContextWithFontScale(fontScale: Float): Context {
    val overrideConfiguration: Configuration = this.resources.configuration
    overrideConfiguration.fontScale = fontScale
    return this.createConfigurationContext(overrideConfiguration)
}
