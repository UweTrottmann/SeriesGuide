package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.provider.SgEpisode2WithShow
import com.battlelancer.seriesguide.ui.movies.AutoGridLayoutManager
import com.battlelancer.seriesguide.ui.shows.CalendarFragment2ViewModel.CalendarItem

class CalendarAdapter2(
    private val context: Context,
    private val itemClickListener: ItemClickListener
) : PagingDataAdapter<CalendarItem, RecyclerView.ViewHolder>(DIFF_CALLBACK),
    AutoGridLayoutManager.SpanCountListener {

    interface ItemClickListener {
        fun onItemClick(episodeId: Long)
        fun onItemLongClick(anchor: View, episode: SgEpisode2WithShow)
        fun onItemWatchBoxClick(episode: SgEpisode2WithShow, isWatched: Boolean)
    }

    var isMultiColumn: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return CalendarItemViewHolder(parent, itemClickListener)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = getItem(position)!! // not using placeholders
        val previousPosition = position - 1
        val previousItem = if (previousPosition >= 0) getItem(previousPosition) else null
        when (holder) {
            is CalendarItemViewHolder -> holder.bind(
                context,
                currentItem,
                previousItem,
                isMultiColumn
            )
            else -> throw IllegalArgumentException("Unknown view holder type")
        }
    }

    override fun onSetSpanCount(spanCount: Int) {
        isMultiColumn = spanCount > 1
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CalendarItem>() {
            override fun areItemsTheSame(old: CalendarItem, new: CalendarItem): Boolean =
                old.episode.id == new.episode.id

            override fun areContentsTheSame(old: CalendarItem, new: CalendarItem): Boolean {
                return old.episode == new.episode
            }
        }
    }
}