// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.ImageTools

class SearchResultViewHolder(
    itemView: View,
    itemClickListener: BaseAddShowsFragment.ItemClickListener
) : RecyclerView.ViewHolder(itemView) {

    private val title = itemView.findViewById<TextView>(R.id.textViewAddTitle)
    private val description = itemView.findViewById<TextView>(R.id.textViewAddDescription)
    private val poster = itemView.findViewById<ImageView>(R.id.imageViewAddPoster)
    private val addIndicator = itemView.findViewById<AddIndicator>(R.id.addIndicatorAddShow)
    private val moreOptionsButton = itemView.findViewById<ImageView>(R.id.buttonItemAddMoreOptions)
    private var item: SearchResult? = null

    init {
        itemView.setOnClickListener { _ ->
            item?.let {
                itemClickListener.onItemClick(it)
            }
        }
        addIndicator.setOnAddClickListener { _ ->
            item?.let {
                itemClickListener.onAddClick(it)
            }
        }
    }

    fun bindTo(searchResult: SearchResult?) {
        this.item = searchResult

        // hide more options button
        moreOptionsButton.visibility = View.GONE

        // add indicator
        if (searchResult != null) {
            addIndicator.setState(searchResult.state)
            addIndicator.setNameOfAssociatedItem(searchResult.title)
            addIndicator.visibility = View.VISIBLE
        } else {
            addIndicator.visibility = View.GONE
        }

        title.text = searchResult?.title
        description.text = searchResult?.overview

        // only local shows will have a poster path set
        // try to fall back to the TMDB poster for all others
        val posterUrl = if (searchResult != null) {
            ImageTools.posterUrlOrResolve(
                searchResult.posterPath,
                searchResult.tmdbId,
                searchResult.language,
                itemView.context
            )
        } else null
        ImageTools.loadShowPosterUrlResizeCrop(itemView.context, poster, posterUrl)
    }

    companion object {
        fun create(
            parent: ViewGroup,
            itemClickListener: BaseAddShowsFragment.ItemClickListener
        ): SearchResultViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_addshow, parent, false)
            return SearchResultViewHolder(view, itemClickListener)
        }
    }

}