// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.similar

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.battlelancer.seriesguide.shows.search.discover.BaseAddShowsFragment
import com.battlelancer.seriesguide.shows.search.discover.SearchResult
import com.battlelancer.seriesguide.shows.search.discover.SearchResultDiffCallback
import com.battlelancer.seriesguide.shows.search.discover.SearchResultViewHolder

class SimilarShowsAdapter(
    private val onItemClickListener: BaseAddShowsFragment.OnItemClickListener
) : ListAdapter<SearchResult, SearchResultViewHolder>(
    SearchResultDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        return SearchResultViewHolder.create(parent, onItemClickListener)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        holder.bindTo(getItem(position))
    }

}