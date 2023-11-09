// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.lists

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ItemShowBinding
import com.battlelancer.seriesguide.lists.database.SgListItemWithDetails
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.shows.overview.SeasonTools
import com.battlelancer.seriesguide.shows.tools.ShowStatus
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.battlelancer.seriesguide.util.TimeTools.formatWithDeviceZoneToDayAndTime
import org.threeten.bp.Instant

class SgListItemAdapter(
    private val context: Context,
    private val onItemClickListener: OnItemClickListener
) : ListAdapter<SgListItemWithDetails, SgListItemViewHolder>(DIFF_CALLBACK) {

    private val drawableStar = VectorDrawableCompat
        .create(context.resources, R.drawable.ic_star_black_24dp, context.theme)!!
    private val drawableStarZero = VectorDrawableCompat
        .create(context.resources, R.drawable.ic_star_border_black_24dp, context.theme)!!

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SgListItemViewHolder {
        return SgListItemViewHolder(
            ItemShowBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ), onItemClickListener,
            drawableStar,
            drawableStarZero
        )
    }

    override fun onBindViewHolder(holder: SgListItemViewHolder, position: Int) {
        holder.bindTo(getItem(position), context)
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SgListItemWithDetails>() {
            override fun areItemsTheSame(
                oldItem: SgListItemWithDetails,
                newItem: SgListItemWithDetails
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: SgListItemWithDetails,
                newItem: SgListItemWithDetails
            ): Boolean = oldItem == newItem
        }
    }

    interface OnItemClickListener {
        fun onItemClick(anchor: View, item: SgListItemWithDetails)
        fun onMenuClick(anchor: View, item: SgListItemWithDetails)
        fun onFavoriteClick(showId: Long, isFavorite: Boolean)
    }

}

class SgListItemViewHolder(
    private val binding: ItemShowBinding,
    onItemClickListener: SgListItemAdapter.OnItemClickListener,
    private val drawableStar: Drawable,
    private val drawableStarZero: Drawable
) : RecyclerView.ViewHolder(binding.root) {

    var item: SgListItemWithDetails? = null

    init {
        // item
        binding.root.setOnClickListener { view ->
            item?.let { onItemClickListener.onItemClick(view, it) }
        }
        // favorite star
        binding.favoritedLabel.setOnClickListener {
            item?.let { onItemClickListener.onFavoriteClick(it.showId, !it.favorite) }
        }
        // context menu
        binding.imageViewShowsContextMenu.setOnClickListener { view ->
            item?.let { onItemClickListener.onMenuClick(view, it) }
        }
    }

    fun bindTo(item: SgListItemWithDetails?, context: Context) {
        this.item = item

        if (item == null) {
            binding.seriesname.text = null
            binding.favoritedLabel.setImageDrawable(null)
            return
        }

        // show title
        binding.seriesname.text = item.title

        // favorite label
        binding.favoritedLabel.apply {
            setImageDrawable(if (item.favorite) drawableStar else drawableStarZero)
            contentDescription = context.getString(
                if (item.favorite) {
                    R.string.context_unfavorite
                } else {
                    R.string.context_favorite
                }
            )
        }

        // poster
        ImageTools.loadShowPosterResizeCrop(context, binding.showposter, item.posterSmall)

        // network, regular day and time, or type for legacy season/episode
        if (item.type == ListItemTypes.TMDB_SHOW || item.type == ListItemTypes.TVDB_SHOW) {
            // show details
            val weekDay = item.releaseWeekDayOrDefault
            val network = item.network

            val releaseTimeShow = TimeTools.getReleaseDateTime(context, item)

            binding.textViewShowsTimeAndNetwork.text = TextTools.dotSeparate(
                network,
                releaseTimeShow?.formatWithDeviceZoneToDayAndTime()
            )

            // next episode info
            val fieldValue: String? = item.nextText
            if (fieldValue.isNullOrEmpty()) {
                // display show status if there is no next episode
                binding.episodetime.text = ShowStatus.getStatus(context, item.statusOrUnknown)
                binding.TextViewShowListNextEpisode.text = null
            } else {
                binding.TextViewShowListNextEpisode.text = fieldValue

                val releaseTimeEpisode = TimeTools.applyUserOffset(context, item.nextAirdateMs)
                val displayExactDate = DisplaySettings.isDisplayExactDate(context)
                val dateTime = if (displayExactDate) {
                    TimeTools.formatToLocalDateShort(context, releaseTimeEpisode)
                } else {
                    TimeTools.formatToLocalRelativeTime(context, releaseTimeEpisode)
                }
                if (TimeTools.isSameWeekDay(
                        Instant.ofEpochMilli(releaseTimeEpisode.time),
                        releaseTimeShow?.toInstant(),
                        weekDay
                    )) {
                    // just display date
                    binding.episodetime.text = dateTime
                } else {
                    // display date and explicitly day
                    binding.episodetime.text = context.getString(
                        R.string.format_date_and_day, dateTime,
                        TimeTools.formatToLocalDay(releaseTimeEpisode)
                    )
                }
            }

            // remaining count
            setRemainingCount(item.unwatchedCount)
        } else if (item.type == ListItemTypes.SEASON) {
            binding.textViewShowsTimeAndNetwork.setText(R.string.season)
            binding.episodetime.text = null
            binding.textViewShowsRemaining.visibility = View.GONE

            // Note: Running query in adapter, but it's for legacy items, so fine for now.
            val sesaonTvdbId: Int = item.itemRefId.toIntOrNull() ?: 0
            val seasonNumbersOrNull = SgRoomDatabase.getInstance(context).sgSeason2Helper()
                .getSeasonNumbersByTvdbId(sesaonTvdbId)
            if (seasonNumbersOrNull != null) {
                binding.TextViewShowListNextEpisode.text = SeasonTools.getSeasonString(
                    context,
                    seasonNumbersOrNull.number
                )
            } else {
                binding.TextViewShowListNextEpisode.setText(R.string.unknown)
            }
        } else if (item.type == ListItemTypes.EPISODE) {
            binding.textViewShowsTimeAndNetwork.setText(R.string.episode)
            binding.textViewShowsRemaining.visibility = View.GONE

            // Note: Running query in adapter, but it's for legacy items, so fine for now.
            val episodeTvdbId: Int = item.itemRefId.toIntOrNull() ?: 0
            val helper = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
            val episodeIdOrZero = helper.getEpisodeIdByTvdbId(episodeTvdbId)
            val episodeInfo = if (episodeIdOrZero > 0) {
                helper.getEpisodeInfo(episodeIdOrZero)
            } else null

            if (episodeInfo != null) {
                binding.TextViewShowListNextEpisode.text = TextTools.getNextEpisodeString(
                    context,
                    episodeInfo.season,
                    episodeInfo.episodenumber,
                    episodeInfo.title
                )
                val releaseTime = episodeInfo.firstReleasedMs
                if (releaseTime != -1L) {
                    // "in 15 mins (Fri)"
                    val actualRelease = TimeTools.applyUserOffset(context, releaseTime)
                    binding.episodetime.text = context.getString(
                        R.string.format_date_and_day,
                        TimeTools.formatToLocalRelativeTime(context, actualRelease),
                        TimeTools.formatToLocalDay(actualRelease)
                    )
                }
            } else {
                binding.TextViewShowListNextEpisode.setText(R.string.unknown)
                binding.episodetime.text = null
            }
        }
    }

    private fun setRemainingCount(unwatched: Int) {
        val textView = binding.textViewShowsRemaining
        textView.text = TextTools.getRemainingEpisodes(textView.resources, unwatched)
        if (unwatched > 0) {
            textView.visibility = View.VISIBLE
        } else {
            textView.visibility = View.GONE
        }
    }

}
