package com.battlelancer.seriesguide.ui.shows

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager.widget.ViewPager
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider
import com.battlelancer.seriesguide.settings.AdvancedSettings
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences
import com.battlelancer.seriesguide.ui.dialogs.SingleChoiceDialogFragment
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.safeShow
import com.battlelancer.seriesguide.widgets.SlidingTabLayout

class ShowsDistillationFragment : AppCompatDialogFragment() {

    @BindView(R.id.tabLayoutShowsDistillation)
    internal lateinit var tabLayout: SlidingTabLayout
    @BindView(R.id.viewPagerShowsDistillation)
    internal lateinit var viewPager: ViewPager

    private lateinit var unbinder: Unbinder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_SeriesGuide_Dialog_Distillation)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog.window?.setGravity(Gravity.TOP or Gravity.END)

        val view = inflater.inflate(R.layout.dialog_shows_distillation, container, false)
        unbinder = ButterKnife.bind(this, view)


        val tabsAdapter = ShowsDistillationPageAdapter(
            context!!,
            FilterShowsView.ShowFilter.fromSettings(context!!),
            filterListener,
            SortShowsView.ShowSortOrder.fromSettings(context!!),
            sortOrderListener
        )
        viewPager.adapter = tabsAdapter
        tabLayout.setCustomTabView(
            R.layout.tabstrip_item_allcaps_transparent,
            R.id.textViewTabStripItem
        )
        tabLayout.setSelectedIndicatorColors(
            ContextCompat.getColor(
                context!!,
                if (SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_DarkBlue) {
                    R.color.white
                } else {
                    Utils.resolveAttributeToResourceId(context!!.theme, R.attr.colorPrimary)
                }
            )
        )
        tabLayout.setViewPager(viewPager)

        return view
    }

    private val filterListener = object : FilterShowsView.FilterListener {
        override fun onFilterUpdate(filter: FilterShowsView.ShowFilter) {
            // save new setting
            PreferenceManager.getDefaultSharedPreferences(activity).edit()
                .putBoolean(
                    ShowsDistillationSettings.KEY_FILTER_FAVORITES, filter.isFilterFavorites
                )
                .putBoolean(
                    ShowsDistillationSettings.KEY_FILTER_UNWATCHED, filter.isFilterUnwatched
                )
                .putBoolean(ShowsDistillationSettings.KEY_FILTER_UPCOMING, filter.isFilterUpcoming)
                .putBoolean(ShowsDistillationSettings.KEY_FILTER_HIDDEN, filter.isFilterHidden)
                .apply()

            // broadcast new filter
            ShowsDistillationSettings.filterLiveData.postValue(filter)
        }

        override fun onConfigureUpcomingRangeClick() {
            // yes, converting back to a string for comparison
            val upcomingLimit = AdvancedSettings.getUpcomingLimitInDays(activity).toString()
            val filterRanges = resources.getStringArray(R.array.upcominglimitData)
            var selectedIndex = 0
            var i = 0
            val filterRangesLength = filterRanges.size
            while (i < filterRangesLength) {
                val range = filterRanges[i]
                if (upcomingLimit == range) {
                    selectedIndex = i
                    break
                }
                i++
            }

            SingleChoiceDialogFragment.show(
                fragmentManager,
                R.array.upcominglimit,
                R.array.upcominglimitData,
                selectedIndex,
                AdvancedSettings.KEY_UPCOMING_LIMIT,
                R.string.pref_upcominglimit,
                "upcomingRangeDialog"
            )
        }

    }

    private val sortOrderListener = object : SortShowsView.SortOrderListener {
        override fun onSortOrderUpdate(showSortOrder: SortShowsView.ShowSortOrder) {
            // save new sort order to preferences
            PreferenceManager.getDefaultSharedPreferences(activity).edit()
                .putInt(ShowsDistillationSettings.KEY_SORT_ORDER, showSortOrder.sortOrderId)
                .putBoolean(
                    ShowsDistillationSettings.KEY_SORT_FAVORITES_FIRST,
                    showSortOrder.isSortFavoritesFirst
                )
                .putBoolean(
                    DisplaySettings.KEY_SORT_IGNORE_ARTICLE,
                    showSortOrder.isSortIgnoreArticles
                )
                .apply()

            // broadcast new sort order
            ShowsDistillationSettings.sortOrderLiveData.postValue(showSortOrder)

            if (showSortOrder.changedIgnoreArticles) {
                // refresh all list widgets
                ListWidgetProvider.notifyDataChanged(context)
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder.unbind()
    }

    companion object {

        private const val TAG = "shows-distillation-dialog"

        @JvmStatic
        fun show(fragmentManager: FragmentManager) {
            ShowsDistillationFragment().safeShow(fragmentManager, TAG)
        }
    }

}