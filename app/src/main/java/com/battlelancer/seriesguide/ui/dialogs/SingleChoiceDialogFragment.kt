package com.battlelancer.seriesguide.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.content.edit
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.util.safeShow
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * A dialog displaying a list of options to choose from, saving the selected option to the given
 * preference upon selection by the user.
 */
class SingleChoiceDialogFragment : AppCompatDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()

        val items: Array<out String> = resources.getStringArray(args.getInt(ARG_ITEM_ARRAY))

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(args.getInt(ARG_DIALOG_TITLE)))
            .setSingleChoiceItems(
                items,
                args.getInt(ARG_SELECTED)
            ) { _: DialogInterface?, item: Int ->
                val value = resources.getStringArray(args.getInt(ARG_ITEM_DATA))[item]
                PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
                    putString(args.getString(ARG_PREF_KEY), value)
                }
                dismiss()
            }
            .create()
    }

    companion object {

        private const val ARG_ITEM_ARRAY = "ITEM_ARRAY"
        private const val ARG_ITEM_DATA = "ITEM_DATA"
        private const val ARG_SELECTED = "SELECTED"
        private const val ARG_PREF_KEY = "PREF_KEY"
        private const val ARG_DIALOG_TITLE = "DIALOG_TITLE"

        fun show(
            fragmentManager: FragmentManager,
            itemArrayResource: Int,
            itemDataArrayResource: Int,
            selectedItemIndex: Int,
            preferenceKey: String,
            dialogTitleResource: Int,
            tag: String
        ) {
            SingleChoiceDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ITEM_ARRAY, itemArrayResource)
                    putInt(ARG_ITEM_DATA, itemDataArrayResource)
                    putInt(ARG_SELECTED, selectedItemIndex)
                    putString(ARG_PREF_KEY, preferenceKey)
                    putInt(ARG_DIALOG_TITLE, dialogTitleResource)
                }
            }.safeShow(fragmentManager, tag)
        }
    }
}