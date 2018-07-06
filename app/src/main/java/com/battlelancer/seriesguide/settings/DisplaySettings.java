package com.battlelancer.seriesguide.settings;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import java.util.Locale;

/**
 * Settings related to appearance, display formats and sort orders.
 */
public class DisplaySettings {

    public static final String LANGUAGE_EN = "en";

    public static final String KEY_THEME = "com.battlelancer.seriesguide.theme";

    public static final String KEY_LANGUAGE_PREFERRED = "language";
    public static final String KEY_LANGUAGE_FALLBACK = "com.battlelancer.seriesguide.languageFallback";

    public static final String KEY_MOVIES_LANGUAGE = "com.battlelancer.seriesguide.languagemovies";

    public static final String KEY_MOVIES_REGION = "com.battlelancer.seriesguide.regionmovies";

    public static final String KEY_LANGUAGE_SEARCH = "com.battlelancer.seriesguide.languagesearch";

    public static final String KEY_NUMBERFORMAT = "numberformat";

    public static final String NUMBERFORMAT_DEFAULT = "default";

    public static final String NUMBERFORMAT_ENGLISHLOWER = "englishlower";

    public static final String KEY_SHOWS_TIME_OFFSET = "com.battlelancer.seriesguide.timeoffset";

    public static final String KEY_NO_RELEASED_EPISODES = "onlyFutureEpisodes";

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

    public static final String KEY_PREVENT_SPOILERS =
            "com.battlelancer.seriesguide.PREVENT_SPOILERS";

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
     * Returns two letter ISO 639-1 language code of the show language preferred by the user.
     * Defaults to 'en'.
     */
    public static String getShowsLanguage(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_LANGUAGE_PREFERRED, LANGUAGE_EN);
    }

    /**
     * Returns two letter ISO 639-1 language code of the fallback show language preferred by the
     * user. Defaults to 'en'.
     */
    public static String getShowsLanguageFallback(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_LANGUAGE_FALLBACK, LANGUAGE_EN);
    }

    /**
     * @return Two letter ISO 639-1 language code plus an extra ISO-3166-1 region tag used by TMDB
     * as preferred by the user. Or the default language.
     */
    public static String getMoviesLanguage(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_MOVIES_LANGUAGE, context.getString(R.string.movie_default_language));
    }

    /**
     * @return Two letter ISO-3166-1 region tag used by TMDB as preferred by the user. Or the
     * default region of the device. Or as a last resort "US".
     */
    public static String getMoviesRegion(Context context) {
        String countryCode = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_MOVIES_REGION, null);
        if (TextUtils.isEmpty(countryCode)) {
            countryCode = Locale.getDefault().getCountry();
            if (TextUtils.isEmpty(countryCode)) {
                countryCode = Locale.US.getCountry();
            }
        }
        return countryCode;
    }

    /**
     * @return Two letter ISO 639-1 language code of the language the user prefers when searching or
     * 'xx' if all languages should be searched. Defaults to {@link #getShowsLanguage(Context)}.
     */
    public static String getSearchLanguage(Context context) {
        String languageCode = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_LANGUAGE_SEARCH, getShowsLanguage(context));
        return TextUtils.isEmpty(languageCode)
                ? context.getString(R.string.language_code_any)
                : languageCode;
    }

    public static String getNumberFormat(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_NUMBERFORMAT, NUMBERFORMAT_DEFAULT);
    }

    /**
     * @return A positive or negative number of hours to offset show release times by. Defaults to
     * 0.
     */
    public static int getShowsTimeOffset(Context context) {
        try {
            return Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(KEY_SHOWS_TIME_OFFSET, "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
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

    /**
     * Whether the app should hide details potentially spoiling an unwatched episode.
     */
    public static boolean preventSpoilers(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                KEY_PREVENT_SPOILERS, false);
    }
}
