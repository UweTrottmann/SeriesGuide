// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.battlelancer.seriesguide.databinding.ViewFilterWatchProvidersBinding
import com.battlelancer.seriesguide.streaming.SgWatchProvider

class WatchProviderFilterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewFilterWatchProvidersBinding
    private val adapter: WatchProviderFilterAdapter

    init {
        orientation = VERTICAL

        // can't do in onFinishInflate as that is only called when inflating from XML
        binding = ViewFilterWatchProvidersBinding.inflate(LayoutInflater.from(context), this)

        adapter = WatchProviderFilterAdapter()
        binding.recyclerViewProviderFilter.also {
            it.layoutManager = LinearLayoutManager(context)
            it.adapter = adapter
        }
    }

    fun setWatchProviders(watchProviders: List<SgWatchProvider>) {
        adapter.submitList(watchProviders)
    }

}