package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.model.EpisodeWithShow
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.widgets.WatchedBox

class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val show: TextView = itemView.findViewById(R.id.textViewActivityShow)
    private val episode: TextView = itemView.findViewById(R.id.textViewActivityEpisode)
    private val collected: View = itemView.findViewById(R.id.imageViewActivityCollected)
    private val watchedBox: WatchedBox = itemView.findViewById(R.id.watchedBoxActivity)
    private val info: TextView = itemView.findViewById(R.id.textViewActivityInfo)
    private val timestamp: TextView = itemView.findViewById(R.id.textViewActivityTimestamp)
    private val poster: ImageView = itemView.findViewById(R.id.imageViewActivityPoster)

    private var item: EpisodeWithShow? = null

    fun bind(item: EpisodeWithShow, context: Context) {
        this.item = item

        // TODO expand
        show.text = item.seriestitle

        val hideTitle =
            EpisodeTools.isUnwatched(item.watched) && DisplaySettings.preventSpoilers(context)
        episode.text = TextTools.getNextEpisodeString(
            context,
            item.season,
            item.episodenumber,
            if (hideTitle) null else item.episodetitle
        )

        collected.isGone = !item.episode_collected

        // set poster
        TvdbImageTools.loadShowPosterResizeSmallCrop(
            context, poster,
            TvdbImageTools.smallSizeUrl(item.poster)
        )
    }

    companion object {

        fun create(parent: ViewGroup): CalendarViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_calendar, parent, false)
            return CalendarViewHolder(view)
        }

    }

}