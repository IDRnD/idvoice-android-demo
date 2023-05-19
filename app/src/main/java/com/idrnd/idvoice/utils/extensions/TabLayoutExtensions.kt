package com.idrnd.idvoice.utils.extensions

import com.google.android.material.tabs.TabLayout

/**
 * Add a TabLayout.OnTabSelectedListener that will be invoked when tab selection changes.
 *
 * <p>Components that add a listener should take care to remove it when finished via {@link
 * #removeOnTabSelectedListener(OnTabSelectedListener)}.
 *
 */
inline fun TabLayout?.addOnTabSelectedListener(
    crossinline onTabReselected: (tab: TabLayout.Tab?) -> Unit = { },
    crossinline onTabUnselected: (tab: TabLayout.Tab?) -> Unit = { },
    crossinline onTabSelected: (tab: TabLayout.Tab?) -> Unit = { },
) {
    this?.addOnTabSelectedListener(
        object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) = onTabReselected(tab)
            override fun onTabUnselected(tab: TabLayout.Tab?) = onTabUnselected(tab)
            override fun onTabSelected(tab: TabLayout.Tab?) = onTabSelected(tab)
        },
    )
}
