package com.battlelancer.seriesguide.movies

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.R
import java.util.Locale

object MoviesSettings {

    private const val KEY_LAST_ACTIVE_MOVIES_TAB = "com.battlelancer.seriesguide.moviesActiveTab"

    private const val KEY_MOVIES_LANGUAGE = "com.battlelancer.seriesguide.languagemovies"
    private const val KEY_MOVIES_REGION = "com.battlelancer.seriesguide.regionmovies"

    @JvmStatic
    fun saveLastMoviesTabPosition(context: Context, position: Int) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putInt(KEY_LAST_ACTIVE_MOVIES_TAB, position)
            .apply()
    }

    /**
     * Return the position of the last selected movies tab.
     */
    @JvmStatic
    fun getLastMoviesTabPosition(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(KEY_LAST_ACTIVE_MOVIES_TAB, 0)
    }

    fun saveMoviesLanguage(context: Context, code: String?) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(KEY_MOVIES_LANGUAGE, code)
        }
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

    fun saveMoviesRegion(context: Context, code: String?) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(KEY_MOVIES_REGION, code)
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

}