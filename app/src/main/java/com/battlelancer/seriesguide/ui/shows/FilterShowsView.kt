package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import butterknife.BindView
import butterknife.ButterKnife
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.widgets.FilterBox

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

        ViewTools.setVectorIcon(
            context.theme,
            buttonUpcomingRange,
            R.drawable.ic_settings_white_24dp
        )

        checkBoxFavorites.setOnClickListener { updateFilterListener() }
        checkBoxUnwatched.setOnClickListener { updateFilterListener() }
        checkBoxUpcoming.setOnClickListener { updateFilterListener() }
        checkBoxHidden.setOnClickListener { updateFilterListener() }
        buttonClearFilters.setOnClickListener {
            checkBoxFavorites.state = null
            checkBoxUnwatched.state = null
            checkBoxUpcoming.state = null
            checkBoxHidden.state = null
            filterListener?.onFilterUpdate(ShowFilter.allDisabled())
        }
        buttonUpcomingRange.setOnClickListener { filterListener?.onConfigureUpcomingRangeClick() }
    }

    @BindView(R.id.checkbox_shows_filter_favorites)
    internal lateinit var checkBoxFavorites: FilterBox
    @BindView(R.id.checkbox_shows_filter_unwatched)
    internal lateinit var checkBoxUnwatched: FilterBox
    @BindView(R.id.checkbox_shows_filter_upcoming)
    internal lateinit var checkBoxUpcoming: FilterBox
    @BindView(R.id.checkbox_shows_filter_hidden)
    internal lateinit var checkBoxHidden: FilterBox
    @BindView(R.id.button_shows_filter_remove)
    internal lateinit var buttonClearFilters: Button
    @BindView(R.id.button_shows_filter_upcoming_range)
    internal lateinit var buttonUpcomingRange: ImageButton

    private var filterListener: FilterListener? = null

    private fun updateFilterListener() {
        filterListener?.onFilterUpdate(
            ShowFilter(
                checkBoxFavorites.state,
                checkBoxUnwatched.state,
                checkBoxUpcoming.state,
                checkBoxHidden.state
            )
        )
    }

    fun setInitialFilter(showFilter: ShowFilter) {
        checkBoxFavorites.state = showFilter.isFilterFavorites
        checkBoxUnwatched.state = showFilter.isFilterUnwatched
        checkBoxUpcoming.state = showFilter.isFilterUpcoming
        checkBoxHidden.state = showFilter.isFilterHidden
    }

    fun setFilterListener(filterListener: FilterListener) {
        this.filterListener = filterListener
    }

    data class ShowFilter(
        val isFilterFavorites: Boolean?,
        val isFilterUnwatched: Boolean?,
        val isFilterUpcoming: Boolean?,
        val isFilterHidden: Boolean?
    ) {
        fun isAnyFilterEnabled(): Boolean {
            return isFilterFavorites != null || isFilterUnwatched != null
                    || isFilterUpcoming != null || isFilterHidden != null
        }

        companion object {
            @JvmStatic
            fun allDisabled(): ShowFilter {
                return ShowFilter(null, null, null, null)
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