package com.battlelancer.seriesguide.ui.search

import androidx.recyclerview.widget.DiffUtil

/** [DiffUtil.ItemCallback] for [SearchResult] items for use with a RecyclerView adapter. */
class SearchResultDiffCallback : DiffUtil.ItemCallback<SearchResult>() {

    override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean =
        oldItem.tmdbId == newItem.tmdbId
                && oldItem.title == newItem.title

    override fun areContentsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean =
        oldItem.state == newItem.state
                && oldItem.language == newItem.language
                && oldItem.posterPath == newItem.posterPath
                && oldItem.overview == newItem.overview

}