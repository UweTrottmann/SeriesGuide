package com.battlelancer.seriesguide.ui.streams

import android.content.Context

class MovieHistoryAdapter(
    context: Context,
    itemClickListener: OnItemClickListener
) : BaseHistoryAdapter(context, itemClickListener) {

    override fun onBindHistoryItemViewHolder(
        holder: HistoryItemViewHolder,
        item: TraktEpisodeHistoryLoader.HistoryItem
    ) {
        holder.bindToMovie(item.historyEntry)
    }
}