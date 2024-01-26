// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.battlelancer.seriesguide.databinding.ItemWatchProviderBinding
import com.battlelancer.seriesguide.streaming.SgWatchProvider

class WatchProviderFilterAdapter :
    ListAdapter<SgWatchProvider, WatchProviderViewHolder>(WatchProviderDiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchProviderViewHolder {
        return WatchProviderViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: WatchProviderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class WatchProviderViewHolder(private val binding: ItemWatchProviderBinding) :
    ViewHolder(binding.root) {

    fun bind(watchProvider: SgWatchProvider) {
        TODO()
    }

    companion object {
        fun create(parent: ViewGroup): WatchProviderViewHolder =
            WatchProviderViewHolder(
                ItemWatchProviderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
    }
}

object WatchProviderDiffCallback : DiffUtil.ItemCallback<SgWatchProvider>() {
    override fun areItemsTheSame(oldItem: SgWatchProvider, newItem: SgWatchProvider): Boolean {
        TODO("Not yet implemented")
    }

    override fun areContentsTheSame(oldItem: SgWatchProvider, newItem: SgWatchProvider): Boolean {
        TODO("Not yet implemented")
    }

}