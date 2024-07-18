// SPDX-License-Identifier: Apache-2.0
// Copyright 2022-2024 Uwe Trottmann

package com.battlelancer.seriesguide.movies

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.LanguageTools
import java.util.Locale

object MoviesSettings {

    private const val KEY_LAST_ACTIVE_MOVIES_TAB = "seriesguide.movies.selectedtab"

    private const val KEY_MOVIES_LANGUAGE = "com.battlelancer.seriesguide.languagemovies"
    private const val KEY_MOVIES_REGION = "com.battlelancer.seriesguide.regionmovies"

    fun saveLastMoviesTabPosition(context: Context, position: Int) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putInt(KEY_LAST_ACTIVE_MOVIES_TAB, position)
            .apply()
    }

    /**
     * Return the position of the last selected movies tab.
     */
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
        val languageCode = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_MOVIES_LANGUAGE, null)
            ?: context.getString(R.string.content_default_language)
        return LanguageTools.mapLegacyMovieCode(languageCode)
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