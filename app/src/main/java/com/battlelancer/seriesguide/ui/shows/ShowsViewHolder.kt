package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.model.SgShow
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools

class ShowsViewHolder(
    itemView: View,
    onItemClickListener: ShowsRecyclerAdapter.OnItemClickListener,
    private val drawableStar: VectorDrawableCompat,
    private val drawableStarZero: VectorDrawableCompat
) : RecyclerView.ViewHolder(itemView) {

    private val name: TextView = itemView.findViewById(R.id.seriesname)
    private val timeAndNetwork: TextView = itemView.findViewById(R.id.textViewShowsTimeAndNetwork)
    private val episode: TextView = itemView.findViewById(R.id.TextViewShowListNextEpisode)
    private val episodeTime: TextView = itemView.findViewById(R.id.episodetime)
    private val remainingCount: TextView = itemView.findViewById(R.id.textViewShowsRemaining)
    private val poster: ImageView = itemView.findViewById(R.id.showposter)
    private val favorited: ImageView = itemView.findViewById(R.id.favoritedLabel)
    private val contextMenu: ImageView = itemView.findViewById(R.id.imageViewShowsContextMenu)

    var showTvdbId: Int = 0
    var episodeTvdbId: Int = 0
    var isFavorited: Boolean = false
    var isHidden: Boolean = false

    init {
        // item
        itemView.setOnClickListener {
            onItemClickListener.onItemClick(it, showTvdbId)
        }
        // favorite star
        favorited.setOnClickListener {
            onItemClickListener.onItemFavoriteClick(showTvdbId, !isFavorited)
        }
        // context menu
        contextMenu.setOnClickListener { v ->
            onItemClickListener.onItemMenuClick(v, this)
        }
    }

    fun bind(context: Context, show: SgShow) {
        showTvdbId = show.tvdbId
        isFavorited = show.favorite

        name.text = show.title

        setFavoriteState()
        setRemainingCount(show.unwatchedCount)

        val weekDay = show.releaseWeekDay
        val time = show.releaseTime
        val timeZone = show.releaseTimeZone
        val country = show.releaseCountry
        val network = show.network
        val releaseTimeShow = if (time != -1) {
            TimeTools.getShowReleaseDateTime(context, time, weekDay, timeZone, country, network)
        } else {
            null
        }

        // next episode info
        val fieldValue = show.nextText
        if (TextUtils.isEmpty(fieldValue)) {
            // display show status if there is no next episode
            episodeTime.text = ShowTools.getStatus(context, show.status?.toInt() ?: -1)
            episode.text = ""
        } else {
            episode.text = fieldValue

            val releaseTimeEpisode = TimeTools.applyUserOffset(context, show.nextAirdateMs)
            val displayExactDate = DisplaySettings.isDisplayExactDate(context)
            val dateTime = if (displayExactDate) {
                TimeTools.formatToLocalDateShort(context, releaseTimeEpisode)
            } else {
                TimeTools.formatToLocalRelativeTime(context, releaseTimeEpisode)
            }
            if (TimeTools.isSameWeekDay(releaseTimeEpisode, releaseTimeShow, weekDay)) {
                // just display date
                episodeTime.text = dateTime
            } else {
                // display date and explicitly day
                episodeTime.text = context.getString(
                    R.string.format_date_and_day,
                    dateTime, TimeTools.formatToLocalDay(releaseTimeEpisode)
                )
            }
        }

        // network, day and time
        timeAndNetwork.text = TextTools.networkAndTime(context, releaseTimeShow, weekDay, network)

        // set poster
        TvdbImageTools.loadShowPosterResizeCrop(poster.context, poster, show.poster)

        // context menu
        episodeTvdbId = show.nextEpisode?.toInt() ?: 0
        isHidden = show.hidden
    }

    private fun setFavoriteState() {
        favorited.setImageDrawable(if (isFavorited) drawableStar else drawableStarZero)
        favorited.contentDescription = favorited.context
            .getString(if (isFavorited) R.string.context_unfavorite else R.string.context_favorite)
    }

    private fun setRemainingCount(unwatched: Int) {
        if (unwatched > 0) {
            remainingCount.text = remainingCount.resources
                .getQuantityString(R.plurals.remaining_episodes_plural, unwatched, unwatched)
            remainingCount.visibility = View.VISIBLE
        } else {
            remainingCount.text = null
            remainingCount.visibility = View.GONE
        }
    }

    companion object {

        fun create(
            parent: ViewGroup,
            onItemClickListener: ShowsRecyclerAdapter.OnItemClickListener,
            drawableStar: VectorDrawableCompat,
            drawableStarZero: VectorDrawableCompat
        ): ShowsViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_show, parent, false)
            return ShowsViewHolder(v, onItemClickListener, drawableStar, drawableStarZero)
        }
    }

}