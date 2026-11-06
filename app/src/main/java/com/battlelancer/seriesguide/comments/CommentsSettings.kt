// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.comments

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.dialogs.SingleChoiceDialogFragment
import com.uwetrottmann.trakt5.enums.CommentsSortBy
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object CommentsSettings {

    private const val KEY_COMMENTS_LANGUAGE = "seriesguide.comments.language"
    private const val KEY_COMMENTS_SORT_ORDER = "seriesguide.comments.sortorder"

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

    /**
     * The [index] must match the position in [R.array.comments_sorting] and the [value] the one
     * in [R.array.comments_sorting_data].
     */
    enum class CommentsSortOrder(
        val index: Int,
        val value: String,
        val sortBy: CommentsSortBy
    ) {
        NEWEST(0, "newest", CommentsSortBy.NEWEST),
        LIKES(1, "likes", CommentsSortBy.LIKES);

        companion object {
            fun fromValue(value: String?): CommentsSortOrder =
                entries.find { it.value == value } ?: NEWEST
        }
    }

    /**
     * @return The sort order preferred by the user, by default [CommentsSortOrder.NEWEST].
     */
    fun getCommentsSortOrder(context: Context): CommentsSortOrder {
        val value = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_COMMENTS_SORT_ORDER, null)
        return CommentsSortOrder.fromValue(value)
    }

    /**
     * Shows a dialog to choose the sort order, which is saved once selected.
     * Use [sortOrderChanges] to get notified.
     */
    fun showSortOrderDialog(context: Context, fragmentManager: FragmentManager) {
        SingleChoiceDialogFragment.show(
            fragmentManager,
            R.array.comments_sorting,
            R.array.comments_sorting_data,
            getCommentsSortOrder(context).index,
            KEY_COMMENTS_SORT_ORDER,
            R.string.sort,
            "commentsSortOrderDialog"
        )
    }

    /**
     * Emits the new sort order each time it is changed. Stops listening for changes once collection
     * is canceled.
     */
    fun sortOrderChanges(context: Context): Flow<CommentsSortOrder> = callbackFlow {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        // Keep a reference to the listener, SharedPreferences only holds a weak one
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
            if (key == KEY_COMMENTS_SORT_ORDER) {
                trySend(
                    CommentsSortOrder.fromValue(sharedPrefs.getString(KEY_COMMENTS_SORT_ORDER, null))
                )
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

}
