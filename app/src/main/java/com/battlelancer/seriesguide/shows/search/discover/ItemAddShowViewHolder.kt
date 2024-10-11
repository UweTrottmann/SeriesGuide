// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ItemAddshowBinding
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.ViewTools.setContextAndLongClickListener

class ItemAddShowViewHolder(
    private val binding: ItemAddshowBinding,
    private val clickListener: ClickListener
) : RecyclerView.ViewHolder(binding.root) {

    private var item: SearchResult? = null

    init {
        itemView.setOnClickListener {
            item?.let { clickListener.onItemClick(it) }
        }
        binding.addIndicatorAddShow.setOnAddClickListener {
            item?.let { clickListener.onAddClick(it) }
        }
        binding.buttonItemAddMoreOptions.also {
            TooltipCompat.setTooltipText(it, it.contentDescription)
        }
    }

    private fun onMoreOptionsClick(anchor: View) {
        item?.let {
            clickListener.onMoreOptionsClick(anchor, it)
        }
    }

    // Nullable item to support placeholders of paging adapters
    fun bindTo(
        context: Context,
        item: SearchResult?,
        showWatchlistActions: Boolean
    ) {
        this.item = item

        if (item == null) {
            // placeholder data
            binding.textViewAddTitle.text = null
            binding.textViewAddDescription.text = null
            binding.addIndicatorAddShow.isGone = true
            binding.imageViewAddPoster.setImageResource(R.drawable.ic_photo_gray_24dp)

            itemView.setContextAndLongClickListener(null)
            binding.buttonItemAddMoreOptions.isGone = true
            return
        }

        // title and overview
        val showTitle = item.title
        binding.textViewAddTitle.text = showTitle
        binding.textViewAddDescription.text = item.overview

        // add button/indicator
        binding.addIndicatorAddShow.apply {
            setState(item.state)
            setNameOfAssociatedItem(showTitle)
            isVisible = true
        }

        // image
        ImageTools.loadShowPosterResizeCrop(
            context,
            binding.imageViewAddPoster,
            item.posterPath
        )

        // context/long press listener and more options button
        val canBeAdded = item.state == SearchResult.STATE_ADD
        // If not added, always display add action on long press for accessibility
        if (canBeAdded) {
            itemView.setContextAndLongClickListener {
                onMoreOptionsClick(itemView)
            }
        } else {
            // Remove listener to prevent long press feedback
            itemView.setContextAndLongClickListener(null)
        }
        // Only display more options button when displaying watchlist actions
        binding.buttonItemAddMoreOptions.apply {
            if (showWatchlistActions) {
                setOnClickListener {
                    onMoreOptionsClick(binding.buttonItemAddMoreOptions)
                }
                isVisible = true
            } else {
                setOnClickListener(null)
                isGone = true
            }
        }
    }

    companion object {
        fun create(parent: ViewGroup, clickListener: ClickListener) =
            ItemAddShowViewHolder(
                ItemAddshowBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                clickListener
            )
    }

    interface ClickListener {
        fun onItemClick(item: SearchResult)
        fun onAddClick(item: SearchResult)
        fun onMoreOptionsClick(view: View, show: SearchResult)
    }

}