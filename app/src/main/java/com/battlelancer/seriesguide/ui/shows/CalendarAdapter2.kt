package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.model.EpisodeWithShow

class CalendarAdapter2(private val context: Context) :
    ListAdapter<EpisodeWithShow, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

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

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<EpisodeWithShow>() {
            override fun areItemsTheSame(old: EpisodeWithShow, new: EpisodeWithShow): Boolean =
                // TODO expand
                old.episodeTvdbId == new.episodeTvdbId

            override fun areContentsTheSame(old: EpisodeWithShow, new: EpisodeWithShow): Boolean {
                return old == new
            }
        }
    }
}