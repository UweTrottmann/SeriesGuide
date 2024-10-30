// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.TooltipCompat
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.databinding.ItemShowListBinding
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.ViewTools.setContextAndLongClickListener

class ShowsViewHolder(
    private val binding: ItemShowListBinding,
    private val itemClickListener: ItemClickListener
) : RecyclerView.ViewHolder(binding.root) {

    interface ItemClickListener {
        fun onItemClick(anchor: View, showRowId: Long)
        fun onMoreOptionsClick(anchor: View, show: ShowsAdapter.ShowItem)
        fun onSetWatchedClick(show: ShowsAdapter.ShowItem)
    }

    private var showItem: ShowsAdapter.ShowItem? = null

    init {
        // item
        itemView.setOnClickListener { view ->
            showItem?.let {
                itemClickListener.onItemClick(view, it.rowId)
            }
        }
        // set watched button
        binding.imageViewItemShowListSetWatched.also {
            TooltipCompat.setTooltipText(it, it.contentDescription)
            @Suppress("UNUSED_ANONYMOUS_PARAMETER")
            it.setOnClickListener { v ->
                showItem?.let {
                    itemClickListener.onSetWatchedClick(it)
                }
            }
        }
        // more options button
        itemView.setContextAndLongClickListener {
            onMoreOptionsClick()
        }
        binding.imageViewItemShowListMoreOptions.also {
            TooltipCompat.setTooltipText(it, it.contentDescription)
            it.setOnClickListener {
                onMoreOptionsClick()
            }
        }
    }

    private fun onMoreOptionsClick() {
        showItem?.let {
            itemClickListener.onMoreOptionsClick(binding.imageViewItemShowListMoreOptions, it)
        }
    }

    fun bind(show: ShowsAdapter.ShowItem, context: Context) {
        showItem = show

        binding.textViewItemShowListTitle.text = show.name
        binding.textViewItemShowListTimeAndNetwork.text = show.timeAndNetwork
        binding.textViewItemShowListNextEpisode.text = show.episode
        binding.textViewItemShowListNextEpisodeTime.text = show.episodeTime

        binding.textViewItemShowListRemaining.apply {
            text = show.remainingCount
            visibility = if (show.remainingCount != null) View.VISIBLE else View.GONE
        }

        binding.imageViewItemShowListFavorited.visibility =
            if (show.isFavorite) View.VISIBLE else View.GONE

        binding.imageViewItemShowListSetWatched.visibility =
            if (show.hasNextEpisode) View.VISIBLE else View.GONE

        // set poster
        ImageTools.loadShowPosterResizeCrop(
            context,
            binding.imageViewItemShowListPoster,
            show.posterPath
        )
    }

    companion object {

        fun create(
            parent: ViewGroup,
            itemClickListener: ItemClickListener
        ): ShowsViewHolder {
            return ShowsViewHolder(
                ItemShowListBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                itemClickListener
            )
        }
    }

}