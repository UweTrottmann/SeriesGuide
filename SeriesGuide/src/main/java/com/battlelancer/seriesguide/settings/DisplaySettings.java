package com.battlelancer.seriesguide.settings;

import android.content.Context;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.ShowsActivity;

/**
 * Settings related to appearance, display formats and sort orders.
 */
public class DisplaySettings {

    public static final String LANGUAGE_EN = "en";

    public static final String KEY_THEME = "com.battlelancer.seriesguide.theme";

    public static final String KEY_LANGUAGE = "language";

    public static final String KEY_LANGUAGE_SEARCH = "com.battlelancer.seriesguide.languagesearch";

    public static final String KEY_NUMBERFORMAT = "numberformat";

    public static final String NUMBERFORMAT_DEFAULT = "default";

    public static final String NUMBERFORMAT_ENGLISHLOWER = "englishlower";

    public static final String KEY_NO_RELEASED_EPISODES = "onlyFutureEpisodes";

    public static final String KEY_NO_WATCHED_EPISODES
            = "com.battlelancer.seriesguide.activity.nowatched";

    public static final String KEY_SEASON_SORT_ORDER = "seasonSorting";

    public static final String KEY_EPISODE_SORT_ORDER = "episodeSorting";

    public static final String KEY_HIDE_SPECIALS = "onlySeasonEpisodes";

    public static final String KEY_SORT_IGNORE_ARTICLE
            = "com.battlelancer.seriesguide.sort.ignorearticle";

    public static final String KEY_LAST_ACTIVE_SHOWS_TAB
            = "com.battlelancer.seriesguide.activitytab";

    public static final String KEY_LAST_ACTIVE_LISTS_TAB
            = "com.battlelancer.seriesguide.listsActiveTab";

    public static final String KEY_LAST_ACTIVE_MOVIES_TAB
            = "com.battlelancer.seriesguide.moviesActiveTab";

    public static final String KEY_DISPLAY_EXACT_DATE =
            "com.battlelancer.seriesguide.shows.exactdate";

    /**
     * Returns true for xlarge, xlarge-land or sw720dp screens.
     */
    public static boolean isVeryLargeScreen(Context context) {
        return context.getResources().getBoolean(R.bool.isLargeTablet);
    }

    /**
     * Returns true if this is a large screen.
     */
    public static boolean isLargeScreen(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /**
     * Returns true for all screens with dpi higher than {@link DisplayMetrics#DENSITY_HIGH}.
     */
    public static boolean isVeryHighDensityScreen(Context context) {
        return context.getResources().getDisplayMetrics().densityDpi > DisplayMetrics.DENSITY_HIGH;
    }

    public static String getThemeIndex(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).
                getString(KEY_THEME, "0");
    }

    /**
     * @return Two letter ISO 639-1 language code of the content language preferred by the user. If
     * the value does not exist, defaults to English.
     */
    public static String getContentLanguage(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_LANGUAGE, LANGUAGE_EN);
    }

    /**
     * @return Two letter ISO 639-1 language code of the language the user prefers when searching or
     * an empty string if all languages should be searched. Defaults to {@link
     * #getContentLanguage(Context)} if the value does not exist.
     */
    public static String getSearchLanguage(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_LANGUAGE_SEARCH, getContentLanguage(context));
    }

    public static String getNumberFormat(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_NUMBERFORMAT, NUMBERFORMAT_DEFAULT);
    }

    public static Constants.EpisodeSorting getEpisodeSortOrder(Context context) {
        String orderId = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_EPISODE_SORT_ORDER, Constants.EpisodeSorting.OLDEST_FIRST.value());
        return Constants.EpisodeSorting.fromValue(orderId);
    }

    public static Constants.SeasonSorting getSeasonSortOrder(Context context) {
        String orderId = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_SEASON_SORT_ORDER, Constants.SeasonSorting.LATEST_FIRST.value());
        return Constants.SeasonSorting.fromValue(orderId);
    }

    public static boolean isNoReleasedEpisodes(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_NO_RELEASED_EPISODES, false);
    }

    public static boolean isNoWatchedEpisodes(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_NO_WATCHED_EPISODES, false);
    }

    /**
     * Whether to exclude special episodes wherever possible (except in the actual seasons and
     * episode lists of a show).
     */
    public static boolean isHidingSpecials(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                KEY_HIDE_SPECIALS, false);
    }

    /**
     * Whether shows and movies sorted by title should ignore the leading article.
     */
    public static boolean isSortOrderIgnoringArticles(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_SORT_IGNORE_ARTICLE, false);
    }

    /**
     * Return the position of the last selected shows tab.
     */
    public static int getLastShowsTabPosition(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(KEY_LAST_ACTIVE_SHOWS_TAB, ShowsActivity.InitBundle.INDEX_TAB_SHOWS);
    }

    /**
     * Return the position of the last selected lists tab.
     */
    public static int getLastListsTabPosition(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(KEY_LAST_ACTIVE_LISTS_TAB, 0);
    }

    /**
     * Return the position of the last selected movies tab.
     */
    public static int getLastMoviesTabPosition(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(KEY_LAST_ACTIVE_MOVIES_TAB, 0);
    }

    /**
     * Whether to show the exact/absolute date (31.10.2010) instead of a relative time string (in 5
     * days).
     */
    public static boolean isDisplayExactDate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                KEY_DISPLAY_EXACT_DATE, false);
    }
}
