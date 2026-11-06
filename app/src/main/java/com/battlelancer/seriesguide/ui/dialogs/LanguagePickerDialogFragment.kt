// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright 2024-2025 Uwe Trottmann

package com.battlelancer.seriesguide.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.ArrayRes
import androidx.appcompat.app.AppCompatDialogFragment
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.LanguageTools
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.Collator

/**
 * A dialog displaying a list of languages or none to choose from.
 *
 * The first languages of the list may be most used languages that keep their order (see the
 * factory methods), the remaining are sorted alphabetically by localized display name.
 */
class LanguagePickerDialogFragment : AppCompatDialogFragment() {

    interface OnPickedListener {
        fun onPicked(languageCode: String?)
    }

    var onPickedListener: OnPickedListener? = null
    private lateinit var languageCodes: Array<String>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val languageCodes =
            resources.getStringArray(requireArguments().getInt(ARG_RES_ID_LANGUAGE_CODES))
        val localizationItems = languageCodes.mapTo(ArrayList(languageCodes.size)) {
            // Codes are two-letter, so country-less
            LocalizationItem(it, LanguageTools.getDisplayNameForLanguageCode(it))
        }

        // Keep order of the most used languages, sort the remaining by display name
        val collator = Collator.getInstance()
        val topCount = requireArguments().getInt(ARG_TOP_COUNT)
            .coerceIn(0, localizationItems.size)
        val top = localizationItems.take(topCount)
        val remaining = localizationItems.drop(topCount).toMutableList()
        remaining.sortWith { left: LocalizationItem, right: LocalizationItem ->
            collator.compare(left.displayText, right.displayText)
        }

        val combined = top + remaining
        this.languageCodes = combined.mapToArray { it.code }
        val languageNames = combined.mapToArray { it.displayText }

        val selectedLanguageCode = requireArguments().getString(ARG_SELECTED_LANGUAGE_CODE)
        var selectedLanguageIndex = -1 /* select none by default */
        if (selectedLanguageCode != null) {
            val indexOrMinus1 = this.languageCodes.indexOf(selectedLanguageCode)
            if (indexOrMinus1 != -1) {
                selectedLanguageIndex = indexOrMinus1
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(requireArguments().getInt(ARG_RES_ID_TITLE))
            .setSingleChoiceItems(
                languageNames,
                selectedLanguageIndex
            ) { _, selectedLanguagePosition ->
                onPickedListener?.onPicked(this.languageCodes[selectedLanguagePosition])
                dismiss()
            }
            .setNegativeButton(R.string.action_reset) { _, _ ->
                onPickedListener?.onPicked(null)
            }
            .create()
    }

    private inline fun <T, reified R> List<T>.mapToArray(transform: (T) -> R): Array<R> {
        return Array(size) { index ->
            transform(get(index))
        }
    }

    data class LocalizationItem(
        val code: String,
        val displayText: String
    )

    companion object {

        private const val ARG_SELECTED_LANGUAGE_CODE = "selectedLanguageCode"
        private const val ARG_RES_ID_LANGUAGE_CODES = "resIdLanguageCodes"
        private const val ARG_RES_ID_TITLE = "resIdTitle"
        private const val ARG_TOP_COUNT = "topCount"

        /**
         * The first 5 languages of the shows and movies lists are the most used ones.
         */
        private const val TOP_COUNT_TMDB = 5

        fun createForShows(selectedLanguageCode: String?): LanguagePickerDialogFragment =
            create(selectedLanguageCode, R.array.filter_languages_shows, TOP_COUNT_TMDB)

        fun createForMovies(selectedLanguageCode: String?): LanguagePickerDialogFragment =
            create(selectedLanguageCode, R.array.filter_languages_movies, TOP_COUNT_TMDB)

        /**
         * All languages are sorted by display name.
         */
        fun createForComments(selectedLanguageCode: String?): LanguagePickerDialogFragment =
            create(selectedLanguageCode, R.array.filter_languages_comments, 0)

        /**
         * @param selectedLanguageCode two-letter ISO 639-1 language code. If null selects none.
         * @param topCount number of languages at the start of the list that keep their order.
         */
        private fun create(
            selectedLanguageCode: String?,
            @ArrayRes languageCodesRes: Int,
            topCount: Int
        ): LanguagePickerDialogFragment {
            return LanguagePickerDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SELECTED_LANGUAGE_CODE, selectedLanguageCode)
                    putInt(ARG_RES_ID_LANGUAGE_CODES, languageCodesRes)
                    putInt(ARG_TOP_COUNT, topCount)
                    putInt(ARG_RES_ID_TITLE, R.string.filter_language)
                }
            }
        }

    }
}
