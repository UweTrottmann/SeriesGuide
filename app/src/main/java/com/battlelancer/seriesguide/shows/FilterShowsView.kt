// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2025 Uwe Trottmann

package com.battlelancer.seriesguide.shows

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ViewFilterShowsBinding
import com.battlelancer.seriesguide.shows.ShowsDistillationSettings.ShowFilters
import com.battlelancer.seriesguide.util.TextTools

class FilterShowsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val binding: ViewFilterShowsBinding

    init {
        // can't do in onFinishInflate as that is only called when inflating from XML
        binding = ViewFilterShowsBinding.inflate(LayoutInflater.from(context), this)

        binding.checkboxShowsFilterFavorites.setOnClickListener { updateFilterListener() }
        binding.checkboxShowsFilterUnwatched.setOnClickListener { updateFilterListener() }
        binding.checkboxShowsFilterUpcoming.setOnClickListener { updateFilterListener() }
        binding.checkboxShowsFilterHidden.setOnClickListener { updateFilterListener() }
        binding.checkboxShowsFilterContinuing.setOnClickListener { updateFilterListener() }
        binding.buttonShowsFilterReset.setOnClickListener {
            setInitialFilter(ShowFilters.default(), binding.checkboxShowsFilterNoReleased.isChecked)
            updateFilterListener()
        }
        binding.buttonShowsFilterAllVisible.setOnClickListener { filterListener?.onMakeAllHiddenVisibleClick() }
        binding.buttonShowsFilterUpcomingRange.setOnClickListener { filterListener?.onConfigureUpcomingRangeClick() }
        binding.checkboxShowsFilterNoReleased.apply {
            setOnClickListener { filterListener?.onNoReleasedChanged(binding.checkboxShowsFilterNoReleased.isChecked) }
            text = TextTools.buildTitleAndSummary(
                context,
                R.string.pref_onlyfuture,
                R.string.pref_onlyfuturesummary
            )
        }
    }

    private var filterListener: FilterListener? = null

    private fun updateFilterListener() {
        filterListener?.onFilterUpdate(
            ShowFilters(
                binding.checkboxShowsFilterFavorites.state,
                binding.checkboxShowsFilterUnwatched.state,
                binding.checkboxShowsFilterUpcoming.state,
                binding.checkboxShowsFilterHidden.state,
                binding.checkboxShowsFilterContinuing.state
            )
        )
    }

    fun setInitialFilter(showFilters: ShowFilters, noReleased: Boolean) {
        binding.checkboxShowsFilterFavorites.state = showFilters.isFilterFavorites
        binding.checkboxShowsFilterUnwatched.state = showFilters.isFilterUnwatched
        binding.checkboxShowsFilterUpcoming.state = showFilters.isFilterUpcoming
        binding.checkboxShowsFilterHidden.state = showFilters.isFilterHidden
        binding.checkboxShowsFilterContinuing.state = showFilters.isFilterContinuing
        binding.checkboxShowsFilterNoReleased.isChecked = noReleased
    }

    fun setFilterListener(filterListener: FilterListener) {
        this.filterListener = filterListener
    }

    interface FilterListener {
        fun onFilterUpdate(filters: ShowFilters)
        fun onConfigureUpcomingRangeClick()
        fun onMakeAllHiddenVisibleClick()
        fun onNoReleasedChanged(value: Boolean)
    }

}