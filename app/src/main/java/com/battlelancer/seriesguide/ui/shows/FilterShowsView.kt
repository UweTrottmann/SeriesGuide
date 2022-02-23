package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.battlelancer.seriesguide.databinding.ViewFilterShowsBinding

class FilterShowsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    val binding: ViewFilterShowsBinding

    init {
        orientation = VERTICAL
        // can't do in onFinishInflate as that is only called when inflating from XML
        binding = ViewFilterShowsBinding.inflate(LayoutInflater.from(context), this)

        binding.checkboxShowsFilterFavorites.setOnClickListener { updateFilterListener() }
        binding.checkboxShowsFilterUnwatched.setOnClickListener { updateFilterListener() }
        binding.checkboxShowsFilterUpcoming.setOnClickListener { updateFilterListener() }
        binding.checkboxShowsFilterHidden.setOnClickListener { updateFilterListener() }
        binding.checkboxShowsFilterContinuing.setOnClickListener { updateFilterListener() }
        binding.buttonShowsFilterRemove.setOnClickListener {
            binding.checkboxShowsFilterFavorites.state = null
            binding.checkboxShowsFilterUnwatched.state = null
            binding.checkboxShowsFilterUpcoming.state = null
            binding.checkboxShowsFilterHidden.state = null
            binding.checkboxShowsFilterContinuing.state = null
            filterListener?.onFilterUpdate(ShowFilter.allDisabled())
        }
        binding.buttonShowsFilterAllVisible.setOnClickListener { filterListener?.onMakeAllHiddenVisibleClick() }
        binding.buttonShowsFilterUpcomingRange.setOnClickListener { filterListener?.onConfigureUpcomingRangeClick() }
    }

    private var filterListener: FilterListener? = null

    private fun updateFilterListener() {
        filterListener?.onFilterUpdate(
            ShowFilter(
                binding.checkboxShowsFilterFavorites.state,
                binding.checkboxShowsFilterUnwatched.state,
                binding.checkboxShowsFilterUpcoming.state,
                binding.checkboxShowsFilterHidden.state,
                binding.checkboxShowsFilterContinuing.state
            )
        )
    }

    fun setInitialFilter(showFilter: ShowFilter) {
        binding.checkboxShowsFilterFavorites.state = showFilter.isFilterFavorites
        binding.checkboxShowsFilterUnwatched.state = showFilter.isFilterUnwatched
        binding.checkboxShowsFilterUpcoming.state = showFilter.isFilterUpcoming
        binding.checkboxShowsFilterHidden.state = showFilter.isFilterHidden
        binding.checkboxShowsFilterContinuing.state = showFilter.isFilterContinuing
    }

    fun setFilterListener(filterListener: FilterListener) {
        this.filterListener = filterListener
    }

    data class ShowFilter(
        val isFilterFavorites: Boolean?,
        val isFilterUnwatched: Boolean?,
        val isFilterUpcoming: Boolean?,
        val isFilterHidden: Boolean?,
        val isFilterContinuing: Boolean?
    ) {
        fun isAnyFilterEnabled(): Boolean {
            return isFilterFavorites != null || isFilterUnwatched != null
                    || isFilterUpcoming != null || isFilterHidden != null
                    || isFilterContinuing != null
        }

        companion object {
            @JvmStatic
            fun allDisabled(): ShowFilter {
                return ShowFilter(null, null, null, null, null)
            }

            @JvmStatic
            fun fromSettings(context: Context): ShowFilter {
                return ShowFilter(
                    ShowsDistillationSettings.isFilteringFavorites(context),
                    ShowsDistillationSettings.isFilteringUnwatched(context),
                    ShowsDistillationSettings.isFilteringUpcoming(context),
                    ShowsDistillationSettings.isFilteringHidden(context),
                    ShowsDistillationSettings.isFilteringContinuing(context)
                )
            }
        }
    }

    interface FilterListener {
        fun onFilterUpdate(filter: ShowFilter)
        fun onConfigureUpcomingRangeClick()
        fun onMakeAllHiddenVisibleClick()
    }

}