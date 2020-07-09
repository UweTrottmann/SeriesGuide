package com.battlelancer.seriesguide.ui.streams

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.movies.AutoGridLayoutManager
import com.battlelancer.seriesguide.ui.streams.TraktEpisodeHistoryLoader.HistoryItem
import com.uwetrottmann.trakt5.entities.HistoryEntry

abstract class BaseHistoryAdapter(
    val context: Context,
    val itemClickListener: OnItemClickListener
) : ListAdapter<HistoryItem, RecyclerView.ViewHolder>(
    DIFF_CALLBACK
), AutoGridLayoutManager.SpanCountListener {

    interface OnItemClickListener {
        fun onItemClick(view: View, item: HistoryEntry)
    }

    private var drawableWatched =
        AppCompatResources.getDrawable(context, R.drawable.ic_watch_16dp)!!
    private var drawableCheckIn =
        AppCompatResources.getDrawable(context, R.drawable.ic_checkin_16dp)!!

    private var isMultiColumn: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return HistoryItemViewHolder.inflate(parent, itemClickListener)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HistoryItemViewHolder) {
            val currentItem = getItem(position)
            val previousPosition = position - 1
            val previousItem = if (previousPosition >= 0) getItem(previousPosition) else null
            holder.bindCommon(
                currentItem,
                previousItem,
                drawableWatched,
                drawableCheckIn,
                isMultiColumn
            )
            onBindHistoryItemViewHolder(holder, getItem(position))
        }
    }

    abstract fun onBindHistoryItemViewHolder(
        holder: HistoryItemViewHolder,
        item: HistoryItem
    )

    override fun onSetSpanCount(spanCount: Int) {
        isMultiColumn = spanCount > 1
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<HistoryItem>() {
            override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem) =
                oldItem.historyEntry.id == newItem.historyEntry.id

            override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean =
                HistoryItemViewHolder.areContentsTheSame(oldItem, newItem)
        }
    }

}