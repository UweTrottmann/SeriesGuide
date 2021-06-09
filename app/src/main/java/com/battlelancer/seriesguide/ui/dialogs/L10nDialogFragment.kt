package com.battlelancer.seriesguide.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.ArrayRes
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.FragmentManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.LanguageTools
import com.battlelancer.seriesguide.util.safeShow
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.text.Collator

/**
 * A dialog displaying a list of languages to choose from, posting a
 * [L10nDialogFragment.LanguageChangedEvent] if a
 * language different from the given one was chosen.
 */
class L10nDialogFragment : AppCompatDialogFragment() {

    class LanguageChangedEvent(val selectedLanguageCode: String, val tag: String?)

    data class LocalizationItem(
        val code: String,
        val displayText: String
    )

    private lateinit var sortedLanguageCodes: Array<String>
    private var currentLanguageIndex: Int = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val languageCodes =
            resources.getStringArray(requireArguments().getInt(ARG_RES_ID_LANGUAGE_CODES))
        val localizationItems = languageCodes.mapTo(ArrayList(languageCodes.size)) {
            LocalizationItem(it, LanguageTools.buildLanguageDisplayName(it))
        }

        val collator = Collator.getInstance()
        localizationItems.sortWith { left: LocalizationItem, right: LocalizationItem ->
            collator.compare(left.displayText, right.displayText)
        }

        sortedLanguageCodes = localizationItems.mapToArray { it.code }
        val sortedLanguages = localizationItems.mapToArray { it.displayText }

        val currentLanguageCode = requireArguments().getString(ARG_SELECTED_LANGUAGE_CODE)
        currentLanguageIndex = 0
        if (currentLanguageCode != null) {
            val indexOrMinus1 = sortedLanguageCodes.indexOf(currentLanguageCode)
            if (indexOrMinus1 != -1) {
                currentLanguageIndex = indexOrMinus1
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.pref_language)
                .setSingleChoiceItems(
                        sortedLanguages,
                        currentLanguageIndex
                ) { _, item -> postLanguageChangedEvent(item) }.create()
    }

    private inline fun <T, reified R> List<T>.mapToArray(transform: (T) -> R): Array<R> {
        return Array(size) { index ->
            transform(get(index))
        }
    }

    private fun postLanguageChangedEvent(selectedLanguagePosition: Int) {
        if (selectedLanguagePosition == currentLanguageIndex) {
            Timber.d("Language is unchanged, do nothing.")
            dismiss()
            return
        }

        EventBus.getDefault()
                .post(LanguageChangedEvent(sortedLanguageCodes[selectedLanguagePosition], tag))
        dismiss()
    }

    companion object {

        const val TAG_ADD_DIALOG = "languageDialogAdd"
        const val TAG_DISCOVER = "languageDialogDiscover"

        private const val ARG_SELECTED_LANGUAGE_CODE = "selectedLanguageCode"
        private const val ARG_RES_ID_LANGUAGE_CODES = "resIdLanguageCodes"

        private fun show(
            fragmentManager: FragmentManager,
            selectedLanguageCode: String?,
            @ArrayRes resIdLanguageCodes: Int,
            tag: String
        ) {
            L10nDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SELECTED_LANGUAGE_CODE, selectedLanguageCode)
                    putInt(ARG_RES_ID_LANGUAGE_CODES, resIdLanguageCodes)
                }
            }.safeShow(fragmentManager, tag)
        }

        /**
         * @param selectedLanguageCode two letter ISO 639-1 language code,
         * plus optional ISO-3166-1 region tag. If null selects first language code.
         */
        @JvmStatic
        fun forShow(
            fragmentManager: FragmentManager,
            selectedLanguageCode: String?,
            tag: String
        ) {
            show(fragmentManager, selectedLanguageCode, R.array.languageCodesShows, tag)
        }

        /**
         * @param selectedLanguageCode two letter ISO 639-1 language code,
         * plus optional ISO-3166-1 region tag. If null selects first language code.
         */
        @JvmStatic
        fun forPerson(
            fragmentManager: FragmentManager,
            selectedLanguageCode: String?,
            tag: String
        ) {
            show(fragmentManager, selectedLanguageCode, R.array.languageCodesMovies, tag)
        }
    }
}
