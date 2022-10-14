package com.battlelancer.seriesguide.lists

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.view.isGone
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.DialogManageListsBinding
import com.battlelancer.seriesguide.databinding.ItemListCheckedBinding
import com.battlelancer.seriesguide.lists.ManageListsDialogFragmentViewModel.ListWithItem
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.safeShow
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Displays a dialog displaying all user created lists,
 * allowing to add or remove the given show for any.
 */
class ManageListsDialogFragment : AppCompatDialogFragment() {

    private var binding: DialogManageListsBinding? = null
    private val model by viewModels<ManageListsDialogFragmentViewModel> {
        ManageListsDialogFragmentViewModelFactory(requireActivity().application, showId)
    }
    private var showId: Long = 0

    /**
     * Remains 0 if TMDB id for show not found (show is not migrated to TMDB data).
     */
    private var showTmdbId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showId = requireArguments().getLong(ARG_LONG_SHOW_ID)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogManageListsBinding.inflate(layoutInflater)
        this.binding = binding

        binding.textViewManageListsError.isGone = true
        val adapter = ListsWithItemAdapter(onItemClickListener)
        binding.recyclerViewManageLists.also {
            val layoutManager = LinearLayoutManager(it.context)
            it.layoutManager = layoutManager
            it.adapter = adapter
            it.itemAnimator = null // Disable to let checkbox show its change animation.
        }

        // Note: viewLifeCycleOwner not available for DialogFragment, use DialogFragment itself.
        model.showDetails.observe(this) { showDetails ->
            if (showDetails == null) {
                Toast.makeText(requireContext(), R.string.database_error, Toast.LENGTH_LONG).show()
                dismiss()
                return@observe
            }
            showTmdbId = showDetails.tmdbId ?: 0

            binding.item.text = showDetails.title

            if (showTmdbId <= 0) {
                // Note: see OK button handler that prevents changing lists if not migrated.
                ViewTools.configureNotMigratedWarning(binding.textViewManageListsError, true)
            }
        }
        model.listsWithItem.observe(this) { lists ->
            adapter.submitList(lists)
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                // Note: see show details loader that prevents loading list data if not migrated.
                if ((model.showDetails.value?.tmdbId ?: 0) <= 0) {
                    dismiss()
                    return@setPositiveButton
                }
                // add item to selected lists, remove from previously selected lists
                val addToTheseLists: MutableList<String> = ArrayList()
                val removeFromTheseLists: MutableList<String> = ArrayList()
                val lists = model.listsWithItem.value
                    ?: return@setPositiveButton // Do nothing, lists not loaded, yet.
                lists
                    .filter { it.isItemOnList != it.isItemOnListOriginal }
                    .forEach {
                        if (it.isItemOnListOriginal) {
                            // remove from list
                            removeFromTheseLists.add(it.listId)
                        } else {
                            // add to list
                            addToTheseLists.add(it.listId)
                        }
                    }
                ListsTools.changeListsOfItem(
                    requireContext(), showTmdbId,
                    ListItemTypes.TMDB_SHOW, addToTheseLists, removeFromTheseLists
                )
                dismiss()
            }
            .create()
    }

    private val onItemClickListener = object : ListsWithItemAdapter.ItemClickListener {
        override fun onItemClick(v: View, listId: String, isChecked: Boolean) {
            model.setIsItemOnList(listId, !isChecked)
        }
    }

    private class ListsWithItemAdapter(
        private val itemClickListener: ItemClickListener
    ) : ListAdapter<ListWithItem, ListsWithItemAdapter.ListWithItemViewHolder>(
        ListWithItemDiffCallback()
    ) {

        interface ItemClickListener {
            fun onItemClick(v: View, listId: String, isChecked: Boolean)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListWithItemViewHolder =
            ListWithItemViewHolder.inflate(parent, itemClickListener)

        override fun onBindViewHolder(holder: ListWithItemViewHolder, position: Int) =
            holder.bindTo(getItem(position))

        private class ListWithItemViewHolder(
            private val binding: ItemListCheckedBinding,
            itemClickListener: ItemClickListener
        ) : RecyclerView.ViewHolder(binding.root) {
            private var listWithItem: ListWithItem? = null

            init {
                binding.checkedTextViewList.setOnClickListener { view ->
                    listWithItem?.also {
                        itemClickListener.onItemClick(
                            view,
                            it.listId,
                            binding.checkedTextViewList.isChecked
                        )
                    }
                }
            }

            fun bindTo(listWithItem: ListWithItem?) {
                this.listWithItem = listWithItem
                binding.checkedTextViewList.text = listWithItem?.listName ?: ""
                binding.checkedTextViewList.isChecked = listWithItem?.isItemOnList ?: false
            }

            companion object {
                fun inflate(parent: ViewGroup, itemClickListener: ItemClickListener) =
                    ListWithItemViewHolder(
                        ItemListCheckedBinding.inflate(
                            LayoutInflater.from(parent.context),
                            parent,
                            false
                        ),
                        itemClickListener
                    )
            }
        }

        private class ListWithItemDiffCallback :
            DiffUtil.ItemCallback<ListWithItem>() {
            override fun areItemsTheSame(oldItem: ListWithItem, newItem: ListWithItem): Boolean =
                oldItem.listId == newItem.listId

            override fun areContentsTheSame(oldItem: ListWithItem, newItem: ListWithItem): Boolean =
                oldItem == newItem

        }
    }

    companion object {
        private const val TAG = "listsdialog"
        private const val ARG_LONG_SHOW_ID = "show_id"

        private fun newInstance(showId: Long): ManageListsDialogFragment {
            val f = ManageListsDialogFragment()
            val args = Bundle()
            args.putLong(ARG_LONG_SHOW_ID, showId)
            f.arguments = args
            return f
        }

        /**
         * Display a dialog which asks if the user wants to add the given show to one or more lists.
         */
        @JvmStatic
        fun show(fm: FragmentManager, showId: Long): Boolean {
            if (showId <= 0) return false
            // replace any currently showing list dialog (do not add it to the back stack)
            val ft = fm.beginTransaction()
            val prev = fm.findFragmentByTag(TAG)
            if (prev != null) {
                ft.remove(prev)
            }
            return newInstance(showId).safeShow(fm, ft, TAG)
        }
    }
}