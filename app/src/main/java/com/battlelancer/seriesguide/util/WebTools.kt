// SPDX-License-Identifier: Apache-2.0
// Copyright 2023-2025 Uwe Trottmann

package com.battlelancer.seriesguide.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.WebTools.openInApp
import com.battlelancer.seriesguide.util.WebTools.openInCustomTab

object WebTools {

    /**
     * Opens in a Custom Tab if a supporting browser is installed.
     * Otherwise automatically falls back to opening a full browser.
     *
     * Only use if absolutely necessary. Custom Tabs often have limited features: no ability to
     * auto-translate, to bookmark, or others.
     *
     * Returns false (and shows an error toast) if there is no app available to handle the view
     * intent, see [tryStartActivity].
     *
     * See also [openInApp].
     */
    fun openInCustomTab(context: Context, url: String): Boolean {
        val darkParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(
                ContextCompat.getColor(context, R.color.md_theme_dark_surfaceContainer)
            )
            .build()
        val defaultParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(
                ContextCompat.getColor(context, R.color.md_theme_light_surfaceContainer)
            )
            .build()
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setColorScheme(CustomTabsIntent.COLOR_SCHEME_SYSTEM)
            .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, darkParams)
            .setDefaultColorSchemeParams(defaultParams)
            .build().intent
            .apply { data = Uri.parse(url) }
        return context.tryStartActivity(customTabsIntent, true)
    }

    /**
     * Opens in a supporting app, typically a browser or an app registered for a deep link, if one
     * is installed.
     *
     * Returns false (and shows an error toast) if there is no app available to handle the view
     * intent, see [tryStartActivity].
     *
     * See also [openInCustomTab].
     */
    fun openInApp(context: Context, url: String): Boolean {
        return context.tryStartActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)),
            true
        )
    }

}