package com.battlelancer.seriesguide.util

import android.app.Activity
import android.content.Context
import androidx.annotation.AttrRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences
import com.google.android.material.color.DynamicColors
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.seriesguide.widgets.SlidingTabLayout

/**
 * Helper methods to support SeriesGuide's different themes.
 */
object ThemeUtils {

    /**
     * Sets the global app theme variable. Applied by all activities once they are created.
     */
    @Synchronized
    fun updateTheme(themeIndex: String) {
        SeriesGuidePreferences.THEME = R.style.Theme_SeriesGuide_DayNight
        when (themeIndex.toIntOrNull() ?: 0) {
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else ->
                // Defaults as recommended by https://medium.com/androiddevelopers/appcompat-v23-2-daynight-d10f90c83e94
                if (AndroidUtils.isAtLeastQ) {
                    AppCompatDelegate
                        .setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                } else {
                    AppCompatDelegate
                        .setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                }
        }
    }

    @JvmStatic
    fun getColorFromAttribute(context: Context, @AttrRes attribute: Int): Int {
        return ContextCompat.getColor(
            context,
            Utils.resolveAttributeToResourceId(context.theme, attribute)
        )
    }

    @JvmStatic
    fun SlidingTabLayout.setDefaultStyle() {
        setCustomTabView(R.layout.tabstrip_item_transparent, R.id.textViewTabStripItem)
        setSelectedIndicatorColors(getColorFromAttribute(context, R.attr.colorPrimary))
        setUnderlineColor(getColorFromAttribute(context, R.attr.sgColorDivider))
    }

    /**
     * If [DisplaySettings.isDynamicColorsEnabled] applies Material 3 Dynamic Colors theme overlay
     * if available on the device.
     * https://m3.material.io/libraries/mdc-android/color-theming
     */
    fun setTheme(activity: Activity, themeResId: Int = SeriesGuidePreferences.THEME) {
        activity.setTheme(themeResId)
        if (DisplaySettings.isDynamicColorsEnabled(activity)) {
            DynamicColors.applyIfAvailable(activity)
        }
    }
}