// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

@file:JvmName("DialogTools")

package com.battlelancer.seriesguide.util

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

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

@Suppress("UNCHECKED_CAST")
fun <T : DialogFragment> findDialog(fragmentManager: FragmentManager, tag: String): T? {
    return fragmentManager.findFragmentByTag(tag) as T?
}
