// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide

import android.content.Context
import com.battlelancer.seriesguide.billing.BillingRepository
import com.battlelancer.seriesguide.diagnostics.DebugLogBuffer
import com.battlelancer.seriesguide.util.PackageTools
import com.battlelancer.seriesguide.util.PackageTools.isEuropeanEconomicArea
import com.battlelancer.seriesguide.util.PackageTools.isUnitedStates
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber

class SgAppContainer(context: Context, coroutineScope: CoroutineScope) {

    val debugLogBuffer by lazy { DebugLogBuffer(context) }

    /**
     * If true, should not display links to third-party websites that in any way link to a website
     * that accepts payments.
     */
    val preventExternalLinks by lazy {
        val installedByPlay = PackageTools.wasInstalledByPlayStore(context)
        val region = PackageTools.getDeviceRegion(context)
        val isEEA = region.isEuropeanEconomicArea(context)
        val isUS = region.isUnitedStates()
        (installedByPlay && !isEEA && !isUS)
            .also {
                Timber.i(
                    "preventExternalLinks=%s installedByPlay=%s region=%s isEEA=%s isUS=%s",
                    installedByPlay,
                    it,
                    region.code,
                    isEEA,
                    isUS
                )
            }
//            .let { if (BuildConfig.DEBUG) true else it }
    }

    val billingRepository by lazy {
        BillingRepository(context, coroutineScope)
    }
}
