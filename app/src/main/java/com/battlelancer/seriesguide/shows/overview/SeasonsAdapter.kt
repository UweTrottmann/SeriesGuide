// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.overview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ItemSeasonBinding
import com.battlelancer.seriesguide.shows.database.SgSeason2
import com.battlelancer.seriesguide.shows.overview.SeasonsViewModel.SgSeasonWithStats
import com.battlelancer.seriesguide.util.TextTools
import com.uwetrottmann.androidutils.AndroidUtils

/**
 * Provides a list of seasons.
 */
class SeasonsAdapter(
    private val context: Context,
    private val itemClickListener: ItemClickListener
) : ListAdapter<SgSeasonWithStats, SeasonsAdapter.ViewHolder>(SgSeason2DiffCallback()) {

    interface ItemClickListener {
        fun onItemClick(v: View, seasonRowId: Long)
        fun onPopupMenuClick(v: View, seasonRowId: Long)
    }

    class ViewHolder(
        private val binding: ItemSeasonBinding,
        itemClickListener: ItemClickListener
    ) : RecyclerView.ViewHolder(binding.root) {

        private val isRtlLayout = AndroidUtils.isRtlLayout
        private var season: SgSeason2? = null

        init {
            itemView.setOnClickListener { view ->
                season?.also {
                    itemClickListener.onItemClick(view, it.id)
                }
            }
            binding.imageViewContextMenu.setOnClickListener { view ->
                season?.also {
                    itemClickListener.onPopupMenuClick(view, it.id)
                }
            }
        }

        fun bindTo(seasonWithStats: SgSeasonWithStats, context: Context) {
            val season = seasonWithStats.season.also { this.season = it }
            val stats = seasonWithStats.stats

            // Title
            binding.textViewSeasonTitle.text = SeasonTools.getSeasonString(context, season.number)

            // Not watched episodes by type.
            val released = stats.notWatchedReleased
            val toBeReleased = stats.notWatchedToBeReleased
            val noRelease = stats.notWatchedNoRelease

            // Progress bar
            val max = stats.total
            val progress = max - released - toBeReleased - noRelease
            binding.progressBarSeason.apply {
                this.max = max
                if (AndroidUtils.isNougatOrHigher) {
                    setProgress(progress, true)
                } else {
                    setProgress(progress)
                }
            }
            // Progress text
            val res = context.resources
            binding.textViewSeasonProgress.text = if (isRtlLayout) {
                res.getString(R.string.format_progress_and_total, max, progress)
            } else {
                res.getString(R.string.format_progress_and_total, progress, max)
            }

            // Skipped and collected indicator
            val skipped = stats.skipped
            val collected = stats.collected
            binding.imageViewSeasonSkipped.isVisible = skipped in 1..<max
            binding.imageViewSeasonSkippedAll.isVisible = skipped > 0 && skipped == max
            binding.imageViewSeasonCollected.isVisible = collected in 1..<max
            binding.imageViewSeasonCollectedAll.isVisible = collected > 0 && collected == max

            // Status text
            val countText = StringBuilder()
            val watchable = released + noRelease
            if (watchable > 0) {
                // some released or other episodes left to watch
                TextViewCompat.setTextAppearance(
                    binding.textViewSeasonWatchCount,
                    R.style.TextAppearance_SeriesGuide_Caption_Narrow
                )
                if (released > 0) {
                    countText.append(TextTools.getRemainingEpisodes(res, released))
                }
            } else {
                TextViewCompat.setTextAppearance(
                    binding.textViewSeasonWatchCount,
                    R.style.TextAppearance_SeriesGuide_Caption_Narrow_Dim
                )
                // Ensure there are no episodes to watch not just because all of them are not
                // watchable or skipped.
                if (toBeReleased + noRelease + skipped != max) {
                    // all watched
                    countText.append(context.getString(R.string.season_allwatched))
                }
            }
            if (noRelease > 0) {
                // there are unwatched episodes without a release date
                if (countText.isNotEmpty()) countText.append(" · ")
                countText.append(
                    res.getQuantityString(
                        R.plurals.other_episodes_plural,
                        noRelease, noRelease
                    )
                )
            }
            if (toBeReleased > 0) {
                // there are not yet released episodes
                if (countText.isNotEmpty()) countText.append(" · ")
                countText.append(
                    res.getQuantityString(
                        R.plurals.not_released_episodes_plural,
                        toBeReleased, toBeReleased
                    )
                )
            }
            if (skipped > 0) {
                if (countText.isNotEmpty()) countText.append(" · ")
                countText.append(
                    res.getQuantityString(
                        R.plurals.skipped_episodes_plural,
                        skipped, skipped
                    )
                )
            }
            if (collected > 0) {
                if (countText.isNotEmpty()) countText.append(" · ")
                countText.append(
                    res.getQuantityString(
                        R.plurals.collected_episodes_plural,
                        collected, collected
                    )
                )
            }
            binding.textViewSeasonWatchCount.text = countText
        }

        companion object {
            fun inflate(
                parent: ViewGroup,
                itemClickListener: ItemClickListener
            ): ViewHolder {
                return ViewHolder(
                    ItemSeasonBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                    itemClickListener
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.inflate(parent, itemClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindTo(getItem(position), context)
    }

}

class SgSeason2DiffCallback : DiffUtil.ItemCallback<SgSeasonWithStats>() {
    override fun areItemsTheSame(oldItem: SgSeasonWithStats, newItem: SgSeasonWithStats): Boolean =
        oldItem.season.id == newItem.season.id

    override fun areContentsTheSame(
        oldItem: SgSeasonWithStats,
        newItem: SgSeasonWithStats
    ): Boolean = oldItem == newItem
}
