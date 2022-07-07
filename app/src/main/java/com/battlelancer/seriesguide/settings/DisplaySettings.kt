package com.battlelancer.seriesguide.settings

import android.content.Context
import android.util.DisplayMetrics
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.util.TextTools

/**
 * Settings related to appearance, display formats and sort orders.
 */
object DisplaySettings {

    const val KEY_THEME = "com.uwetrottmann.seriesguide.theme"
    const val KEY_DYNAMIC_COLOR = "com.uwetrottmann.seriesguide.dynamiccolor"

    const val KEY_NUMBERFORMAT = "numberformat"
    const val KEY_SHOWS_TIME_OFFSET = "com.battlelancer.seriesguide.timeoffset"
    const val KEY_NO_RELEASED_EPISODES = "onlyFutureEpisodes"
    const val KEY_HIDE_SPECIALS = "onlySeasonEpisodes"
    const val KEY_SORT_IGNORE_ARTICLE = "com.battlelancer.seriesguide.sort.ignorearticle"
    const val KEY_DISPLAY_EXACT_DATE = "com.battlelancer.seriesguide.shows.exactdate"
    const val KEY_PREVENT_SPOILERS = "com.battlelancer.seriesguide.PREVENT_SPOILERS"

    /**
     * Returns true for all screens with dpi higher than [DisplayMetrics.DENSITY_HIGH].
     */
    @JvmStatic
    fun isVeryHighDensityScreen(context: Context): Boolean {
        return context.resources.displayMetrics.densityDpi > DisplayMetrics.DENSITY_HIGH
    }

    fun getThemeIndex(context: Context): String {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_THEME, null) ?: "0"
    }

    fun isDynamicColorsEnabled(context: Context): Boolean {
        // Default to false to keep our own brand colors.
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_DYNAMIC_COLOR, false)
    }

    @JvmStatic
    fun getNumberFormat(context: Context): String {
        val formatOrNull = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_NUMBERFORMAT, TextTools.EpisodeFormat.DEFAULT.value)
        return formatOrNull ?: TextTools.EpisodeFormat.DEFAULT.value
    }

    /**
     * @return A positive or negative number of hours to offset show release times by.
     * Defaults to 0.
     */
    @JvmStatic
    fun getShowsTimeOffset(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_SHOWS_TIME_OFFSET, null)?.toIntOrNull()
            ?: 0
    }

    @JvmStatic
    fun isNoReleasedEpisodes(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_NO_RELEASED_EPISODES, false)
    }

    fun setNoReleasedEpisodes(context: Context, value: Boolean) {
        return PreferenceManager.getDefaultSharedPreferences(context).edit {
            putBoolean(KEY_NO_RELEASED_EPISODES, value)
        }
    }

    /**
     * Whether to exclude special episodes wherever possible (except in the actual seasons and
     * episode lists of a show).
     */
    @JvmStatic
    fun isHidingSpecials(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_HIDE_SPECIALS, false)
    }

    /**
     * Whether shows and movies sorted by title should ignore the leading article.
     */
    @JvmStatic
    fun isSortOrderIgnoringArticles(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_SORT_IGNORE_ARTICLE, false)
    }

    /**
     * Whether to show the exact/absolute date (31.10.2010) instead of a relative time string (in 5
     * days).
     */
    @JvmStatic
    fun isDisplayExactDate(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_DISPLAY_EXACT_DATE, false)
    }

    /**
     * Whether the app should hide details potentially spoiling an unwatched episode.
     */
    @JvmStatic
    fun preventSpoilers(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_PREVENT_SPOILERS, false)
    }
}