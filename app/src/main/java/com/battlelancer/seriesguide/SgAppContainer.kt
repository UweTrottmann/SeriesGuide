// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide

import android.content.Context
import com.battlelancer.seriesguide.diagnostics.DebugLogBuffer
import com.battlelancer.seriesguide.util.PackageTools
import timber.log.Timber

class SgAppContainer(context: Context) {

    val debugLogBuffer by lazy { DebugLogBuffer(context) }

    /**
     * If true, should not display links to third-party websites that in any way link to a website
     * that accepts payments.
     */
    val preventExternalLinks by lazy {
        val installedByPlay = PackageTools.wasInstalledByPlayStore(context)
        val isDeviceInEEA = PackageTools.isDeviceInEEA(context)
        (installedByPlay && !isDeviceInEEA)
            .also { Timber.d("preventExternalLinks = %s", it) }
//            .let { if (BuildConfig.DEBUG) true else it }
    }

}
