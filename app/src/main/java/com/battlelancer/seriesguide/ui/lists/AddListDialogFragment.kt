package com.battlelancer.seriesguide.ui.lists

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.FragmentManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.DialogListManageBinding
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.util.safeShow
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import java.util.HashSet

/**
 * Displays a dialog to add a new list to lists.
 */
class AddListDialogFragment : AppCompatDialogFragment() {

    private var binding: DialogListManageBinding? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogListManageBinding.inflate(layoutInflater)
        this.binding = binding

        // title
        val editTextName = binding.textInputLayoutListManageListName.editText!!
        editTextName.addTextChangedListener(
            ListNameTextWatcher(
                requireContext(),
                binding.textInputLayoutListManageListName, binding.buttonPositive, null
            )
        )

        // buttons
        binding.buttonNegative.setText(android.R.string.cancel)
        binding.buttonNegative.setOnClickListener { dismiss() }
        binding.buttonPositive.setText(R.string.list_add)
        binding.buttonPositive.setOnClickListener {
            val editText = this.binding?.textInputLayoutListManageListName?.editText
                ?: return@setOnClickListener

            // add list
            val listName = editText.text.toString().trim()
            ListsTools.addList(requireContext(), listName)

            dismiss()
        }
        binding.buttonPositive.isEnabled = false

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.getRoot())
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    /**
     * Disables the given button if the watched text has only whitespace or the list name is already
     * used. Does currently not protect against a new list resulting in the same list id (if
     * inserted just resets the properties of the existing list).
     */
    class ListNameTextWatcher(
        private val context: Context,
        private val textInputLayoutName: TextInputLayout,
        private val buttonPositive: TextView,
        private val currentName: String?
    ) : TextWatcher {

        private val listNames: HashSet<String>

        init {
            val listNameQuery = context.contentResolver
                .query(
                    SeriesGuideContract.Lists.CONTENT_URI,
                    arrayOf(SeriesGuideContract.Lists._ID, SeriesGuideContract.Lists.NAME),
                    null,
                    null,
                    null
                )
            listNames = HashSet()
            if (listNameQuery != null) {
                while (listNameQuery.moveToNext()) {
                    listNames.add(listNameQuery.getString(1))
                }
                listNameQuery.close()
            }
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            val name = s.toString().trim { it <= ' ' }
            if (name.isEmpty()) {
                buttonPositive.isEnabled = false
                return
            }
            if (currentName != null && currentName == name) {
                buttonPositive.isEnabled = true
                return
            }
            if (listNames.contains(name)) {
                textInputLayoutName.error = context.getString(R.string.error_name_already_exists)
                textInputLayoutName.isErrorEnabled = true
                buttonPositive.isEnabled = false
            } else {
                textInputLayoutName.error = null
                textInputLayoutName.isErrorEnabled = false
                buttonPositive.isEnabled = true
            }
        }

        override fun afterTextChanged(s: Editable) {}
    }

    companion object {
        private const val TAG = "addlistdialog"

        /**
         * Display a dialog which allows to edit the title of this list or remove it.
         */
        fun show(fm: FragmentManager) {
            // replace any currently showing list dialog (do not add it to the back stack)
            val ft = fm.beginTransaction()
            val prev = fm.findFragmentByTag(TAG)
            if (prev != null) {
                ft.remove(prev)
            }
            AddListDialogFragment().safeShow(fm, ft, TAG)
        }
    }
}