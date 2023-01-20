package com.battlelancer.seriesguide.shows.search.discover

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.battlelancer.seriesguide.databinding.LayoutAddIndicatorBinding

/**
 * A three state visual indicator with states add, adding and added from [SearchResult].
 */
class AddIndicator(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val binding: LayoutAddIndicatorBinding

    init {
        binding = LayoutAddIndicatorBinding.inflate(LayoutInflater.from(context), this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding.imageViewAddIndicatorAdded.visibility = GONE
        binding.progressBarAddIndicator.visibility = GONE
    }

    fun setContentDescriptionAdded(contentDescription: CharSequence?) {
        binding.imageViewAddIndicatorAdded.contentDescription = contentDescription
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