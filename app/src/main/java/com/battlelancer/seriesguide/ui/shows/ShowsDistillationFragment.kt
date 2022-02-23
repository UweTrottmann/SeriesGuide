package com.battlelancer.seriesguide.ui.shows

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.ViewPager
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider
import com.battlelancer.seriesguide.settings.AdvancedSettings
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.dialogs.SingleChoiceDialogFragment
import com.battlelancer.seriesguide.util.TaskManager
import com.battlelancer.seriesguide.util.ThemeUtils.setDefaultStyle
import com.battlelancer.seriesguide.util.safeShow
import com.uwetrottmann.seriesguide.widgets.SlidingTabLayout

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
        dialog?.window?.setGravity(Gravity.TOP or Gravity.END)

        val view = inflater.inflate(R.layout.dialog_shows_distillation, container, false)
        unbinder = ButterKnife.bind(this, view)


        val tabsAdapter = ShowsDistillationPageAdapter(
            requireContext(),
            FilterShowsView.ShowFilter.fromSettings(requireContext()),
            filterListener,
            SortShowsView.ShowSortOrder.fromSettings(requireContext()),
            sortOrderListener
        )
        viewPager.adapter = tabsAdapter
        tabLayout.setDefaultStyle()
        tabLayout.setViewPager(viewPager)

        // ensure size matches children in any case
        // (on some devices did not resize correctly, Android layouting change?)
        viewPager.post {
            @Suppress("UNNECESSARY_SAFE_CALL") // view might already been unbound
            viewPager?.requestLayout()
        }

        return view
    }

    private val filterListener = object : FilterShowsView.FilterListener {
        override fun onFilterUpdate(filter: FilterShowsView.ShowFilter) {
            // save new setting
            ShowsDistillationSettings.saveFilter(
                context!!,
                filter.isFilterFavorites,
                filter.isFilterUnwatched,
                filter.isFilterUpcoming,
                filter.isFilterHidden,
                filter.isFilterContinuing
            )

            // broadcast new filter
            ShowsDistillationSettings.filterLiveData.postValue(filter)
        }

        override fun onConfigureUpcomingRangeClick() {
            // yes, converting back to a string for comparison
            val upcomingLimit = AdvancedSettings.getUpcomingLimitInDays(requireContext()).toString()
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
                parentFragmentManager,
                R.array.upcominglimit,
                R.array.upcominglimitData,
                selectedIndex,
                AdvancedSettings.KEY_UPCOMING_LIMIT,
                R.string.pref_upcominglimit,
                "upcomingRangeDialog"
            )
        }

        override fun onMakeAllHiddenVisibleClick() {
            dismiss()
            MakeAllVisibleDialogFragment().safeShow(parentFragmentManager, "makeAllVisibleDialog")
        }

        override fun onNoReleasedChanged(value: Boolean) {
            DisplaySettings.setNoReleasedEpisodes(requireContext(), value)
            TaskManager.getInstance().tryNextEpisodeUpdateTask(requireContext())
        }

    }

    private val sortOrderListener = object : SortShowsView.SortOrderListener {
        override fun onSortOrderUpdate(showSortOrder: SortShowsView.ShowSortOrder) {
            // save new sort order to preferences
            PreferenceManager.getDefaultSharedPreferences(activity).edit {
                putInt(ShowsDistillationSettings.KEY_SORT_ORDER, showSortOrder.sortOrderId)
                putBoolean(
                    ShowsDistillationSettings.KEY_SORT_FAVORITES_FIRST,
                    showSortOrder.isSortFavoritesFirst
                )
                putBoolean(
                    DisplaySettings.KEY_SORT_IGNORE_ARTICLE,
                    showSortOrder.isSortIgnoreArticles
                )
            }

            // broadcast new sort order
            ShowsDistillationSettings.sortOrderLiveData.postValue(showSortOrder)

            if (showSortOrder.changedIgnoreArticles) {
                // refresh all list widgets
                ListWidgetProvider.notifyDataChanged(context!!)
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