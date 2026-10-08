// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2017 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.battlelancer.seriesguide.databinding.ViewSyncStatusBinding
import com.battlelancer.seriesguide.sync.SyncProgress.SyncEvent

class SyncStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding: ViewSyncStatusBinding

    init {
        orientation = HORIZONTAL

        binding = ViewSyncStatusBinding.inflate(LayoutInflater.from(context), this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding.imageViewSyncStatus.setVisibility(GONE)
    }

    /**
     * If there is progress or a failure result, displays it.
     * Otherwise sets the view [android.view.View.GONE].
     */
    fun setProgress(event: SyncEvent) {
        if (event.isSyncing) {
            binding.progressBarSyncStatus.visibility = VISIBLE
            binding.imageViewSyncStatus.setVisibility(GONE)
            visibility = VISIBLE
        } else {
            // Finished.
            binding.progressBarSyncStatus.visibility = GONE

            if (event.isFinishedWithError) {
                binding.imageViewSyncStatus.setVisibility(VISIBLE)
                visibility = VISIBLE
            } else {
                // Successful.
                binding.imageViewSyncStatus.setVisibility(GONE)
                visibility = GONE
                return  // No need to update status text.
            }
        }
        binding.textViewSyncStatus.text = event.getDescription(context)
    }
}
