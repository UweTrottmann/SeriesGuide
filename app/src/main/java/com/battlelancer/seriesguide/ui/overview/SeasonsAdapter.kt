package com.battlelancer.seriesguide.ui.overview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ItemSeasonBinding
import com.battlelancer.seriesguide.model.SgSeason2
import com.battlelancer.seriesguide.util.SeasonTools
import com.uwetrottmann.androidutils.AndroidUtils

/**
 * Provides a list of seasons.
 */
class SeasonsAdapter(
    private val context: Context,
    private val itemClickListener: ItemClickListener
) : ListAdapter<SgSeason2, SeasonsAdapter.ViewHolder>(SgSeason2DiffCallback()) {

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

        fun bindTo(season: SgSeason2, context: Context) {
            this.season = season

            // Title
            binding.textViewSeasonTitle.text = SeasonTools.getSeasonString(context, season.number)

            // Not watched episodes by type.
            val released = season.notWatchedReleased
            val toBeReleased = season.notWatchedToBeReleased
            val noRelease = season.notWatchedNoRelease

            // Progress bar
            val max = season.total
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

            // Skipped indicator
            binding.imageViewSeasonSkipped.isGone = !SeasonTools.hasSkippedTag(season.tags)

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
                    countText.append(
                        res.getQuantityString(
                            R.plurals.remaining_episodes_plural,
                            released, released
                        )
                    )
                }
            } else {
                TextViewCompat.setTextAppearance(
                    binding.textViewSeasonWatchCount,
                    R.style.TextAppearance_SeriesGuide_Caption_Narrow_Dim
                )
                // ensure at least 1 watched episode by comparing amount of unwatched to total
                if (toBeReleased + noRelease != max) {
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
            binding.textViewSeasonWatchCount.text = countText

            // Context menu

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

class SgSeason2DiffCallback : DiffUtil.ItemCallback<SgSeason2>() {
    override fun areItemsTheSame(oldItem: SgSeason2, newItem: SgSeason2): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: SgSeason2, newItem: SgSeason2): Boolean =
        oldItem == newItem
}
