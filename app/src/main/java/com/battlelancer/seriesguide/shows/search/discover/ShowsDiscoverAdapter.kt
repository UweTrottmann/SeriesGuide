// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ItemAddshowBinding
import com.battlelancer.seriesguide.databinding.ItemDiscoverEmptyBinding
import com.battlelancer.seriesguide.databinding.ItemDiscoverHeaderBinding
import com.battlelancer.seriesguide.databinding.ItemDiscoverLinkBinding
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.ui.AutoGridLayoutManager
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.ViewTools.setContextAndLongClickListener

/**
 * Displays a set of links and if loaded a list of results, separated by a header.
 */
class ShowsDiscoverAdapter(
    private val context: Context,
    private val itemClickListener: ItemClickListener,
    private val showMenuWatchlist: Boolean,
    private val hideMenuWatchlistIfAdded: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var emptyText: String = ""
    private var hasError: Boolean = false
    private val searchResults = mutableListOf<SearchResult>()
    private val links: MutableList<DiscoverShowsLink> = mutableListOf()

    val layoutManager = AutoGridLayoutManager(
        context, R.dimen.showgrid_columnWidth,
        2, 2
    ).apply {
        spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (getItemViewType(position)) {
                    VIEW_TYPE_LINK -> 1
                    VIEW_TYPE_HEADER -> spanCount
                    VIEW_TYPE_SHOW -> 2
                    VIEW_TYPE_EMPTY -> spanCount
                    else -> 0
                }
            }
        }
    }

    init {
        links.add(DiscoverShowsLink.POPULAR)
        if (TraktCredentials.get(context).hasCredentials()) {
            links.add(DiscoverShowsLink.WATCHED)
            links.add(DiscoverShowsLink.COLLECTION)
            links.add(DiscoverShowsLink.WATCHLIST)
        }
    }

    @SuppressLint("NotifyDataSetChanged") // No need for incremental updates/animations.
    fun updateSearchResults(
        newSearchResults: List<SearchResult>?,
        emptyText: String,
        hasError: Boolean
    ) {
        searchResults.clear()
        if (newSearchResults != null) {
            searchResults.addAll(newSearchResults)
        }
        this.emptyText = emptyText
        this.hasError = hasError
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

    private fun hasResults() = searchResults.isNotEmpty()

    override fun getItemViewType(position: Int): Int {
        return when {
            position < links.size -> VIEW_TYPE_LINK
            position == links.size -> VIEW_TYPE_HEADER
            else -> if (hasResults()) VIEW_TYPE_SHOW else VIEW_TYPE_EMPTY
        }
    }

    override fun getItemCount(): Int {
        return links.size +
                1 /* header */ +
                if (hasResults()) searchResults.size else 1 /* empty view */
    }

    private fun getSearchResultFor(position: Int): SearchResult =
        searchResults[position - links.size - 1 /* header */]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_LINK -> LinkViewHolder.inflate(parent, itemClickListener)
            VIEW_TYPE_HEADER -> HeaderViewHolder.inflate(parent, itemClickListener)
            VIEW_TYPE_SHOW -> ShowViewHolder.inflate(parent, itemClickListener)
            VIEW_TYPE_EMPTY -> EmptyViewHolder.inflate(parent, itemClickListener)
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

            is EmptyViewHolder -> {
                holder.bindTo(emptyText, hasError)
            }
        }
    }

    interface ItemClickListener {
        fun onLinkClick(anchor: View, link: DiscoverShowsLink)
        fun onHeaderButtonClick(anchor: View)
        fun onItemClick(item: SearchResult)
        fun onAddClick(item: SearchResult)
        fun onMoreOptionsClick(view: View, showTmdbId: Int)
        fun onEmptyViewButtonClick()
    }

    class LinkViewHolder(
        private val binding: ItemDiscoverLinkBinding,
        itemClickListener: ItemClickListener
    ) : RecyclerView.ViewHolder(binding.root) {

        private var link: DiscoverShowsLink? = null

        init {
            itemView.setOnClickListener {
                link?.let {
                    itemClickListener.onLinkClick(itemView, it)
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
            fun inflate(parent: ViewGroup, itemClickListener: ItemClickListener) =
                LinkViewHolder(
                    ItemDiscoverLinkBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    itemClickListener
                )
        }
    }

    class HeaderViewHolder(
        private val binding: ItemDiscoverHeaderBinding,
        itemClickListener: ItemClickListener
    ) : RecyclerView.ViewHolder(binding.root) {

        private var link: DiscoverShowsLink? = null

        init {
            binding.textViewGridHeader.setOnClickListener {
                link?.let {
                    itemClickListener.onLinkClick(itemView, it)
                }
            }
            binding.buttonDiscoverHeader.apply {
                setIconResource(R.drawable.ic_filter_white_24dp)
                contentDescription = context.getString(R.string.action_shows_filter)
                TooltipCompat.setTooltipText(this, contentDescription)
                setOnClickListener { view ->
                    itemClickListener.onHeaderButtonClick(view)
                }
            }
        }

        fun bindTo(link: DiscoverShowsLink) {
            this.link = link
            binding.textViewGridHeader.setText(link.titleRes)
        }

        companion object {
            fun inflate(parent: ViewGroup, itemClickListener: ItemClickListener) =
                HeaderViewHolder(
                    ItemDiscoverHeaderBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    itemClickListener
                )
        }
    }

    class ShowViewHolder(
        private val binding: ItemAddshowBinding,
        private val itemClickListener: ItemClickListener
    ) : RecyclerView.ViewHolder(binding.root) {

        private var item: SearchResult? = null

        init {
            itemView.setOnClickListener {
                item?.let { itemClickListener.onItemClick(it) }
            }
            binding.addIndicatorAddShow.setOnAddClickListener {
                item?.let { itemClickListener.onAddClick(it) }
            }
        }

        private fun onMoreOptionsClick() {
            item?.let {
                itemClickListener.onMoreOptionsClick(binding.buttonItemAddMoreOptions, it.tmdbId)
            }
        }

        fun bindTo(
            context: Context,
            item: SearchResult,
            showMenuWatchlist: Boolean,
            hideMenuWatchlistIfAdded: Boolean
        ) {
            this.item = item

            // hide and disable watchlist menu if not useful
            val showMenuWatchlistActual = showMenuWatchlist
                    && (!hideMenuWatchlistIfAdded || item.state != SearchResult.STATE_ADDED)
            if (showMenuWatchlistActual) {
                itemView.setContextAndLongClickListener {
                    onMoreOptionsClick()
                }
                binding.buttonItemAddMoreOptions.setOnClickListener {
                    onMoreOptionsClick()
                }
                binding.buttonItemAddMoreOptions.isVisible = true
            } else {
                // remove listener to prevent long press feedback
                itemView.setContextAndLongClickListener(null)
                binding.buttonItemAddMoreOptions.setOnClickListener(null)
                binding.buttonItemAddMoreOptions.isGone = true
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
            fun inflate(parent: ViewGroup, itemClickListener: ItemClickListener) =
                ShowViewHolder(
                    ItemAddshowBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                    itemClickListener
                )
        }
    }

    class EmptyViewHolder(
        private val binding: ItemDiscoverEmptyBinding,
        itemClickListener: ItemClickListener
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.emptyViewShowsDiscover.setButtonClickListener {
                itemClickListener.onEmptyViewButtonClick()
            }
        }

        fun bindTo(emptyMessage: String, hasError: Boolean) {
            binding.emptyViewShowsDiscover.setMessage(emptyMessage)
            binding.emptyViewShowsDiscover.setButtonGone(!hasError)
        }

        companion object {
            fun inflate(parent: ViewGroup, itemClickListener: ItemClickListener) =
                EmptyViewHolder(
                    ItemDiscoverEmptyBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    itemClickListener
                )
        }
    }

    companion object {
        const val VIEW_TYPE_LINK = 1
        const val VIEW_TYPE_HEADER = 2
        const val VIEW_TYPE_SHOW = 3
        const val VIEW_TYPE_EMPTY = 4
    }
}
