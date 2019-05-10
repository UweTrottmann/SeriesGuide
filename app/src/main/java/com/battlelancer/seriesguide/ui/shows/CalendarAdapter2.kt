package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.model.EpisodeWithShow
import com.battlelancer.seriesguide.ui.shows.CalendarFragment2ViewModel.CalendarItem

class CalendarAdapter2(
    private val context: Context,
    private val itemClickListener: ItemClickListener
) :
    ListAdapter<CalendarItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    interface ItemClickListener {
        fun onItemClick(episodeTvdbId: Int)
        fun onItemLongClick(anchor: View, episode: EpisodeWithShow)
        fun onItemWatchBoxClick(episode: EpisodeWithShow, isWatched: Boolean)
    }

    override fun getItemViewType(position: Int): Int {
        val isHeader = getItem(position).episode == null
        return if (isHeader) {
            VIEW_TYPE_HEADER
        } else {
            VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> CalendarHeaderViewHolder.create(parent)
            VIEW_TYPE_ITEM -> CalendarItemViewHolder.create(parent, itemClickListener)
            else -> throw IllegalArgumentException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CalendarHeaderViewHolder -> holder.bind(context, getItem(position).headerTime)
            is CalendarItemViewHolder -> holder.bind(context, getItem(position))
            else -> throw IllegalArgumentException("Unknown view holder type")
        }
    }

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ITEM = 1

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CalendarItem>() {
            override fun areItemsTheSame(old: CalendarItem, new: CalendarItem): Boolean =
                // TODO expand
                old.headerTime == new.headerTime
                        && old.episode?.episodeTvdbId == new.episode?.episodeTvdbId

            override fun areContentsTheSame(old: CalendarItem, new: CalendarItem): Boolean {
                return old == new
            }
        }
    }
}