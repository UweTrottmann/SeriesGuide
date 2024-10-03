// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2024 Uwe Trottmann

package com.battlelancer.seriesguide.lists

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ItemShowListBinding
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
    private val itemClickListener: SgListItemViewHolder.ItemClickListener
) : ListAdapter<SgListItemWithDetails, SgListItemViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SgListItemViewHolder {
        return SgListItemViewHolder.create(itemClickListener, parent)
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

}

class SgListItemViewHolder(
    private val binding: ItemShowListBinding,
    private val itemClickListener: ItemClickListener
) : RecyclerView.ViewHolder(binding.root) {

    interface ItemClickListener {
        fun onItemClick(anchor: View, item: SgListItemWithDetails)
        fun onMoreOptionsClick(anchor: View, item: SgListItemWithDetails)
        fun onSetWatchedClick(item: SgListItemWithDetails)
    }

    var item: SgListItemWithDetails? = null

    init {
        // item
        binding.root.setOnClickListener { view ->
            item?.let { itemClickListener.onItemClick(view, it) }
        }
        // set watched button
        binding.imageViewShowsSetWatched.apply {
            TooltipCompat.setTooltipText(this, this.contentDescription)
            setOnClickListener {
                item?.let { itemClickListener.onSetWatchedClick(it) }
            }
        }
        // more options button
        binding.root.setOnLongClickListener {
            onMoreOptionsClick()
            true
        }
        binding.imageViewShowListMoreOptions.apply {
            TooltipCompat.setTooltipText(this, this.contentDescription)
            setOnClickListener {
                onMoreOptionsClick()
            }
        }
    }

    private fun onMoreOptionsClick() {
        item?.let { itemClickListener.onMoreOptionsClick(binding.imageViewShowListMoreOptions, it) }
    }

    fun bindTo(item: SgListItemWithDetails?, context: Context) {
        this.item = item

        if (item == null) {
            binding.seriesname.text = null
            return
        }

        binding.seriesname.text = item.title
        binding.favoritedLabel.isVisible = item.favorite
        ImageTools.loadShowPosterResizeCrop(context, binding.showposter, item.posterSmall)

        val isShow = item.type == ListItemTypes.TMDB_SHOW || item.type == ListItemTypes.TVDB_SHOW
        // Hide set watched button for legacy season and episode items
        binding.imageViewShowsSetWatched.isVisible = isShow

        // network, regular day and time, or type for legacy season/episode
        if (isShow) {
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
            val hasNoNextEpisode = fieldValue.isNullOrEmpty()
            binding.imageViewShowsSetWatched.isGone = hasNoNextEpisode
            if (hasNoNextEpisode) {
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

    companion object {
        fun create(
            itemClickListener: ItemClickListener,
            parent: ViewGroup
        ): SgListItemViewHolder = SgListItemViewHolder(
            ItemShowListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            itemClickListener
        )
    }

}
