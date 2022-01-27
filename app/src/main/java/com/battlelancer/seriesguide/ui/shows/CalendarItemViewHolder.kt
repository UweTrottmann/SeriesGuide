package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.battlelancer.seriesguide.widgets.WatchedBox
import java.util.Date

class CalendarItemViewHolder(
    parent: ViewGroup,
    itemClickListener: CalendarAdapter2.ItemClickListener
) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context).inflate(
        R.layout.item_calendar,
        parent,
        false
    )
) {

    private val headerTextView: TextView = itemView.findViewById(R.id.textViewGridHeader)
    private val itemContainer: ViewGroup = itemView.findViewById(R.id.constraintLayoutCalendar)
    private val showTextView: TextView = itemView.findViewById(R.id.textViewActivityShow)
    private val episodeTextView: TextView = itemView.findViewById(R.id.textViewActivityEpisode)
    private val collected: View = itemView.findViewById(R.id.imageViewActivityCollected)
    private val watchedBox: WatchedBox = itemView.findViewById(R.id.watchedBoxActivity)
    private val info: TextView = itemView.findViewById(R.id.textViewActivityInfo)
    private val timestamp: TextView = itemView.findViewById(R.id.textViewActivityTimestamp)
    private val poster: ImageView = itemView.findViewById(R.id.imageViewActivityPoster)

    private var item: CalendarFragment2ViewModel.CalendarItem? = null

    init {
        itemContainer.setOnClickListener {
            item?.episode?.let {
                itemClickListener.onItemClick(it.id)
            }
        }
        itemContainer.setOnLongClickListener {
            item?.episode?.let {
                itemClickListener.onItemLongClick(itemView, it)
            }
            true
        }
        watchedBox.setOnClickListener {
            item?.episode?.let {
                itemClickListener.onItemWatchBoxClick(
                    it,
                    EpisodeTools.isWatched(watchedBox.episodeFlag)
                )
            }
        }

        TooltipCompat.setTooltipText(watchedBox, watchedBox.contentDescription)
    }

    fun bind(
        context: Context,
        item: CalendarFragment2ViewModel.CalendarItem,
        previousItem: CalendarFragment2ViewModel.CalendarItem?,
        multiColumn: Boolean
    ) {
        this.item = item

        // optional header
        val isShowingHeader = previousItem == null || previousItem.headerTime != item.headerTime
        if (multiColumn) {
            // in a multi-column layout it looks nicer if all items are inset by header height
            headerTextView.isInvisible = !isShowingHeader
        } else {
            headerTextView.isGone = !isShowingHeader
        }
        headerTextView.text = if (isShowingHeader) {
            // display headers like "Mon in 3 days", also "today" when applicable
            TimeTools.formatToLocalDayAndRelativeWeek(context, Date(item.headerTime))
        } else {
            null
        }

        val episode = item.episode

        // show title
        showTextView.text = episode.seriestitle

        // episode number and title
        val hideTitle =
            EpisodeTools.isUnwatched(episode.watched) && DisplaySettings.preventSpoilers(context)
        episodeTextView.text = TextTools.getNextEpisodeString(
            context,
            episode.season,
            episode.episodenumber,
            if (hideTitle) null else episode.episodetitle
        )

        // timestamp, absolute time and network
        val releaseTime = episode.episode_firstairedms
        val time = if (releaseTime != -1L) {
            val actualRelease = TimeTools.applyUserOffset(context, releaseTime)
            // timestamp
            timestamp.text = if (DisplaySettings.isDisplayExactDate(context)) {
                TimeTools.formatToLocalDateShort(context, actualRelease)
            } else {
                TimeTools.formatToLocalRelativeTime(context, actualRelease)
            }
            // release time of this episode
            TimeTools.formatToLocalTime(context, actualRelease)
        } else {
            timestamp.text = null
            null
        }
        info.text = TextTools.dotSeparate(episode.network, time)

        // watched box
        val episodeFlag = episode.watched
        watchedBox.episodeFlag = episodeFlag
        val watched = EpisodeTools.isWatched(episodeFlag)
        watchedBox.contentDescription =
            context.getString(if (watched) R.string.action_unwatched else R.string.action_watched)

        // collected indicator
        collected.isGone = !episode.episode_collected

        // set poster
        ImageTools.loadShowPosterResizeSmallCrop(context, poster, episode.series_poster_small)
    }

}