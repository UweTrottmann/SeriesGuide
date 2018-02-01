package com.battlelancer.seriesguide.ui.search

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools

class ShowsDiscoverAdapter(
        private val context: Context,
        private val menuClickListener: OnItemClickListener,
        private val showMenuWatchlist: Boolean,
        private val hideMenuWatchlistIfAdded: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_LINK = R.layout.item_discover_link
    private val VIEW_TYPE_HEADER = R.layout.item_discover_header
    private val VIEW_TYPE_SHOW = R.layout.item_addshow

    private val searchResults = mutableListOf<SearchResult>()
    private val linksCount = 0
    private var showOnlyResults = false

    fun updateSearchResults(newSearchResults: List<SearchResult>?, showOnlyResults: Boolean) {
        this.showOnlyResults = showOnlyResults
        searchResults.clear()
        if (newSearchResults != null) {
            searchResults.addAll(newSearchResults)
        }
        notifyDataSetChanged()
    }

    fun setStateForTvdbId(showTvdbId: Int, state: Int) {
        // multiple items may have the same TVDB id
        val matching = searchResults.filter { it.tvdbid == showTvdbId }.onEach { it.state = state }
        if (matching.isNotEmpty()) {
            notifyDataSetChanged()
        }
    }

    fun setAllPendingNotAdded() {
        val matching = searchResults
                .filter { it.state == SearchResult.STATE_ADDING }
                .onEach { it.state = SearchResult.STATE_ADD }
        if (matching.isNotEmpty()) {
            notifyDataSetChanged()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (showOnlyResults) {
            VIEW_TYPE_SHOW
        } else {
            when {
                position < linksCount -> VIEW_TYPE_LINK
                position == linksCount -> VIEW_TYPE_HEADER
                else -> VIEW_TYPE_SHOW
            }
        }
    }

    override fun getItemCount(): Int {
        return if (showOnlyResults) {
            searchResults.size
        } else {
            linksCount + 1 /* header */ + searchResults.size
        }
    }

    private fun getSearchResultFor(position: Int): SearchResult {
        return if (showOnlyResults) {
            searchResults[position]
        } else {
            searchResults[position - linksCount - 1 /* header */]
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
                    .let { ShowViewHolder(it, menuClickListener) }
            else -> throw IllegalArgumentException("View type $viewType is unknown")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        when (holder) {
            is LinkViewHolder -> {

            }
            is HeaderViewHolder -> {
                holder.header.setText(R.string.title_new_episodes)
            }
            is ShowViewHolder -> {
                val item = getSearchResultFor(position)
                holder.item = item

                // hide watchlist menu if not useful
                val showMenuWatchlistActual = showMenuWatchlist
                        && (!hideMenuWatchlistIfAdded || item.state != SearchResult.STATE_ADDED)
                holder.buttonContextMenu.visibility = if (showMenuWatchlistActual) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                // display added indicator instead of add button if already added that show
                holder.addIndicator.setState(item.state)
                val showTitle = item.title
                holder.addIndicator.setContentDescriptionAdded(
                        context.getString(R.string.add_already_exists, showTitle))

                // set text properties immediately
                holder.title.text = showTitle
                holder.description.text = item.overview

                // only local shows will have a poster path set
                // resolve the TVDB poster URL for all others
                val posterUrl = TvdbImageTools.smallSizeOrResolveUrl(item.posterPath,
                        item.tvdbid, item.language)
                TvdbImageTools.loadUrlResizeCrop(context, holder.poster, posterUrl)
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(item: SearchResult)
        fun onAddClick(item: SearchResult)
        fun onMenuWatchlistClick(view: View, showTvdbId: Int)
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

    class ShowViewHolder(itemView: View, onItemClickListener: OnItemClickListener)
        : RecyclerView.ViewHolder(itemView) {

        lateinit var item: SearchResult

        @BindView(R.id.textViewAddTitle)
        lateinit var title: TextView
        @BindView(R.id.textViewAddDescription)
        lateinit var description: TextView
        @BindView(R.id.imageViewAddPoster)
        lateinit var poster: ImageView
        @BindView(R.id.addIndicatorAddShow)
        lateinit var addIndicator: AddIndicator
        @BindView(R.id.buttonItemAddMore)
        lateinit var buttonContextMenu: ImageView

        init {
            ButterKnife.bind(this, itemView)
            itemView.setOnClickListener { onItemClickListener.onItemClick(item) }
            addIndicator.setOnAddClickListener({
                onItemClickListener.onAddClick(item)
            })
            buttonContextMenu.setOnClickListener {
                onItemClickListener.onMenuWatchlistClick(it, item.getTvdbid())
            }
        }
    }
}
