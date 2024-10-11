// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter

class SearchResultPagingAdapter(
    private val context: Context,
    private val itemClickListener: ItemAddShowViewHolder.ClickListener,
    private val showWatchlistActions: Boolean
) : PagingDataAdapter<SearchResult, ItemAddShowViewHolder>(
    SearchResultDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemAddShowViewHolder {
        return ItemAddShowViewHolder.create(parent, itemClickListener)
    }

    override fun onBindViewHolder(holder: ItemAddShowViewHolder, position: Int) {
        holder.bindTo(context, getItem(position), showWatchlistActions)
    }

    fun setStateForTmdbId(showTmdbId: Int, newState: Int) {
        // use the current PagedList instead of getItem to avoid loading more items
        (snapshot().items as List<SearchResult?>).let {
            val count = it.size
            for (i in 0 until count) {
                val item = it[i]
                if (item != null && item.tmdbId == showTmdbId) {
                    item.state = newState
                    notifyItemChanged(i)
                    break
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged") // Too much work to track changed positions.
    fun setAllPendingNotAdded() {
        // use the current PagedList instead of getItem to avoid loading more items
        (snapshot().items as List<SearchResult?>).let {
            val count = it.size
            for (i in 0 until count) {
                val item = it[i]
                if (item != null && item.state == SearchResult.STATE_ADDING) {
                    item.state = SearchResult.STATE_ADD
                }
            }
        }
        notifyDataSetChanged()
    }

}