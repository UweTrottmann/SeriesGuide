// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.popular

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDialogFragment
import com.battlelancer.seriesguide.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

/**
 * A dialog displaying a list of languages or none to choose from.
 */
class LanguagePickerDialogFragment : AppCompatDialogFragment() {

    interface OnPickedListener {
        fun onPicked(languageCode: String?)
    }

    var onPickedListener: OnPickedListener? = null
    private lateinit var languageCodes: Array<String>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        languageCodes =
            resources.getStringArray(requireArguments().getInt(ARG_RES_ID_LANGUAGE_CODES))
        val languageNames = languageCodes.mapToArray { Locale(it, "").displayName }

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

    private inline fun <T, reified R> Array<T>.mapToArray(transform: (T) -> R): Array<R> {
        return Array(size) { index ->
            transform(get(index))
        }
    }

    companion object {

        private const val ARG_SELECTED_LANGUAGE_CODE = "selectedLanguageCode"
        private const val ARG_RES_ID_LANGUAGE_CODES = "resIdLanguageCodes"
        private const val ARG_RES_ID_TITLE = "resIdTitle"

        /**
         * @param selectedLanguageCode two letter ISO 639-1 language code. If null selects none.
         */
        fun create(
            selectedLanguageCode: String?,
            @StringRes titleRes: Int = R.string.filter_language
        ): LanguagePickerDialogFragment {
            return LanguagePickerDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SELECTED_LANGUAGE_CODE, selectedLanguageCode)
                    putInt(ARG_RES_ID_LANGUAGE_CODES, R.array.filter_languages)
                    putInt(ARG_RES_ID_TITLE, titleRes)
                }
            }
        }

    }
}
