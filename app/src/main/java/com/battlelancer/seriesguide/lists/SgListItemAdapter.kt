// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2021 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.lists

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.databinding.ItemShowListBinding
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.ViewTools.setContextAndLongClickListener

class SgListItemAdapter(
    private val context: Context,
    private val itemClickListener: SgListItemViewHolder.ItemClickListener
) : ListAdapter<UiListItem, SgListItemViewHolder>(UiListItem.DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SgListItemViewHolder {
        return SgListItemViewHolder.create(itemClickListener, parent)
    }

    override fun onBindViewHolder(holder: SgListItemViewHolder, position: Int) {
        holder.bindTo(getItem(position), context)
    }


}

class SgListItemViewHolder(
    private val binding: ItemShowListBinding,
    private val itemClickListener: ItemClickListener
) : RecyclerView.ViewHolder(binding.root) {

    interface ItemClickListener {
        fun onItemClick(anchor: View, item: UiListItem)
        fun onMoreOptionsClick(anchor: View, item: UiListItem)
        fun onSetWatchedClick(item: UiListItem)
    }

    var item: UiListItem? = null

    init {
        // item
        binding.root.setOnClickListener { view ->
            item?.let { itemClickListener.onItemClick(view, it) }
        }
        // set watched button
        binding.imageViewItemShowListSetWatched.apply {
            TooltipCompat.setTooltipText(this, this.contentDescription)
            setOnClickListener {
                item?.let { itemClickListener.onSetWatchedClick(it) }
            }
        }
        // more options button
        binding.root.setContextAndLongClickListener {
            onMoreOptionsClick()
        }
        binding.imageViewItemShowListMoreOptions.apply {
            TooltipCompat.setTooltipText(this, this.contentDescription)
            setOnClickListener {
                onMoreOptionsClick()
            }
        }
    }

    private fun onMoreOptionsClick() {
        item?.let {
            itemClickListener.onMoreOptionsClick(
                binding.imageViewItemShowListMoreOptions,
                it
            )
        }
    }

    fun bindTo(item: UiListItem?, context: Context) {
        this.item = item

        // Title and favorite star
        binding.textViewItemShowListTitle.text = item?.titleText
        binding.imageViewItemShowListFavorited.isVisible = item?.isFavorite ?: false

        // Poster
        ImageTools.loadShowPosterResizeCrop(
            context,
            binding.imageViewItemShowListPoster,
            item?.posterUrl
        )

        // Set watched button
        binding.imageViewItemShowListSetWatched.isVisible = item?.isSetWatchedButtonVisible ?: false

        binding.textViewItemShowListNextEpisode.text = item?.nextEpisodeText
        binding.textViewItemShowListNextEpisodeTime.text = item?.nextEpisodeTimeText
        binding.textViewItemShowListTimeAndNetwork.text = item?.timeAndNetworkText
        // Use gone if not needed because remaining view adds padding
        binding.textViewItemShowListRemaining.isVisible = item?.remainingText != null
        binding.textViewItemShowListRemaining.text = item?.remainingText
    }

    companion object {
        fun create(
            itemClickListener: ItemClickListener,
            parent: ViewGroup
        ): SgListItemViewHolder = SgListItemViewHolder(
            ItemShowListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            itemClickListener
        )
    }

}
