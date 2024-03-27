// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows.search.discover

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.databinding.ItemListCheckedBinding
import com.battlelancer.seriesguide.streaming.SgWatchProvider

class ShowsDiscoverFilterAdapter(
    private val clickListener: ClickListener
) : PagingDataAdapter<SgWatchProvider, SgWatchProviderViewHolder>(
    SgWatchProviderDiffCallback
) {
    interface ClickListener {
        fun onClick(watchProvider: SgWatchProvider)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SgWatchProviderViewHolder {
        return SgWatchProviderViewHolder(
            ItemListCheckedBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            clickListener
        )
    }

    override fun onBindViewHolder(holder: SgWatchProviderViewHolder, position: Int) {
        holder.bindTo(getItem(position))
    }
}

object SgWatchProviderDiffCallback : DiffUtil.ItemCallback<SgWatchProvider>() {
    override fun areItemsTheSame(oldItem: SgWatchProvider, newItem: SgWatchProvider): Boolean =
        oldItem._id == newItem._id

    override fun areContentsTheSame(oldItem: SgWatchProvider, newItem: SgWatchProvider): Boolean =
        oldItem == newItem
}

class SgWatchProviderViewHolder(
    private val binding: ItemListCheckedBinding,
    clickListener: ShowsDiscoverFilterAdapter.ClickListener
) : RecyclerView.ViewHolder(binding.root) {

    private var watchProvider: SgWatchProvider? = null

    init {
        binding.root.setOnClickListener {
            watchProvider?.let {
                clickListener.onClick(it)
            }
        }
    }

    fun bindTo(watchProvider: SgWatchProvider?) {
        this.watchProvider = watchProvider
        if (watchProvider == null) {
            binding.textViewListItem.text = null
            binding.checkBoxListItem.apply {
                isChecked = false
                isEnabled = false
            }
        } else {
            binding.textViewListItem.text = watchProvider.provider_name
            binding.checkBoxListItem.apply {
                isChecked = watchProvider.enabled
                isEnabled = true
            }
        }
    }
}
