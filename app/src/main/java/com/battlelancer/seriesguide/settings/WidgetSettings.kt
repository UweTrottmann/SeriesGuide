package com.battlelancer.seriesguide.settings

import android.content.Context
import androidx.core.content.ContextCompat
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.shows.ShowsDistillationSettings

/**
 * Access some widget related settings values.
 */
object WidgetSettings {

    interface Type {
        companion object {
            const val UPCOMING = 0
            const val RECENT = 1
            const val SHOWS = 2
        }
    }

    const val SETTINGS_FILE = "ListWidgetPreferences"
    const val KEY_PREFIX_WIDGET_BACKGROUND_OPACITY = "background_color_"
    const val KEY_PREFIX_WIDGET_THEME = "theme_"
    const val KEY_PREFIX_WIDGET_LISTTYPE = "type_"
    const val KEY_PREFIX_WIDGET_HIDE_WATCHED = "unwatched_"
    const val KEY_PREFIX_WIDGET_ONLY_COLLECTED = "only_collected_"
    const val KEY_PREFIX_WIDGET_ONLY_FAVORITES = "only_favorites_"
    const val KEY_PREFIX_WIDGET_ONLY_PREMIERES = "only_premieres_"
    const val KEY_PREFIX_WIDGET_IS_INFINITE = "is_infinite_"
    const val KEY_PREFIX_WIDGET_IS_LARGE_FONT = "is_largefont_"
    const val KEY_PREFIX_WIDGET_IS_HIDE_WATCH_BUTTON = "is_hide_watch_button_"
    const val KEY_PREFIX_WIDGET_SHOWS_SORT_ORDER = "shows_order_"
    const val DEFAULT_WIDGET_BACKGROUND_OPACITY = "100"
    private const val DEFAULT_WIDGET_BACKGROUND_OPACITY_INT = 100

    /**
     * Returns the type of episodes that the widget should display.
     *
     * @return One of [com.battlelancer.seriesguide.settings.WidgetSettings.Type].
     */
    fun getWidgetListType(context: Context, appWidgetId: Int): Int {
        return context.getSharedPreferences(SETTINGS_FILE, 0)
            .getString(KEY_PREFIX_WIDGET_LISTTYPE + appWidgetId, null)?.toIntOrNull()
            ?: Type.UPCOMING
    }

    /**
     * Returns the sort order of shows. Should be used when the widget is set to the shows type.
     *
     * @return A [ShowsDistillationSettings.ShowsSortOrder]
     * id.
     */
    fun getWidgetShowsSortOrderId(context: Context, appWidgetId: Int): Int {
        val sortOrder = context.getSharedPreferences(SETTINGS_FILE, 0)
            .getString(KEY_PREFIX_WIDGET_SHOWS_SORT_ORDER + appWidgetId, null)?.toIntOrNull()
            ?: context.getString(R.string.widget_default_show_sort_order)
        return when (sortOrder) {
            1 -> ShowsDistillationSettings.ShowsSortOrder.TITLE_ID
            2 -> ShowsDistillationSettings.ShowsSortOrder.OLDEST_EPISODE_ID
            3 -> ShowsDistillationSettings.ShowsSortOrder.LAST_WATCHED_ID
            4 -> ShowsDistillationSettings.ShowsSortOrder.LEAST_REMAINING_EPISODES_ID
            else -> ShowsDistillationSettings.ShowsSortOrder.LATEST_EPISODE_ID
        }
    }

    /**
     * Returns if this widget should not show watched episodes.
     */
    fun isHidingWatchedEpisodes(context: Context, appWidgetId: Int): Boolean {
        val prefs = context.getSharedPreferences(SETTINGS_FILE, 0)
        return prefs.getBoolean(KEY_PREFIX_WIDGET_HIDE_WATCHED + appWidgetId, false)
    }

    /**
     * Returns if this widget should only show collected episodes.
     */
    fun isOnlyCollectedEpisodes(context: Context, appWidgetId: Int): Boolean {
        val prefs = context.getSharedPreferences(SETTINGS_FILE, 0)
        return prefs.getBoolean(KEY_PREFIX_WIDGET_ONLY_COLLECTED + appWidgetId, false)
    }

    /**
     * Returns if this widget should only show episodes of favorited shows.
     */
    fun isOnlyFavoriteShows(context: Context, appWidgetId: Int): Boolean {
        val prefs = context.getSharedPreferences(SETTINGS_FILE, 0)
        return prefs.getBoolean(KEY_PREFIX_WIDGET_ONLY_FAVORITES + appWidgetId, false)
    }

    /**
     * Returns if this widget should only display premieres (first episodes).
     */
    fun isOnlyPremieres(context: Context, appWidgetId: Int): Boolean {
        val prefs = context.getSharedPreferences(SETTINGS_FILE, 0)
        return prefs.getBoolean(KEY_PREFIX_WIDGET_ONLY_PREMIERES + appWidgetId, false)
    }

    /**
     * Returns if this widget should display an infinite number of days.
     */
    fun isInfinite(context: Context, appWidgetId: Int): Boolean {
        val prefs = context.getSharedPreferences(SETTINGS_FILE, 0)
        return prefs.getBoolean(KEY_PREFIX_WIDGET_IS_INFINITE + appWidgetId, false)
    }

    /**
     * Returns if the layouts using larger fonts should be used.
     */
    fun isLargeFont(context: Context, appWidgetId: Int): Boolean {
        val prefs = context.getSharedPreferences(SETTINGS_FILE, 0)
        return prefs.getBoolean(KEY_PREFIX_WIDGET_IS_LARGE_FONT + appWidgetId, false)
    }

    /**
     * Returns if the watch button should be hidden.
     */
    fun isHideWatchButton(context: Context, appWidgetId: Int): Boolean {
        val prefs = context.getSharedPreferences(SETTINGS_FILE, 0)
        return prefs.getBoolean(KEY_PREFIX_WIDGET_IS_HIDE_WATCH_BUTTON + appWidgetId, false)
    }

    enum class WidgetTheme {
        DARK,
        LIGHT,
        SYSTEM
    }

    fun getTheme(context: Context, appWidgetId: Int): WidgetTheme {
        val value = context.getSharedPreferences(SETTINGS_FILE, 0)
            .getString(KEY_PREFIX_WIDGET_THEME + appWidgetId, null)
        return when (value) {
            context.getString(R.string.widget_theme_light) -> WidgetTheme.LIGHT
            context.getString(R.string.widget_theme_dark) -> WidgetTheme.DARK
            else -> WidgetTheme.SYSTEM
        }
    }

    /**
     * Calculates the background color for this widget based on user preference.
     *
     * @param lightBackground If true, will return white with alpha. Otherwise black with alpha. See
     * [getTheme].
     */
    fun getWidgetBackgroundColor(
        context: Context, appWidgetId: Int,
        lightBackground: Boolean
    ): Int {
        val opacity = context.getSharedPreferences(SETTINGS_FILE, 0)
            .getString(KEY_PREFIX_WIDGET_BACKGROUND_OPACITY + appWidgetId, null)?.toIntOrNull()
            ?: DEFAULT_WIDGET_BACKGROUND_OPACITY_INT

        var baseColor = ContextCompat.getColor(
            context,
            if (lightBackground) R.color.widget_default_background_light else R.color.widget_default_background
        )
        // strip alpha from base color
        baseColor = baseColor and 0xFFFFFF
        // add new alpha
        val alpha = (opacity * 255 / 100) shl 24
        return alpha or baseColor
    }
}