// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.history

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