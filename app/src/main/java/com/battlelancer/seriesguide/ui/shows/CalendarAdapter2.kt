package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.ui.shows.CalendarFragment2ViewModel.CalendarItem

class CalendarAdapter2(private val context: Context) :
    ListAdapter<CalendarItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    data class Item(val id: Int)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return CalendarViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CalendarViewHolder -> holder.bind(getItem(position), context)
            else -> throw IllegalArgumentException("Unknown view holder type")
        }
    }

    fun itemHasHeader(itemPosition: Int): Boolean {
        if (itemPosition == 0) return true // top most item always has date header

        // show header if previous episode falls onto another day
        val episode = getItem(itemPosition)
        val episodeBefore = getItem(itemPosition - 1)
        return episode.headerTime != episodeBefore.headerTime
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CalendarItem>() {
            override fun areItemsTheSame(old: CalendarItem, new: CalendarItem): Boolean =
                // TODO expand
                old.episode.episodeTvdbId == new.episode.episodeTvdbId

            override fun areContentsTheSame(old: CalendarItem, new: CalendarItem): Boolean {
                return old == new
            }
        }
    }
}