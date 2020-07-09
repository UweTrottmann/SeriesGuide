package com.battlelancer.seriesguide.ui.streams

import android.content.Context
import com.uwetrottmann.trakt5.entities.HistoryEntry

class MovieHistoryAdapter(
    context: Context,
    itemClickListener: OnItemClickListener
) : BaseHistoryAdapter(context, itemClickListener) {
    override fun onBindHistoryItemViewHolder(holder: HistoryItemViewHolder, item: HistoryEntry) {
        holder.bindToMovie(item)
    }
}