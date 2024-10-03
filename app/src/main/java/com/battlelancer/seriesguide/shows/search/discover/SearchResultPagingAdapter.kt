// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView

class SearchResultPagingAdapter(
     private val itemClickListener: BaseAddShowsFragment.ItemClickListener
) : PagingDataAdapter<SearchResult, RecyclerView.ViewHolder>(
    SearchResultDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return SearchResultViewHolder.create(parent, itemClickListener)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as SearchResultViewHolder).bindTo(getItem(position))
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