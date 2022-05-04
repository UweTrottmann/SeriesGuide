package com.battlelancer.seriesguide.settings

import android.content.Context
import android.util.DisplayMetrics
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.TextTools
import java.util.Locale

/**
 * Settings related to appearance, display formats and sort orders.
 */
object DisplaySettings {

    const val LANGUAGE_EN = "en"

    const val KEY_THEME = "com.uwetrottmann.seriesguide.theme"
    const val KEY_DYNAMIC_COLOR = "com.uwetrottmann.seriesguide.dynamiccolor"

    @Deprecated("") // language is stored per show or defined by place of usage
    const val KEY_LANGUAGE_PREFERRED = "language"
    const val KEY_LANGUAGE_FALLBACK = "com.battlelancer.seriesguide.languageFallback"
    const val KEY_LANGUAGE_SEARCH = "com.battlelancer.seriesguide.languagesearch"

    const val KEY_MOVIES_LANGUAGE = "com.battlelancer.seriesguide.languagemovies"
    const val KEY_MOVIES_REGION = "com.battlelancer.seriesguide.regionmovies"

    private const val KEY_PERSON_LANGUAGE = "com.uwetrottmann.seriesguide.languageperson"

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

    /**
     * @return Two letter ISO 639-1 language code plus an extra ISO-3166-1 region tag used by TMDB
     * as preferred by the user. Or the default language.
     */
    @JvmStatic
    fun getMoviesLanguage(context: Context): String {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_MOVIES_LANGUAGE, null)
            ?: context.getString(R.string.movie_default_language)
    }

    /**
     * @return Two letter ISO 639-1 language code plus an extra ISO-3166-1 region tag used by TMDB
     * as preferred by the user. Or the default language.
     */
    fun getPersonLanguage(context: Context): String {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_PERSON_LANGUAGE, null)
            ?: context.getString(R.string.movie_default_language)
    }

    fun setPersonLanguage(context: Context, languageCode: String) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(KEY_PERSON_LANGUAGE, languageCode)
        }
    }

    /**
     * @return Two letter ISO-3166-1 region tag used by TMDB as preferred by the user. Or the
     * default region of the device. Or as a last resort "US".
     */
    @JvmStatic
    fun getMoviesRegion(context: Context): String {
        var countryCode = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_MOVIES_REGION, null)
        if (countryCode.isNullOrEmpty()) {
            countryCode = Locale.getDefault().country
            if (countryCode.isNullOrEmpty()) {
                countryCode = Locale.US.country
            }
        }
        return countryCode!!
    }

    /**
     * Returns a two letter ISO 639-1 language code, plus optional ISO-3166-1 region tag,
     * of the language the user prefers when searching. Defaults to English.
     */
    @JvmStatic
    fun getShowsSearchLanguage(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        var languageCode = prefs.getString(KEY_LANGUAGE_SEARCH, null)
        // For backwards compatibility: change "any language" code to not set.
        if (languageCode != null
            && context.getString(R.string.language_code_any) == languageCode) {
            prefs.edit().remove(KEY_LANGUAGE_SEARCH).apply()
            languageCode = null
        }
        return if (languageCode.isNullOrEmpty()) {
            context.getString(R.string.show_default_language)
        } else languageCode
    }

    /**
     * Returns a two letter ISO 639-1 language code, plus optional ISO-3166-1 region tag,
     * of the fallback show language preferred by the user. Defaults to 'en'.
     */
    fun getShowsLanguageFallback(context: Context): String {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_LANGUAGE_FALLBACK, null)
            ?: context.getString(R.string.show_default_language)
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