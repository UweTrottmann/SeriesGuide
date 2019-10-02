package com.battlelancer.seriesguide.ui.search

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView

class ShowsPopularAdapter(
    val onItemClickListener: AddFragment.AddAdapter.OnItemClickListener
) : PagedListAdapter<SearchResult, RecyclerView.ViewHolder>(
    SearchResultDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return SearchResultViewHolder.create(parent, onItemClickListener)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as SearchResultViewHolder).bindTo(getItem(position))
    }

    fun setStateForTvdbId(showTvdbId: Int, newState: Int) {
        // use the current PagedList instead of getItem to avoid loading more items
        currentList?.let {
            val count = it.size
            for (i in 0 until count) {
                val item = it[i]
                if (item != null && item.tvdbid == showTvdbId) {
                    item.state = newState
                    notifyDataSetChanged()
                    break
                }
            }
        }
    }

    fun setAllPendingNotAdded() {
        // use the current PagedList instead of getItem to avoid loading more items
        currentList?.let {
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