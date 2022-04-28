package com.battlelancer.seriesguide.shows.search.similar

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.battlelancer.seriesguide.ui.search.AddFragment
import com.battlelancer.seriesguide.ui.search.SearchResult
import com.battlelancer.seriesguide.ui.search.SearchResultDiffCallback
import com.battlelancer.seriesguide.ui.search.SearchResultViewHolder

class SimilarShowsAdapter(
    val onItemClickListener: AddFragment.AddAdapter.OnItemClickListener
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