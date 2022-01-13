package com.battlelancer.seriesguide.ui.lists

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.DialogListManageBinding
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.util.safeShow
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog to rename or remove a list.
 */
class ListManageDialogFragment : AppCompatDialogFragment() {

    private var binding: DialogListManageBinding? = null
    private lateinit var listId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // hide title, use custom theme
        setStyle(STYLE_NO_TITLE, 0)

        listId = requireArguments().getString(ARG_LIST_ID)!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogListManageBinding.inflate(layoutInflater)
        this.binding = binding

        // buttons
        binding.buttonNegative.isEnabled = false
        binding.buttonNegative.setText(R.string.list_remove)
        binding.buttonNegative.setOnClickListener {
            // remove list and items
            ListsTools.removeList(requireContext(), listId)
            dismiss()
        }
        binding.buttonPositive.setText(android.R.string.ok)
        binding.buttonPositive.setOnClickListener {
            val editText = this.binding?.textInputLayoutListManageListName?.editText
                ?: return@setOnClickListener

            // update title
            val listName = editText.text.toString().trim()
            ListsTools.renameList(requireContext(), listId, listName)

            dismiss()
        }

        lifecycleScope.launchWhenCreated {
            configureViews()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.getRoot())
            .create()
    }

    private fun configureViews() {
        // pre-populate list title
        val list = requireContext().contentResolver
            .query(
                SeriesGuideContract.Lists.buildListUri(listId), arrayOf(
                    SeriesGuideContract.Lists.NAME
                ), null, null, null
            )
        if (list == null) {
            // list might have been removed, or query failed
            dismiss()
            return
        }
        if (!list.moveToFirst()) {
            // list not found
            list.close()
            dismiss()
            return
        }
        val listName = list.getString(0)
        list.close()

        val binding = this@ListManageDialogFragment.binding
        if (binding == null) {
            dismiss()
            return
        }

        val textInputLayoutName = binding.textInputLayoutListManageListName
        val editTextName = textInputLayoutName.editText!!
        editTextName.setText(listName)
        editTextName.addTextChangedListener(
            AddListDialogFragment.ListNameTextWatcher(
                requireContext(), textInputLayoutName,
                binding.buttonPositive, listName
            )
        )

        // do only allow removing if this is NOT the last list
        val lists = requireContext().contentResolver.query(
            SeriesGuideContract.Lists.CONTENT_URI, arrayOf(
                SeriesGuideContract.Lists._ID
            ), null, null, null
        )
        if (lists != null) {
            if (lists.count > 1) {
                binding.buttonNegative.isEnabled = true
            }
            lists.close()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {

        private const val TAG = "listmanagedialog"
        private const val ARG_LIST_ID = "listId"

        private fun newInstance(listId: String): ListManageDialogFragment {
            val f = ListManageDialogFragment()
            val args = Bundle()
            args.putString(ARG_LIST_ID, listId)
            f.arguments = args
            return f
        }

        /**
         * Display a dialog which allows to edit the title of this list or remove it.
         */
        fun show(listId: String, fm: FragmentManager) {
            // replace any currently showing list dialog (do not add it to the back stack)
            val ft = fm.beginTransaction()
            val prev = fm.findFragmentByTag(TAG)
            if (prev != null) {
                ft.remove(prev)
            }
            newInstance(listId).safeShow(fm, ft, TAG)
        }
    }
}