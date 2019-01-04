package com.battlelancer.seriesguide.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.ArrayRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.FragmentManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.dialogs.LanguageChoiceDialogFragment.LanguageChangedEvent
import com.battlelancer.seriesguide.util.safeShow
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.util.*

/**
 * A dialog displaying a list of languages to choose from, posting a [LanguageChangedEvent] if
 * a language different from the given one was chosen.
 */
class LanguageChoiceDialogFragment : AppCompatDialogFragment() {

    class LanguageChangedEvent(val selectedLanguageCode: String, val tag: String?)

    private lateinit var languageCodes: Array<String>
    private var currentLanguagePosition: Int = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val languageCodesArrayRes = arguments!!.getInt(ARG_ARRAY_LANGUAGE_CODES)
        languageCodes = resources.getStringArray(languageCodesArrayRes)

        val languageCodeAny = getString(R.string.language_code_any)
        val languages = arrayOfNulls<String>(languageCodes.size)
        for (i in languageCodes.indices) {
            // example: "en" for shows or "en-US" for movies
            val languageCode = languageCodes[i]
            if (languageCodeAny == languageCode) {
                languages[i] = getString(R.string.any_language)
            } else {
                languages[i] = Locale(languageCode.substring(0, 2), "").displayName
            }
        }

        val currentLanguageCode = arguments!!.getString(ARG_SELECTED_LANGUAGE_CODE)

        currentLanguagePosition = 0
        if (currentLanguageCode != null) {
            for (i in languageCodes.indices) {
                if (languageCodes[i] == currentLanguageCode) {
                    currentLanguagePosition = i
                    break
                }
            }
        }

        return AlertDialog.Builder(activity!!)
                .setTitle(R.string.pref_language)
                .setSingleChoiceItems(
                        languages,
                        currentLanguagePosition
                ) { _, item -> postLanguageChangedEvent(item) }.create()
    }

    private fun postLanguageChangedEvent(selectedLanguagePosition: Int) {
        if (selectedLanguagePosition == currentLanguagePosition) {
            Timber.d("Language is unchanged, do nothing.")
            dismiss()
            return
        }

        EventBus.getDefault()
                .post(LanguageChangedEvent(languageCodes[selectedLanguagePosition], tag))
        dismiss()
    }

    companion object {

        const val TAG_ADD_DIALOG = "languageDialogAdd"
        const val TAG_DISCOVER = "languageDialogDiscover"

        private const val ARG_ARRAY_LANGUAGE_CODES = "languageCodes"
        private const val ARG_SELECTED_LANGUAGE_CODE = "selectedLanguageCode"

        /**
         * @param selectedLanguageCode two letter ISO 639-1 language code or 'xx' meaning any
         * language, if null selects first item of languageCodes.
         */
        @JvmStatic
        fun show(fragmentManager: FragmentManager, @ArrayRes languageCodes: Int,
                selectedLanguageCode: String?, tag: String) {
            LanguageChoiceDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ARRAY_LANGUAGE_CODES, languageCodes)
                    putString(ARG_SELECTED_LANGUAGE_CODE, selectedLanguageCode)
                }
            }.safeShow(fragmentManager, tag)
        }
    }
}
