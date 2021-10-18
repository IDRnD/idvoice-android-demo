package com.idrnd.idvoice.utils.extensions

import androidx.fragment.app.Fragment
import com.idrnd.idvoice.R

/**
 * Replace an existing fragment with new one.
 *
 * @param fragment new fragment.
 * @param addToBackStack add this transaction to the back stack.
 */
fun Fragment.replaceWithFragment(fragment: Fragment, addToBackStack: Boolean) {

    val transaction = parentFragmentManager
        .beginTransaction()
        .setCustomAnimations(R.anim.fade_out, R.anim.fade_in, R.anim.fade_out, R.anim.fade_in)
        .replace(R.id.mainActivityContainer, fragment)

    if (addToBackStack) {
        transaction.addToBackStack(null)
    }

    transaction.commit()
}
