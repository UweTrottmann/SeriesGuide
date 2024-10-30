// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows.search.discover

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.appcompat.widget.TooltipCompat
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.LayoutAddIndicatorBinding

/**
 * A three state visual indicator with states add, adding and added from [SearchResult].
 *
 * Make sure to call [setNameOfAssociatedItem] to provide content description and tooltips for all
 * states.
 */
class AddIndicator(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val binding = LayoutAddIndicatorBinding.inflate(LayoutInflater.from(context), this)

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding.imageViewAddIndicator.also {
            TooltipCompat.setTooltipText(it, it.contentDescription)
        }
        binding.imageViewAddIndicatorAdded.visibility = GONE
        binding.progressBarAddIndicator.visibility = GONE
    }

    /**
     * Sets content descriptions and tooltips of added and adding states.
     */
    fun setNameOfAssociatedItem(name: String) {
        binding.apply {
            imageViewAddIndicatorAdded.also {
                it.contentDescription = context.getString(R.string.add_already_exists, name)
                TooltipCompat.setTooltipText(it, it.contentDescription)
            }
            progressBarAddIndicator.also {
                it.contentDescription = context.getString(R.string.add_started, name)
                TooltipCompat.setTooltipText(it, it.contentDescription)
            }
        }
    }

    fun setOnAddClickListener(onClickListener: OnClickListener?) {
        binding.imageViewAddIndicator.setOnClickListener(onClickListener)
    }

    fun setState(state: Int) {
        when (state) {
            SearchResult.STATE_ADD -> {
                binding.imageViewAddIndicator.visibility = VISIBLE
                binding.progressBarAddIndicator.visibility = GONE
                binding.imageViewAddIndicatorAdded.visibility = GONE
            }

            SearchResult.STATE_ADDING -> {
                binding.imageViewAddIndicator.visibility = GONE
                binding.progressBarAddIndicator.visibility = VISIBLE
                binding.imageViewAddIndicatorAdded.visibility = GONE
            }

            SearchResult.STATE_ADDED -> {
                binding.imageViewAddIndicator.visibility = GONE
                binding.progressBarAddIndicator.visibility = GONE
                binding.imageViewAddIndicatorAdded.visibility = VISIBLE
            }
        }
    }
}