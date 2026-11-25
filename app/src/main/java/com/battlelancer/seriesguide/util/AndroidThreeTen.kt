// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original version from Jake Wharton https://github.com/JakeWharton/ThreeTenABP

package com.battlelancer.seriesguide.util

import android.content.Context
import org.threeten.bp.zone.ZoneRulesInitializer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android-specific initializer for the ThreeTen-Backport library.
 */
object AndroidThreeTen {

    private val initialized = AtomicBoolean()

    fun init(context: Context) {
        if (!initialized.getAndSet(true)) {
            ZoneRulesInitializer.setInitializer(
                AssetsZoneRulesInitializer(
                    context,
                    "org/threeten/bp/TZDB.dat"
                )
            )
        }
    }

}
