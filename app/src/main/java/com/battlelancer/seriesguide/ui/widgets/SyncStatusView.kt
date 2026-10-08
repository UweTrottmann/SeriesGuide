// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2017 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ViewSyncStatusBinding
import com.battlelancer.seriesguide.sync.SyncProgress.SyncEvent

class SyncStatusView(
    context: Context,
    attrs: AttributeSet?
) : LinearLayout(context, attrs) {

    private val binding: ViewSyncStatusBinding

    init {
        orientation = HORIZONTAL

        binding = ViewSyncStatusBinding.inflate(LayoutInflater.from(context), this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding.imageViewSyncStatus.visibility = GONE
    }

    /**
     * Makes this visible and if syncing, displays a progress bar. Otherwise, depending on if there
     * was an error, a status message with a success or failure indicator.
     */
    fun setProgress(event: SyncEvent) {
        visibility = VISIBLE
        if (event.isSyncing) {
            binding.progressBarSyncStatus.visibility = VISIBLE
            binding.imageViewSyncStatus.visibility = GONE
        } else {
            // Not syncing: hide progress bar, show final status
            binding.progressBarSyncStatus.visibility = GONE
            val iconRes: Int = if (event.isFinishedWithError) {
                R.drawable.ic_cancel_red_24dp
            } else {
                R.drawable.ic_check_circle_green_24dp
            }
            val drawable = VectorDrawableCompat.create(context.resources, iconRes, context.theme)
            binding.imageViewSyncStatus.setImageDrawable(drawable)
            binding.imageViewSyncStatus.visibility = VISIBLE
        }
        // Also show the message if successful, there might still be a message, like a show that
        // can no longer be updated.
        binding.textViewSyncStatus.text = event.getDescription(context)
    }
}
