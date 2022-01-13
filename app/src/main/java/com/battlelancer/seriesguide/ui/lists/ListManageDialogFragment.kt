package com.battlelancer.seriesguide.ui.lists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.FragmentManager
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.ui.lists.AddListDialogFragment.ListNameTextWatcher
import com.battlelancer.seriesguide.util.safeShow
import com.google.android.material.textfield.TextInputLayout

/**
 * Dialog to rename or remove a list.
 */
class ListManageDialogFragment : AppCompatDialogFragment() {

    @BindView(R.id.textInputLayoutListManageListName)
    var textInputLayoutName: TextInputLayout? = null
    private var editTextName: EditText? = null

    @BindView(R.id.buttonNegative)
    var buttonNegative: Button? = null

    @BindView(R.id.buttonPositive)
    var buttonPositive: Button? = null
    private var unbinder: Unbinder? = null
    private lateinit var listId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // hide title, use custom theme
        setStyle(STYLE_NO_TITLE, 0)

        listId = requireArguments().getString(ARG_LIST_ID)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = inflater.inflate(R.layout.dialog_list_manage, container, false)
        unbinder = ButterKnife.bind(this, layout)

        editTextName = textInputLayoutName!!.editText

        // buttons
        buttonNegative!!.isEnabled = false
        buttonNegative!!.setText(R.string.list_remove)
        buttonNegative!!.setOnClickListener {
            // remove list and items
            ListsTools.removeList(requireContext(), listId)
            dismiss()
        }
        buttonPositive!!.setText(android.R.string.ok)
        buttonPositive!!.setOnClickListener {
            if (editTextName == null) {
                return@setOnClickListener
            }

            // update title
            val listName = editTextName!!.text.toString().trim()
            ListsTools.renameList(requireContext(), listId, listName)

            dismiss()
        }
        return layout
    }

    override fun onActivityCreated(arg0: Bundle?) {
        super.onActivityCreated(arg0)

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
        editTextName!!.setText(listName)
        editTextName!!.addTextChangedListener(
            ListNameTextWatcher(
                requireContext(), textInputLayoutName!!,
                buttonPositive!!, listName
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
                buttonNegative!!.isEnabled = true
            }
            lists.close()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder!!.unbind()
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