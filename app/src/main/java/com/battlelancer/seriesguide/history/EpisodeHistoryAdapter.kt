// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.history

import android.content.Context
import androidx.collection.SparseArrayCompat
import com.battlelancer.seriesguide.SgApp

class EpisodeHistoryAdapter(
    context: Context,
    itemClickListener: OnItemClickListener
) : BaseHistoryAdapter(context, itemClickListener) {

    private var localShowPosters: SparseArrayCompat<String>? = null

    override fun submitList(list: MutableList<TraktEpisodeHistoryLoader.HistoryItem>?) {
        // TODO This should be done async (e.g. in view model).
        localShowPosters = SgApp.getServicesComponent(context).showTools().getTmdbIdsToPoster()
        super.submitList(list)
    }

    override fun onBindHistoryItemViewHolder(
        holder: HistoryItemViewHolder,
        item: TraktEpisodeHistoryLoader.HistoryItem
    ) {
        holder.bindToEpisode(item.historyEntry, localShowPosters)
    }
}