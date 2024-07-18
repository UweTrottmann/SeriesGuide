// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.ArrayRes
import androidx.appcompat.app.AppCompatDialogFragment
import com.battlelancer.seriesguide.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.Collator
import java.util.Locale

/**
 * A dialog displaying a list of languages or none to choose from.
 *
 * The first 5 are most used languages, the remaining are sorted alhpabetically
 * by localized display name.
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
            LocalizationItem(it, Locale(it, "").displayName)
        }

        // Take the first top 5, sort the remaining by display name
        val collator = Collator.getInstance()
        val top5 = localizationItems.take(5)
        val remaining = localizationItems.takeLast(localizationItems.size - 5).toMutableList()
        remaining.sortWith { left: LocalizationItem, right: LocalizationItem ->
            collator.compare(left.displayText, right.displayText)
        }

        val combined = top5 + remaining
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

        fun createForShows(selectedLanguageCode: String?): LanguagePickerDialogFragment =
            create(selectedLanguageCode, R.array.filter_languages_shows)

        fun createForMovies(selectedLanguageCode: String?): LanguagePickerDialogFragment =
            create(selectedLanguageCode, R.array.filter_languages_movies)

        /**
         * @param selectedLanguageCode two letter ISO 639-1 language code. If null selects none.
         */
        private fun create(
            selectedLanguageCode: String?,
            @ArrayRes languageCodesRes: Int
        ): LanguagePickerDialogFragment {
            return LanguagePickerDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SELECTED_LANGUAGE_CODE, selectedLanguageCode)
                    putInt(ARG_RES_ID_LANGUAGE_CODES, languageCodesRes)
                    putInt(ARG_RES_ID_TITLE, R.string.filter_language)
                }
            }
        }

    }
}
