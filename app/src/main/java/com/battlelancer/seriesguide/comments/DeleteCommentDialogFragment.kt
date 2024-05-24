// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.comments

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.activityViewModels
import com.battlelancer.seriesguide.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Confirms to delete the comment of [TraktCommentsViewModel.commentIdToDelete].
 */
class DeleteCommentDialogFragment : AppCompatDialogFragment() {

    private val model: TraktCommentsViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirmation_delete_comment)
            .setPositiveButton(R.string.action_delete_comment) { _, _ ->
                model.deleteComment()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

}