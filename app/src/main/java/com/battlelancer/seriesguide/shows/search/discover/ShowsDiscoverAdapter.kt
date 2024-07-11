// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.TooltipCompat
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ItemAddshowBinding
import com.battlelancer.seriesguide.databinding.ItemDiscoverHeaderBinding
import com.battlelancer.seriesguide.databinding.ItemDiscoverLinkBinding
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
    private val links: MutableList<DiscoverShowsLink> = mutableListOf()

    init {
        links.add(DiscoverShowsLink.POPULAR)
        if (TraktCredentials.get(context).hasCredentials()) {
            links.add(DiscoverShowsLink.WATCHED)
            links.add(DiscoverShowsLink.COLLECTION)
            links.add(DiscoverShowsLink.WATCHLIST)
        }
    }

    @SuppressLint("NotifyDataSetChanged") // No need for incremental updates/animations.
    fun updateSearchResults(newSearchResults: List<SearchResult>?) {
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
        return when {
            position < links.size -> VIEW_TYPE_LINK
            position == links.size -> VIEW_TYPE_HEADER
            else -> VIEW_TYPE_SHOW
        }
    }

    override fun getItemCount(): Int = links.size + 1 /* header */ + searchResults.size

    private fun getSearchResultFor(position: Int): SearchResult =
        searchResults[position - links.size - 1 /* header */]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_LINK -> LinkViewHolder.inflate(parent, itemClickListener)
            VIEW_TYPE_HEADER -> HeaderViewHolder.inflate(parent, itemClickListener)
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
                holder.bindTo(DiscoverShowsLink.NEW_EPISODES)
            }

            is ShowViewHolder -> {
                val item = getSearchResultFor(position)
                holder.bindTo(context, item, showMenuWatchlist, hideMenuWatchlistIfAdded)
            }
        }
    }

    interface OnItemClickListener {
        fun onLinkClick(anchor: View, link: DiscoverShowsLink)
        fun onHeaderButtonClick(anchor: View)
        fun onItemClick(item: SearchResult)
        fun onAddClick(item: SearchResult)
        fun onMenuWatchlistClick(view: View, showTmdbId: Int)
    }

    class LinkViewHolder(
        private val binding: ItemDiscoverLinkBinding,
        onItemClickListener: OnItemClickListener
    ) : RecyclerView.ViewHolder(binding.root) {

        private var link: DiscoverShowsLink? = null

        init {
            itemView.setOnClickListener {
                link?.let {
                    onItemClickListener.onLinkClick(itemView, it)
                }
            }
        }

        fun bindTo(context: Context, link: DiscoverShowsLink) {
            this.link = link
            binding.textViewDiscoverLink.text = context.getString(link.titleRes)
            // Add Trakt icon to highlight Trakt profile specific links.
            if (link != DiscoverShowsLink.POPULAR) {
                ViewTools.setVectorDrawableLeft(
                    binding.textViewDiscoverLink,
                    R.drawable.ic_trakt_icon_primary_24dp
                )
            } else {
                binding.textViewDiscoverLink.setCompoundDrawables(null, null, null, null)
            }
        }

        companion object {
            fun inflate(parent: ViewGroup, onItemClickListener: OnItemClickListener) =
                LinkViewHolder(
                    ItemDiscoverLinkBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    onItemClickListener
                )
        }
    }

    class HeaderViewHolder(
        private val binding: ItemDiscoverHeaderBinding,
        onItemClickListener: OnItemClickListener
    ) : RecyclerView.ViewHolder(binding.root) {

        private var link: DiscoverShowsLink? = null

        init {
            binding.textViewGridHeader.setOnClickListener {
                link?.let {
                    onItemClickListener.onLinkClick(itemView, it)
                }
            }
            binding.buttonDiscoverHeader.apply {
                setIconResource(R.drawable.ic_filter_white_24dp)
                contentDescription = context.getString(R.string.action_shows_filter)
                TooltipCompat.setTooltipText(this, contentDescription)
                setOnClickListener { view ->
                    onItemClickListener.onHeaderButtonClick(view)
                }
            }
        }

        fun bindTo(link: DiscoverShowsLink) {
            this.link = link
            binding.textViewGridHeader.setText(link.titleRes)
        }

        companion object {
            fun inflate(parent: ViewGroup, onItemClickListener: OnItemClickListener) =
                HeaderViewHolder(
                    ItemDiscoverHeaderBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    onItemClickListener
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

        fun bindTo(
            context: Context,
            item: SearchResult,
            showMenuWatchlist: Boolean,
            hideMenuWatchlistIfAdded: Boolean
        ) {
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

            ImageTools.loadShowPosterResizeCrop(
                context,
                binding.imageViewAddPoster,
                item.posterPath
            )
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
        const val VIEW_TYPE_LINK = 1
        const val VIEW_TYPE_HEADER = 2
        const val VIEW_TYPE_SHOW = 3
    }
}
