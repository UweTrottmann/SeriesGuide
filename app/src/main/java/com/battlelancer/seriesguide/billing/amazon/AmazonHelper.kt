// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.billing.amazon

import android.content.Context

object AmazonHelper {

    private var iapManagerInstance: AmazonIapManagerInterface? = null

    /**
     * This is only available after calling [create].
     */
    @JvmStatic
    val iapManager: AmazonIapManagerInterface
        get() = iapManagerInstance!!

    @JvmStatic
    @Synchronized
    fun create(context: Context) {
        if (iapManagerInstance == null) {
            iapManagerInstance = AmazonIapManager(context)
        }
    }
}
