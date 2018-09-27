package com.battlelancer.seriesguide.ui.search

import android.arch.paging.PagedListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup

class ShowsPopularAdapter(val onItemClickListener: AddFragment.AddAdapter.OnItemClickListener) :
        PagedListAdapter<SearchResult, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ShowsPopularViewHolder.create(parent, onItemClickListener)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ShowsPopularViewHolder).bindTo(getItem(position))
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

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SearchResult>() {
            override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean =
                    oldItem.tvdbid == newItem.tvdbid && oldItem.title == newItem.title

            override fun areContentsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
                return (oldItem.state == newItem.state
                        && oldItem.language == newItem.language
                        && oldItem.posterPath == newItem.posterPath
                        && oldItem.overview == newItem.overview)
            }
        }
    }

}