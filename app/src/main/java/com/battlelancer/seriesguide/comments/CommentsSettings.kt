// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.comments

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

object CommentsSettings {

    private const val KEY_COMMENTS_LANGUAGE = "seriesguide.comments.language"

    /**
     * @param code Two-letter ISO 639-1 language code or null to show comments of all languages.
     */
    fun saveCommentsLanguage(context: Context, code: String?) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(KEY_COMMENTS_LANGUAGE, code)
        }
    }

    /**
     * @return Two-letter ISO 639-1 language code to filter comments by as preferred by the user.
     * Or null to show comments of all languages (the default).
     */
    fun getCommentsLanguage(context: Context): String? {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_COMMENTS_LANGUAGE, null)
    }

}
