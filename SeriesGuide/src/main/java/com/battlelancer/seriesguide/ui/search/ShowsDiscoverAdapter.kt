package com.battlelancer.seriesguide.ui.search

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.battlelancer.seriesguide.R

class ShowsDiscoverAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_LINK = R.layout.item_discover_link
    private val VIEW_TYPE_HEADER = R.layout.item_discover_header
    private val VIEW_TYPE_SHOW = R.layout.item_addshow

    private val searchResults = mutableListOf<SearchResult>()
    private val linksCount = 0

    fun updateSearchResults(newSearchResults: List<SearchResult>?) {
        searchResults.clear()
        if (newSearchResults != null) {
            searchResults.addAll(newSearchResults)
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position < linksCount -> VIEW_TYPE_LINK
            position == linksCount -> VIEW_TYPE_HEADER
            else -> VIEW_TYPE_SHOW
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_LINK -> LayoutInflater
                    .from(parent.context)
                    .inflate(VIEW_TYPE_LINK, parent, false)
                    .let { LinkViewHolder(it) }
            VIEW_TYPE_HEADER -> LayoutInflater
                    .from(parent.context)
                    .inflate(VIEW_TYPE_HEADER, parent, false)
                    .let { HeaderViewHolder(it) }
            VIEW_TYPE_SHOW -> LayoutInflater
                    .from(parent.context)
                    .inflate(VIEW_TYPE_SHOW, parent, false)
                    .let { ShowViewHolder(it) }
            else -> throw IllegalArgumentException("View type $viewType is unknown")
        }
    }

    override fun getItemCount(): Int = linksCount + 1 /* header */ + searchResults.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        when (holder) {
            is LinkViewHolder -> {

            }
            is HeaderViewHolder -> {
                holder.header.setText(R.string.title_new_episodes)
            }
            is ShowViewHolder -> {

            }
        }
    }

}

class LinkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    init {
        ButterKnife.bind(this, itemView)
    }
}

class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    @BindView(R.id.textViewGridHeader)
    lateinit var header: TextView

    init {
        ButterKnife.bind(this, itemView)
    }
}

class ShowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    init {
        ButterKnife.bind(this, itemView)
    }
}
