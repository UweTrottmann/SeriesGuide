// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.lists

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.FragmentManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider
import com.battlelancer.seriesguide.databinding.DialogListsSortBinding
import com.battlelancer.seriesguide.lists.ListsDistillationSettings.ListsSortOrder
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.util.safeShow
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.greenrobot.eventbus.EventBus

/**
 * Dialog to choose the sort order for list items and the ignore articles setting for sorting by
 * title.
 */
class ListsSortDialogFragment : AppCompatDialogFragment() {

    private var binding: DialogListsSortBinding? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogListsSortBinding.inflate(layoutInflater)
        this.binding = binding

        val context = requireContext()

        // Set initial state from saved settings
        val sortOrder = ListsDistillationSettings.getSortOrder(context)
        val radioButtonId = when (sortOrder) {
            ListsSortOrder.TITLE_ALPHABETICAL -> R.id.radioListsSortTitle
            ListsSortOrder.LATEST_RELEASE_DATE -> R.id.radioListsSortLatestReleaseDate
            ListsSortOrder.OLDEST_RELEASE_DATE -> R.id.radioListsSortOldestReleaseDate
            ListsSortOrder.LAST_WATCHED -> R.id.radioListsSortLastWatched
            ListsSortOrder.LEAST_REMAINING_EPISODES -> R.id.radioListsSortRemaining
        }
        binding.radioGroupListsSort.check(radioButtonId)
        binding.checkboxListsSortIgnoreArticles.isChecked =
            DisplaySettings.isSortOrderIgnoringArticles(context)

        // Apply sort order change immediately when a radio button is selected
        binding.radioGroupListsSort.setOnCheckedChangeListener { _, checkedId ->
            val newSortOrder = when (checkedId) {
                R.id.radioListsSortTitle -> ListsSortOrder.TITLE_ALPHABETICAL
                R.id.radioListsSortLatestReleaseDate -> ListsSortOrder.LATEST_RELEASE_DATE
                R.id.radioListsSortOldestReleaseDate -> ListsSortOrder.OLDEST_RELEASE_DATE
                R.id.radioListsSortLastWatched -> ListsSortOrder.LAST_WATCHED
                R.id.radioListsSortRemaining -> ListsSortOrder.LEAST_REMAINING_EPISODES
                else -> throw IllegalStateException("Unknown checkedId $checkedId")
            }
            ListsDistillationSettings.saveSortOrder(context, newSortOrder)

            // Post event, so all active list fragments can react
            EventBus.getDefault().post(ListsDistillationSettings.ListsSortOrderChangedEvent())
        }

        // Apply ignore articles change immediately when the check box is toggled
        binding.checkboxListsSortIgnoreArticles.setOnCheckedChangeListener { _, isChecked ->
            DisplaySettings.saveSortOrderIgnoringArticles(context, isChecked)

            // Refresh all list widgets
            ListWidgetProvider.notifyDataChanged(context)

            // Post event, so all active list fragments can react
            EventBus.getDefault().post(ListsDistillationSettings.ListsSortOrderChangedEvent())
        }

        return MaterialAlertDialogBuilder(context)
            .setTitle(R.string.sort)
            .setView(binding.root)
            .setPositiveButton(R.string.dismiss) { _, _ -> dismiss() }
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        private const val TAG = "listsSortDialog"

        fun show(fragmentManager: FragmentManager) {
            ListsSortDialogFragment().safeShow(fragmentManager, TAG)
        }
    }
}
