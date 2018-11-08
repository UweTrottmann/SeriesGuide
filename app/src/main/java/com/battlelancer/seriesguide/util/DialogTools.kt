@file:JvmName("DialogTools")

package com.battlelancer.seriesguide.util

import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction

/**
 * If [FragmentManager.isStateSaved] returns `false`, otherwise shows the dialog and returns `true`.
 */
fun DialogFragment.safeShow(fragmentManager: FragmentManager, tag: String): Boolean {
    if (fragmentManager.isStateSaved) {
        return false
    }
    show(fragmentManager, tag)
    return true
}

/**
 * If [FragmentManager.isStateSaved] returns `false`, otherwise shows the dialog and returns `true`.
 */
fun DialogFragment.safeShow(fragmentManager: FragmentManager,
        fragmentTransaction: FragmentTransaction, tag: String): Boolean {
    if (fragmentManager.isStateSaved) {
        return false
    }
    show(fragmentTransaction, tag)
    return true
}
