package com.battlelancer.seriesguide.ui.search

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter

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