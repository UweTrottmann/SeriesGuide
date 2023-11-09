// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.battlelancer.seriesguide.R

object WebTools {

    /**
     * Opens in a Custom Tab if a supporting browser is installed.
     * Otherwise automatically falls back to opening a full browser.
     *
     * Returns false (and shows an error toast) if there is no app available to handle the view
     * intent, see [Utils.tryStartActivity].
     *
     * See also [openInApp].
     */
    @JvmStatic
    fun openInCustomTab(context: Context, url: String): Boolean {
        val darkParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(
                ContextCompat.getColor(context, R.color.sg_background_app_bar_dark)
            )
            .build()
        val defaultParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(
                ContextCompat.getColor(context, R.color.sg_color_background_light)
            )
            .build()
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setColorScheme(CustomTabsIntent.COLOR_SCHEME_SYSTEM)
            .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, darkParams)
            .setDefaultColorSchemeParams(defaultParams)
            .build().intent
            .apply { data = Uri.parse(url) }
        return Utils.tryStartActivity(context, customTabsIntent, true)
    }

    /**
     * Opens in a supporting app, typically a browser or an app registered for a deep link, if one
     * is installed.
     *
     * Returns false (and shows an error toast) if there is no app available to handle the view
     * intent, see [Utils.tryStartActivity].
     *
     * See also [openInCustomTab].
     */
    fun openInApp(context: Context, url: String): Boolean {
        return Utils.tryStartActivity(
            context,
            Intent(Intent.ACTION_VIEW, Uri.parse(url)),
            true
        )
    }

}