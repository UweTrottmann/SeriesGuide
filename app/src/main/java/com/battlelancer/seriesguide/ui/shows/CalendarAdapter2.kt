package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.paging.AsyncPagedListDiffer
import androidx.paging.PagedList
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.model.EpisodeWithShow
import com.battlelancer.seriesguide.ui.shows.CalendarFragment2ViewModel.CalendarItem

class CalendarAdapter2(
    private val context: Context,
    private val itemClickListener: ItemClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface ItemClickListener {
        fun onItemClick(episodeTvdbId: Int)
        fun onItemLongClick(anchor: View, episode: EpisodeWithShow)
        fun onItemWatchBoxClick(episode: EpisodeWithShow, isWatched: Boolean)
    }

    private val differ = AsyncPagedListDiffer(this, DIFF_CALLBACK)

    fun submitList(pagedList: PagedList<CalendarItem>) {
        differ.submitList(pagedList)
    }

    private fun getItem(position: Int): CalendarItem {
        return differ.getItem(position)!! // not using placeholders
    }

    override fun getItemCount(): Int {
        return differ.itemCount
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        val isHeader = item != null && item.episode == null
        return if (isHeader) {
            VIEW_TYPE_HEADER
        } else {
            VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> CalendarHeaderViewHolder.create(parent)
            VIEW_TYPE_ITEM -> CalendarItemViewHolder(parent, itemClickListener)
            else -> throw IllegalArgumentException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = getItem(position)
        val previousPosition = position - 1
        val previousItem = if (previousPosition >= 0) getItem(previousPosition) else null
        when (holder) {
            is CalendarHeaderViewHolder -> holder.bind(context, currentItem)
            is CalendarItemViewHolder -> holder.bind(context, currentItem, previousItem)
            else -> throw IllegalArgumentException("Unknown view holder type")
        }
    }

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ITEM = 1

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CalendarItem>() {
            override fun areItemsTheSame(old: CalendarItem, new: CalendarItem): Boolean =
                old.headerTime == new.headerTime
                        && old.episode?.episodeTvdbId == new.episode?.episodeTvdbId

            override fun areContentsTheSame(old: CalendarItem, new: CalendarItem): Boolean {
                return old == new
            }
        }
    }
}