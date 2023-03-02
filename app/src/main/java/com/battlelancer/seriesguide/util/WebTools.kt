package com.battlelancer.seriesguide.util

import android.content.Context
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
     */
    @JvmStatic
    fun openAsCustomTab(context: Context, url: String): Boolean {
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

}