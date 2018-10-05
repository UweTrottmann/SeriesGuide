package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.content.res.Resources
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.model.SgShow
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.battlelancer.seriesguide.util.ViewTools

class ShowsRecyclerAdapter(
    private val context: Context,
    drawableTheme: Resources.Theme,
    private val onItemClickListener: OnItemClickListener
) :
    ListAdapter<ShowsRecyclerAdapter.ShowItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    private val drawableStar: VectorDrawableCompat = ViewTools.vectorIconActive(
        context, drawableTheme,
        R.drawable.ic_star_black_24dp
    )
    private val drawableStarZero: VectorDrawableCompat = ViewTools.vectorIconActive(
        context, drawableTheme,
        R.drawable.ic_star_border_black_24dp
    )

    interface OnItemClickListener {
        fun onItemClick(anchor: View, showTvdbId: Int)

        fun onItemMenuClick(anchor: View, show: ShowItem)

        fun onItemFavoriteClick(showTvdbId: Int, isFavorite: Boolean)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ShowsViewHolder.create(parent, onItemClickListener, drawableStar, drawableStarZero)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ShowsViewHolder) {
            holder.bind(getItem(position), context)
        } else {
            throw IllegalArgumentException("Unknown view holder type")
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ShowItem>() {
            override fun areItemsTheSame(old: ShowItem, new: ShowItem): Boolean =
                old.showTvdbId == new.showTvdbId

            override fun areContentsTheSame(old: ShowItem, new: ShowItem): Boolean {
                return old == new
            }
        }
    }

    data class ShowItem(
        val showTvdbId: Int,
        val episodeTvdbId: Int,
        val isFavorite: Boolean,
        val isHidden: Boolean,
        val name: String,
        val timeAndNetwork: String,
        val episode: String,
        val episodeTime: String?,
        val remainingCount: String?,
        val posterPath: String?
    ) {

        companion object {
            fun map(sgShow: SgShow, context: Context): ShowItem {
                val episodeTvdbId = sgShow.nextEpisode?.toIntOrNull() ?: 0

                val remainingCount = if (sgShow.unwatchedCount > 0) {
                    context.resources.getQuantityString(
                        R.plurals.remaining_episodes_plural,
                        sgShow.unwatchedCount,
                        sgShow.unwatchedCount
                    )
                } else {
                    null
                }

                val weekDay = sgShow.releaseWeekDay
                val time = sgShow.releaseTime
                val timeZone = sgShow.releaseTimeZone
                val country = sgShow.releaseCountry
                val network = sgShow.network
                val releaseTimeShow = if (time != -1) {
                    TimeTools.getShowReleaseDateTime(
                        context,
                        time,
                        weekDay,
                        timeZone,
                        country,
                        network
                    )
                } else {
                    null
                }

                // next episode info
                val episodeTime: String?
                val episode: String
                val fieldValue = sgShow.nextText
                if (TextUtils.isEmpty(fieldValue)) {
                    // display show status if there is no next episode
                    episodeTime = ShowTools.getStatus(context, sgShow.status?.toInt() ?: -1)
                    episode = ""
                } else {
                    episode = fieldValue

                    val releaseTimeEpisode =
                        TimeTools.applyUserOffset(context, sgShow.nextAirdateMs)
                    val displayExactDate = DisplaySettings.isDisplayExactDate(context)
                    val dateTime = if (displayExactDate) {
                        TimeTools.formatToLocalDateShort(context, releaseTimeEpisode)
                    } else {
                        TimeTools.formatToLocalRelativeTime(context, releaseTimeEpisode)
                    }
                    episodeTime = if (TimeTools.isSameWeekDay(
                            releaseTimeEpisode,
                            releaseTimeShow,
                            weekDay
                        )
                    ) {
                        dateTime // just display date
                    } else {
                        // display date and explicitly day
                        context.getString(
                            R.string.format_date_and_day,
                            dateTime, TimeTools.formatToLocalDay(releaseTimeEpisode)
                        )
                    }
                }

                val timeAndNetwork =
                    TextTools.networkAndTime(context, releaseTimeShow, weekDay, network)

                return ShowItem(
                    sgShow.tvdbId,
                    episodeTvdbId,
                    sgShow.favorite,
                    sgShow.hidden,
                    sgShow.title,
                    timeAndNetwork,
                    episode,
                    episodeTime,
                    remainingCount,
                    sgShow.poster
                )
            }
        }
    }
}