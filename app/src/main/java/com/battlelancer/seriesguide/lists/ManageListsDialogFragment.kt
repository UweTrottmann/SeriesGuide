// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2023 Uwe Trottmann <uwe@uwetrottmann.com>

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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.DialogManageListsBinding
import com.battlelancer.seriesguide.databinding.ItemListCheckedBinding
import com.battlelancer.seriesguide.lists.ManageListsDialogFragmentViewModel.ListItem
import com.battlelancer.seriesguide.lists.ManageListsDialogFragmentViewModel.ListWithItem
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.safeShow
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Displays a dialog displaying all user created lists,
 * allowing to add or remove the given show or movie for any.
 */
class ManageListsDialogFragment : AppCompatDialogFragment() {

    private var binding: DialogManageListsBinding? = null

    private val listItem: ListItem by lazy {
        val args = requireArguments()
        val movieTmdbId = args.getInt(ARG_INT_MOVIE_TMDB_ID, 0)
        if (movieTmdbId != 0) {
            ListItem.Movie(movieTmdbId)
        } else {
            ListItem.Show(args.getLong(ARG_LONG_SHOW_ID))
        }
    }

    private val model by viewModels<ManageListsDialogFragmentViewModel> {
        ManageListsDialogFragmentViewModelFactory(requireActivity().application, listItem)
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
        model.listItemDetails.observe(this) { details ->
            if (details == null) {
                Toast.makeText(requireContext(), R.string.database_error, Toast.LENGTH_LONG).show()
                dismiss()
                return@observe
            }

            binding.item.text = details.title

            // For shows: warn if not yet migrated to TMDB data. Not applicable for movies.
            if (listItem is ListItem.Show && details.tmdbId <= 0) {
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
                lifecycleScope.launch {
                    model.saveChanges()
                    dismiss()
                }
            }
            .create()
    }

    private val onItemClickListener = object : ListsWithItemAdapter.ItemClickListener {
        override fun onItemClick(v: View, listItem: ListWithItem) {
            model.setIsItemOnList(listItem.listId, !listItem.isItemOnList)
        }
    }

    private class ListsWithItemAdapter(
        private val itemClickListener: ItemClickListener
    ) : ListAdapter<ListWithItem, ListsWithItemAdapter.ListWithItemViewHolder>(
        ListWithItemDiffCallback()
    ) {

        interface ItemClickListener {
            fun onItemClick(v: View, listItem: ListWithItem)
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
                binding.root.setOnClickListener { view ->
                    listWithItem?.also {
                        itemClickListener.onItemClick(view, it)
                    }
                }
            }

            fun bindTo(listWithItem: ListWithItem?) {
                this.listWithItem = listWithItem
                binding.textViewListItem.text = listWithItem?.listName ?: ""
                binding.checkBoxListItem.isChecked = listWithItem?.isItemOnList ?: false
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
        private const val ARG_INT_MOVIE_TMDB_ID = "movie_tmdb_id"

        private fun newInstance(listItem: ListItem): ManageListsDialogFragment =
            ManageListsDialogFragment().apply {
                arguments = Bundle().apply {
                    when (listItem) {
                        is ListItem.Show -> putLong(ARG_LONG_SHOW_ID, listItem.showId)
                        is ListItem.Movie -> putInt(ARG_INT_MOVIE_TMDB_ID, listItem.movieTmdbId)
                    }
                }
            }

        /**
         * Replaces any currently shown list dialog and shows this in its place.
         * (Does not add it to the back stack.)
         */
        private fun ManageListsDialogFragment.replaceAndShow(fm: FragmentManager): Boolean {
            val ft = fm.beginTransaction()
            fm.findFragmentByTag(TAG)?.also { ft.remove(it) }
            return safeShow(fm, ft, TAG)
        }

        /**
         * Display a dialog which asks if the user wants to add the given show to one or more lists.
         */
        @JvmStatic
        fun showForShow(fm: FragmentManager, showId: Long): Boolean {
            if (showId <= 0) return false
            return newInstance(ListItem.Show(showId)).replaceAndShow(fm)
        }

        /**
         * Display a dialog which asks if the user wants to add the given movie to one or more lists.
         */
        @JvmStatic
        fun showForMovie(fm: FragmentManager, movieTmdbId: Int): Boolean {
            if (movieTmdbId <= 0) return false
            return newInstance(ListItem.Movie(movieTmdbId)).replaceAndShow(fm)
        }
    }
}