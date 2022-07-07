package com.battlelancer.seriesguide.people

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.LanguageTools

object PeopleSettings {

    private const val KEY_PERSON_LANGUAGE = "com.uwetrottmann.seriesguide.languageperson"

    /**
     * @return Two letter ISO 639-1 language code plus an extra ISO-3166-1 region tag used by TMDB
     * as preferred by the user. Or the default language.
     */
    fun getPersonLanguage(context: Context): String {
        val languageCode = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_PERSON_LANGUAGE, null)
            ?: context.getString(R.string.content_default_language)
        return LanguageTools.mapLegacyMovieCode(languageCode)
    }

    fun setPersonLanguage(context: Context, languageCode: String) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(KEY_PERSON_LANGUAGE, languageCode)
        }
    }

}