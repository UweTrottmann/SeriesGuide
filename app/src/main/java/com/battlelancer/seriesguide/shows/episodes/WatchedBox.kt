// SPDX-License-Identifier: Apache-2.0
// Copyright 2011-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.episodes

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.TooltipCompat
import com.battlelancer.seriesguide.R

/**
 * Image view that displays a watched, skipped or watch icon depending on the given episode flag.
 *
 * Provides a content description and tooltip out of the box.
 */
class WatchedBox(context: Context, attrs: AttributeSet?) : AppCompatImageView(context, attrs) {

    /**
     * An [EpisodeFlags] flag.
     */
    var episodeFlag = 0
        set(value) {
            EpisodeTools.validateFlags(value)
            field = value
            updateStateImage()
            updateContentDescription()
        }

    init {
        if (isInEditMode) {
            episodeFlag = EpisodeFlags.UNWATCHED
        }
    }

    private fun updateStateImage() {
        when (episodeFlag) {
            EpisodeFlags.WATCHED -> {
                setImageResource(R.drawable.ic_watched_24dp)
            }

            EpisodeFlags.SKIPPED -> {
                setImageResource(R.drawable.ic_skipped_24dp)
            }

            EpisodeFlags.UNWATCHED -> {
                setImageResource(R.drawable.ic_watch_black_24dp)
            }

            else -> {
                setImageResource(R.drawable.ic_watch_black_24dp)
            }
        }
    }

    private fun updateContentDescription() {
        val watched = EpisodeTools.isWatched(episodeFlag)
        contentDescription =
            context.getString(if (watched) R.string.action_unwatched else R.string.action_watched)
        // Re-set tooltip text after updating
        TooltipCompat.setTooltipText(this, contentDescription)
    }
}
