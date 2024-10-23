// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.util

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit

/**
 * Like [commit], but with [FragmentTransaction.setReorderingAllowed] set to `true`.
 */
fun FragmentManager.commitReorderingAllowed(body: FragmentTransaction.() -> Unit) {
    commit {
        // https://developer.android.com/guide/fragments/transactions#reordering
        setReorderingAllowed(true)
        body()
    }
}
