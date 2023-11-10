// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.LanguageTools

object ShowsSettings {

    private const val KEY_LAST_ACTIVE_SHOWS_TAB = "com.battlelancer.seriesguide.activitytab"

//    @Deprecated("language is stored per show or defined by place of usage")
//    private const val KEY_LANGUAGE_PREFERRED = "language"
    const val KEY_LANGUAGE_FALLBACK = "com.battlelancer.seriesguide.languageFallback"
    private const val KEY_LANGUAGE_SEARCH = "com.battlelancer.seriesguide.languagesearch"

    fun saveLastShowsTabPosition(context: Context, position: Int) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putInt(ShowsSettings.KEY_LAST_ACTIVE_SHOWS_TAB, position)
            .apply()
    }

    /**
     * Return the position of the last selected shows tab.
     */
    fun getLastShowsTabPosition(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(
                KEY_LAST_ACTIVE_SHOWS_TAB,
                ShowsActivityImpl.Tab.SHOWS.index
            )
    }

    fun saveShowsSearchLanguage(context: Context, languageCode: String) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(KEY_LANGUAGE_SEARCH, languageCode)
        }
    }

    /**
     * Returns a two letter ISO 639-1 language code plus ISO-3166-1 region tag,
     * of the language the user prefers when searching. Defaults to English.
     */
    @JvmStatic
    fun getShowsSearchLanguage(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        var languageCode = prefs.getString(KEY_LANGUAGE_SEARCH, null)
        // For backwards compatibility:
        if (languageCode != null) {
            // - map legacy codes to include region tag (en -> en-US),
            languageCode = LanguageTools.mapLegacyShowCode(languageCode)
            // - change "any language" code to not set.
            if (context.getString(R.string.language_code_any) == languageCode) {
                prefs.edit().remove(KEY_LANGUAGE_SEARCH).apply()
                languageCode = null
            }
        }
        return if (languageCode.isNullOrEmpty()) {
            context.getString(R.string.content_default_language)
        } else languageCode
    }

    /**
     * Returns a two letter ISO 639-1 language code plus ISO-3166-1 region tag
     * of the fallback show language preferred by the user. Defaults to English.
     */
    fun getShowsLanguageFallback(context: Context): String {
        val languageCode = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_LANGUAGE_FALLBACK, null)
            ?: context.getString(R.string.content_default_language)
        return LanguageTools.mapLegacyShowCode(languageCode)
    }

}