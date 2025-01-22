package com.idrnd.idvoice.utils.extensions

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.idrnd.idvoice.R

/**
 * Replace an existing fragment with new one.
 *
 * @param fragmentClass The Fragment class to instantiate.
 * @param addToBackStack add this transaction to the back stack.
 * @param bundle Bundle to add use as arguments for fragment instance.
 */
fun Fragment.replaceWithFragment(fragmentClass: Class<out Fragment>, addToBackStack: Boolean, bundle: Bundle? = null) {
    val transaction = parentFragmentManager
        .beginTransaction()
        .setCustomAnimations(R.anim.fade_out, R.anim.fade_in, R.anim.fade_out, R.anim.fade_in)
        .replace(R.id.mainActivityContainer, fragmentClass, bundle, null)

    if (addToBackStack) {
        transaction.addToBackStack(null)
    }

    transaction.commit()
}
