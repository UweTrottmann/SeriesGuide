package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.provider.SgShow2ForLists
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools

class ShowsAdapter(
    private val context: Context,
    private val onItemClickListener: OnItemClickListener
) :
    ListAdapter<ShowsAdapter.ShowItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    interface OnItemClickListener {
        fun onItemClick(anchor: View, showRowId: Long)

        fun onItemMenuClick(anchor: View, show: ShowItem)

        fun onItemSetWatchedClick(show: ShowItem)
    }

    var displayFirstRunHeader: Boolean = false

    override fun submitList(list: MutableList<ShowItem>?) {
        if (displayFirstRunHeader) {
            val modifiedList = list ?: mutableListOf()
            if (!modifiedList.contains(ShowItem.HEADER_FIRST_RUN)) {
                modifiedList.add(0, ShowItem.header())
            }
            super.submitList(modifiedList)
        } else {
            super.submitList(list)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isHeader) {
            VIEW_TYPE_FIRST_RUN
        } else {
            VIEW_TYPE_SHOW_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_FIRST_RUN -> FirstRunViewHolder.create(parent)
            VIEW_TYPE_SHOW_ITEM -> ShowsViewHolder.create(parent, onItemClickListener)
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is FirstRunViewHolder -> return // do nothing
            is ShowsViewHolder -> holder.bind(getItem(position), context)
            else -> throw IllegalArgumentException("Unknown view holder type")
        }
    }

    companion object {
        const val VIEW_TYPE_FIRST_RUN = 1
        const val VIEW_TYPE_SHOW_ITEM = 2

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ShowItem>() {
            override fun areItemsTheSame(old: ShowItem, new: ShowItem): Boolean =
                old.rowId == new.rowId

            override fun areContentsTheSame(old: ShowItem, new: ShowItem): Boolean {
                return old == new
            }
        }
    }

    data class ShowItem(
        val rowId: Long,
        val showTvdbId: Int?,
        val nextEpisodeId: Long,
        val hasNextEpisode: Boolean,
        val isFavorite: Boolean,
        val isHidden: Boolean,
        val name: String,
        val timeAndNetwork: String,
        val episode: String,
        val episodeTime: String?,
        val remainingCount: String?,
        val posterPath: String?,
        val isHeader: Boolean
    ) {

        companion object {
            val HEADER_FIRST_RUN = header()

            fun header(): ShowItem {
                return ShowItem(
                    rowId = 0,
                    showTvdbId = 0,
                    nextEpisodeId = 0,
                    hasNextEpisode = false,
                    isFavorite = false,
                    isHidden = false,
                    name = "",
                    timeAndNetwork = "",
                    episode = "",
                    episodeTime = null,
                    remainingCount = null,
                    posterPath = null,
                    isHeader = true
                )
            }

            fun map(sgShow: SgShow2ForLists, context: Context): ShowItem {
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
                val hasNextEpisode = !TextUtils.isEmpty(fieldValue)
                if (!hasNextEpisode) {
                    // display show status if there is no next episode
                    episodeTime = SgApp.getServicesComponent(context).showTools().getStatus(
                        sgShow.status ?: ShowTools.Status.UNKNOWN
                    )
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
                    sgShow.id,
                    sgShow.tvdbId,
                    sgShow.nextEpisode?.toLongOrNull() ?: 0,
                    hasNextEpisode,
                    sgShow.favorite,
                    sgShow.hidden,
                    sgShow.title,
                    timeAndNetwork,
                    episode,
                    episodeTime,
                    remainingCount,
                    sgShow.posterSmall,
                    false
                )
            }
        }
    }
}