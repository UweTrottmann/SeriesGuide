// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows.search.popular

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.shows.search.discover.AddFragment
import com.battlelancer.seriesguide.shows.search.discover.SearchResult
import com.battlelancer.seriesguide.shows.search.discover.SearchResultDiffCallback
import com.battlelancer.seriesguide.shows.search.discover.SearchResultViewHolder

class ShowsPopularAdapter(
    val onItemClickListener: AddFragment.AddAdapter.OnItemClickListener
) : PagingDataAdapter<SearchResult, RecyclerView.ViewHolder>(
    SearchResultDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return SearchResultViewHolder.create(parent, onItemClickListener)
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