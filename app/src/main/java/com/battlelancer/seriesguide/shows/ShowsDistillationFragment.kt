// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2025 Uwe Trottmann

package com.battlelancer.seriesguide.shows

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isGone
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.DialogShowsDistillationBinding
import com.battlelancer.seriesguide.settings.AdvancedSettings
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.shows.ShowsDistillationSettings.ShowFilter
import com.battlelancer.seriesguide.shows.ShowsDistillationSettings.ShowSortOrder
import com.battlelancer.seriesguide.streaming.SgWatchProvider
import com.battlelancer.seriesguide.streaming.StreamingSearchInfoDialog
import com.battlelancer.seriesguide.ui.dialogs.SingleChoiceDialogFragment
import com.battlelancer.seriesguide.util.TaskManager
import com.battlelancer.seriesguide.util.safeShow
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import kotlinx.coroutines.launch

class ShowsDistillationFragment : AppCompatDialogFragment() {

    private val activityModel: ShowsActivityViewModel by activityViewModels()
    private val model: ShowsDistillationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_SeriesGuide_Dialog_Distillation)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setGravity(Gravity.TOP or Gravity.END)

        val binding = DialogShowsDistillationBinding.inflate(inflater, container, false)

        val initialShowFilter = ShowFilter.fromSettings(requireContext())
        val initialShowSortOrder = ShowSortOrder.fromSettings(requireContext())

        binding.apply {
            filterShowsView.isGone = false
            watchProvidersFilterView.isGone = true
            sortShowsView.isGone = true

            tabLayoutShowsDistillation.addOnTabSelectedListener(object : OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    val position = tab?.position ?: return
                    filterShowsView.isGone = position != 0
                    watchProvidersFilterView.isGone = position != 1
                    sortShowsView.isGone = position != 2
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {
                }

            })

            filterShowsView.apply {
                setInitialFilter(
                    initialShowFilter,
                    DisplaySettings.isNoReleasedEpisodes(context)
                )
                setFilterListener(filterListener)
            }
            watchProvidersFilterView.apply {
                // ComposeView in a fragment: dispose already when fragment is destroyed
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    WatchProviderFilter(
                        watchProvidersFlow = model.watchProvidersFlow,
                        onProviderFilterChange = { provider: SgWatchProvider, checked: Boolean ->
                            model.changeWatchProviderFilter(
                                provider,
                                checked
                            )
                        },
                        onProviderIncludeAny = { model.removeWatchProviderFilter() },
                        onSelectRegion = { StreamingSearchInfoDialog.show(parentFragmentManager) },
                        useDynamicColor = DisplaySettings.isDynamicColorsEnabled(context)
                    )
                }
            }
            sortShowsView.apply {
                setInitialSort(initialShowSortOrder)
                setSortOrderListener(sortOrderListener)
            }

            // Display tab icon with indicator if general filter is active
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    activityModel.showsDistillationSettings.showFilter.collect {
                        tabLayoutShowsDistillation.setFilterIsActiveIcon(0, it.isAnyFilterEnabled())
                    }
                }
            }

            // Display tab icon with indicator if filtering by watch provider
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    model.watchProvidersFlow.collect { watchProviders ->
                        val isFiltering =
                            ShowsDistillationViewModel.isFilteringByWatchProviders(watchProviders)
                        tabLayoutShowsDistillation.setFilterIsActiveIcon(1, isFiltering)
                    }
                }
            }
        }

        return binding.root
    }

    private fun TabLayout.setFilterIsActiveIcon(index: Int, isFiltering: Boolean) {
        getTabAt(index)!!
            .setIcon(
                if (isFiltering) {
                    R.drawable.ic_filter_selected_white_24dp
                } else {
                    R.drawable.ic_filter_white_24dp
                }
            )
    }

    private val filterListener = object : FilterShowsView.FilterListener {
        override fun onFilterUpdate(filter: ShowFilter) {
            activityModel.showsDistillationSettings.saveFilter(filter)
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
            TaskManager.tryNextEpisodeUpdateTask(requireContext())
        }

    }

    private val sortOrderListener = object : SortShowsView.SortOrderListener {
        override fun onSortOrderUpdate(showSortOrder: ShowSortOrder) {
            activityModel.showsDistillationSettings.saveSortOrder(showSortOrder)
        }
    }

    companion object {

        private const val TAG = "shows-distillation-dialog"

        @JvmStatic
        fun show(fragmentManager: FragmentManager) {
            ShowsDistillationFragment().safeShow(fragmentManager, TAG)
        }
    }

}