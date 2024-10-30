// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.similar

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.battlelancer.seriesguide.shows.search.discover.ItemAddShowViewHolder
import com.battlelancer.seriesguide.shows.search.discover.SearchResult
import com.battlelancer.seriesguide.shows.search.discover.SearchResultDiffCallback

class SimilarShowsAdapter(
    private val context: Context,
    private val itemClickListener: ItemAddShowViewHolder.ClickListener,
    private val showWatchlistActions: Boolean
) : ListAdapter<SearchResult, ItemAddShowViewHolder>(
    SearchResultDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemAddShowViewHolder {
        return ItemAddShowViewHolder.create(parent, itemClickListener)
    }

    override fun onBindViewHolder(holder: ItemAddShowViewHolder, position: Int) {
        holder.bindTo(context, getItem(position), showWatchlistActions)
    }

}