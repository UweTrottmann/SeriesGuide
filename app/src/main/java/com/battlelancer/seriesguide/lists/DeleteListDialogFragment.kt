// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.lists

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.os.bundleOf
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog to confirm deletion of a list and its items.
 */
class DeleteListDialogFragment : AppCompatDialogFragment() {

    private lateinit var listId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        listId = requireArguments().getString(ARG_LIST_ID)
            ?: throw IllegalArgumentException("$ARG_LIST_ID must be supplied")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val listTitle = SgRoomDatabase.getInstance(requireContext()).sgListHelper()
            .getList(listId)
            ?.name ?: getString(R.string.unknown)

        // Explicitly make negative button cancel (non-destructive action) as the delete list button
        // is the negative action in the originating dialog; so accidentally pressing again in the
        // same region does not do the destructive action.
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(requireContext().getString(R.string.confirm_delete, listTitle))
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                // just dismiss
            }
            .setPositiveButton(R.string.list_remove) { _, _ ->
                ListsTools.deleteList(requireContext(), listId)
            }
            .create()
    }

    companion object {

        private const val ARG_LIST_ID = "list_id"

        fun create(listId: String): DeleteListDialogFragment {
            return DeleteListDialogFragment().apply {
                arguments = bundleOf(ARG_LIST_ID to listId)
            }
        }
    }

}