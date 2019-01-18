package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import butterknife.BindView
import butterknife.ButterKnife
import com.battlelancer.seriesguide.R

class FilterShowsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_filter_shows, this)

        // can't do in onFinishInflate as that is only called when inflating from XML
        ButterKnife.bind(this)

        checkBoxFavorites.setOnClickListener { updateFilterListener() }
        checkBoxUnwatched.setOnClickListener { updateFilterListener() }
        checkBoxUpcoming.setOnClickListener { updateFilterListener() }
        checkBoxHidden.setOnClickListener { updateFilterListener() }
        buttonClearFilters.setOnClickListener {
            checkBoxFavorites.isChecked = false
            checkBoxUnwatched.isChecked = false
            checkBoxUpcoming.isChecked = false
            checkBoxHidden.isChecked = false
            filterListener?.onFilterUpdate(ShowFilter.allDisabled())
        }
        buttonUpcomingRange.setOnClickListener { filterListener?.onConfigureUpcomingRangeClick() }
    }

    @BindView(R.id.checkbox_shows_filter_favorites)
    internal lateinit var checkBoxFavorites: CheckBox
    @BindView(R.id.checkbox_shows_filter_unwatched)
    internal lateinit var checkBoxUnwatched: CheckBox
    @BindView(R.id.checkbox_shows_filter_upcoming)
    internal lateinit var checkBoxUpcoming: CheckBox
    @BindView(R.id.checkbox_shows_filter_hidden)
    internal lateinit var checkBoxHidden: CheckBox
    @BindView(R.id.button_shows_filter_remove)
    internal lateinit var buttonClearFilters: Button
    @BindView(R.id.button_shows_filter_upcoming_range)
    internal lateinit var buttonUpcomingRange: Button

    private var filterListener: FilterListener? = null

    private fun updateFilterListener() {
        filterListener?.onFilterUpdate(
            ShowFilter(
                checkBoxFavorites.isChecked,
                checkBoxUnwatched.isChecked,
                checkBoxUpcoming.isChecked,
                checkBoxHidden.isChecked
            )
        )
    }

    fun setInitialFilter(showFilter: ShowFilter) {
        checkBoxFavorites.isChecked = showFilter.isFilterFavorites
        checkBoxUnwatched.isChecked = showFilter.isFilterUnwatched
        checkBoxUpcoming.isChecked = showFilter.isFilterUpcoming
        checkBoxHidden.isChecked = showFilter.isFilterHidden
    }

    fun setFilterListener(filterListener: FilterListener) {
        this.filterListener = filterListener
    }

    data class ShowFilter(
        val isFilterFavorites: Boolean,
        val isFilterUnwatched: Boolean,
        val isFilterUpcoming: Boolean,
        val isFilterHidden: Boolean
    ) {
        fun isAnyFilterEnabled(): Boolean {
            return isFilterFavorites || isFilterUnwatched || isFilterUpcoming || isFilterHidden
        }

        companion object {
            @JvmStatic
            fun allDisabled(): ShowFilter {
                return ShowFilter(false, false, false, false)
            }

            @JvmStatic
            fun fromSettings(context: Context): ShowFilter {
                return ShowFilter(
                    ShowsDistillationSettings.isFilteringFavorites(context),
                    ShowsDistillationSettings.isFilteringUnwatched(context),
                    ShowsDistillationSettings.isFilteringUpcoming(context),
                    ShowsDistillationSettings.isFilteringHidden(context)
                )
            }
        }
    }

    interface FilterListener {
        fun onFilterUpdate(filter: ShowFilter)
        fun onConfigureUpcomingRangeClick()
    }

}