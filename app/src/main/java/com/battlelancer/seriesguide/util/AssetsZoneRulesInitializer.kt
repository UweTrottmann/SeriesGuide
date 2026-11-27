// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.util

import android.content.Context
import org.threeten.bp.zone.TzdbZoneRulesProvider
import org.threeten.bp.zone.ZoneRulesInitializer
import org.threeten.bp.zone.ZoneRulesProvider
import java.io.IOException

internal class AssetsZoneRulesInitializer(
    private val context: Context,
    private val assetPath: String
) : ZoneRulesInitializer() {

    override fun initializeProviders() {
        val provider = try {
            context.assets.open(assetPath).use { TzdbZoneRulesProvider(it) }
        } catch (e: IOException) {
            throw IllegalStateException("$assetPath missing from assets", e)
        }
        ZoneRulesProvider.registerProvider(provider)
    }

}
