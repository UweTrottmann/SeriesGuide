package com.battlelancer.seriesguide.shows.search.discover

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ItemAddshowBinding
import com.battlelancer.seriesguide.databinding.ItemGridHeaderBinding
import com.battlelancer.seriesguide.databinding.ItemGridLinkBinding
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.ViewTools

/**
 * Displays a set of links and a list of results, each separated by a header.
 */
class ShowsDiscoverAdapter(
    private val context: Context,
    private val itemClickListener: OnItemClickListener,
    private val showMenuWatchlist: Boolean,
    private val hideMenuWatchlistIfAdded: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val searchResults = mutableListOf<SearchResult>()
    private val links: MutableList<TraktShowsLink> = mutableListOf()
    private var showOnlyResults = false

    init {
        links.add(TraktShowsLink.POPULAR)
        if (TraktCredentials.get(context).hasCredentials()) {
            links.add(TraktShowsLink.WATCHED)
            links.add(TraktShowsLink.COLLECTION)
            links.add(TraktShowsLink.WATCHLIST)
        }
    }

    @SuppressLint("NotifyDataSetChanged") // No need for incremental updates/animations.
    fun updateSearchResults(newSearchResults: List<SearchResult>?, showOnlyResults: Boolean) {
        this.showOnlyResults = showOnlyResults
        searchResults.clear()
        if (newSearchResults != null) {
            searchResults.addAll(newSearchResults)
        }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged") // Too much work to track changed positions.
    fun setStateForTmdbId(showTmdbId: Int, state: Int) {
        // multiple items may have the same TMDB id
        val matching = searchResults.asSequence()
            .filter { it.tmdbId == showTmdbId }
            .onEach { it.state = state }
            .toList()
        if (matching.isNotEmpty()) {
            notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged") // Too much work to track changed positions.
    fun setAllPendingNotAdded() {
        val matching = searchResults
            .asSequence()
            .filter { it.state == SearchResult.STATE_ADDING }
            .onEach { it.state = SearchResult.STATE_ADD }
            .toList()
        if (matching.isNotEmpty()) {
            notifyDataSetChanged()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (showOnlyResults) {
            VIEW_TYPE_SHOW
        } else {
            when {
                position < links.size -> VIEW_TYPE_LINK
                position == links.size -> VIEW_TYPE_HEADER
                else -> VIEW_TYPE_SHOW
            }
        }
    }

    override fun getItemCount(): Int {
        return if (showOnlyResults) {
            searchResults.size
        } else {
            links.size + 1 /* header */ + searchResults.size
        }
    }

    private fun getSearchResultFor(position: Int): SearchResult {
        return if (showOnlyResults) {
            searchResults[position]
        } else {
            searchResults[position - links.size - 1 /* header */]
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_LINK -> LinkViewHolder.inflate(parent, itemClickListener)
            VIEW_TYPE_HEADER -> HeaderViewHolder.inflate(parent)
            VIEW_TYPE_SHOW -> ShowViewHolder.inflate(parent, itemClickListener)
            else -> throw IllegalArgumentException("View type $viewType is unknown")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is LinkViewHolder -> {
                holder.bindTo(context, links[position])
            }
            is HeaderViewHolder -> {
                holder.bindTo(R.string.title_new_episodes)
            }
            is ShowViewHolder -> {
                val item = getSearchResultFor(position)
                holder.bindTo(context, item, showMenuWatchlist, hideMenuWatchlistIfAdded)
            }
        }
    }

    interface OnItemClickListener {
        fun onLinkClick(anchor: View, link: TraktShowsLink)
        fun onItemClick(item: SearchResult)
        fun onAddClick(item: SearchResult)
        fun onMenuWatchlistClick(view: View, showTmdbId: Int)
    }

    class LinkViewHolder(
        private val binding: ItemGridLinkBinding,
        onItemClickListener: OnItemClickListener
    ) : RecyclerView.ViewHolder(binding.root) {

        private var link: TraktShowsLink? = null

        init {
            itemView.setOnClickListener {
                link?.let {
                    onItemClickListener.onLinkClick(itemView, it)
                }
            }
        }

        fun bindTo(context: Context, link: TraktShowsLink) {
            this.link = link
            binding.textViewGridLink.text = context.getString(link.titleRes)
            // Add Trakt icon to highlight Trakt profile specific links.
            if (link != TraktShowsLink.POPULAR) {
                ViewTools.setVectorDrawableLeft(
                    binding.textViewGridLink,
                    R.drawable.ic_trakt_icon_primary
                )
            } else {
                binding.textViewGridLink.setCompoundDrawables(null, null, null, null)
            }
        }

        companion object {
            fun inflate(parent: ViewGroup, onItemClickListener: OnItemClickListener) =
                LinkViewHolder(
                    ItemGridLinkBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    onItemClickListener
                )
        }
    }

    class HeaderViewHolder(private val binding: ItemGridHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindTo(@StringRes text: Int) {
            binding.textViewGridHeader.setText(text)
        }

        companion object {
            fun inflate(parent: ViewGroup) = HeaderViewHolder(
                ItemGridHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
    }

    class ShowViewHolder(
        private val binding: ItemAddshowBinding,
        onItemClickListener: OnItemClickListener
    ) : RecyclerView.ViewHolder(binding.root) {

        private var item: SearchResult? = null

        init {
            itemView.setOnClickListener {
                item?.let { onItemClickListener.onItemClick(it) }
            }
            binding.addIndicatorAddShow.setOnAddClickListener {
                item?.let { onItemClickListener.onAddClick(it) }
            }
            binding.buttonItemAddMore.setOnClickListener { view ->
                item?.let { onItemClickListener.onMenuWatchlistClick(view, it.tmdbId) }
            }
        }

        fun bindTo(context: Context, item: SearchResult, showMenuWatchlist: Boolean, hideMenuWatchlistIfAdded: Boolean) {
            this.item = item

            // hide watchlist menu if not useful
            val showMenuWatchlistActual = showMenuWatchlist
                    && (!hideMenuWatchlistIfAdded || item.state != SearchResult.STATE_ADDED)
            binding.buttonItemAddMore.visibility = if (showMenuWatchlistActual) {
                View.VISIBLE
            } else {
                View.GONE
            }
            // display added indicator instead of add button if already added that show
            binding.addIndicatorAddShow.setState(item.state)
            val showTitle = item.title
            binding.addIndicatorAddShow.setContentDescriptionAdded(
                context.getString(R.string.add_already_exists, showTitle)
            )

            // set text properties immediately
            binding.textViewAddTitle.text = showTitle
            binding.textViewAddDescription.text = item.overview

            ImageTools.loadShowPosterResizeCrop(context, binding.imageViewAddPoster, item.posterPath)
        }

        companion object {
            fun inflate(parent: ViewGroup, onItemClickListener: OnItemClickListener) =
                ShowViewHolder(
                    ItemAddshowBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                    onItemClickListener
                )
        }
    }

    companion object {
        const val VIEW_TYPE_LINK = R.layout.item_grid_link
        const val VIEW_TYPE_HEADER = R.layout.item_grid_header
        const val VIEW_TYPE_SHOW = R.layout.item_addshow
    }
}
