// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.compose.ui.platform.ComposeView
import androidx.viewpager.widget.PagerAdapter
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.streaming.SgWatchProvider

class ShowsDistillationPageAdapter(
    private val context: Context,
    private val initialShowFilter: ShowsDistillationSettings.ShowFilter,
    private val filterListener: FilterShowsView.FilterListener,
    private val initialShowSortOrder: SortShowsView.ShowSortOrder,
    private val sortOrderListener: SortShowsView.SortOrderListener,
    private val showsDistillationViewModel: ShowsDistillationViewModel
) : PagerAdapter() {

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val page = DistillationPages.values()[position]
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val view = when (page) {
            DistillationPages.FILTER -> {
                FilterShowsView(context).apply {
                    this.layoutParams = layoutParams
                    setInitialFilter(
                        initialShowFilter,
                        DisplaySettings.isNoReleasedEpisodes(context)
                    )
                    setFilterListener(filterListener)
                }
            }

            DistillationPages.FILTER_WATCH_PROVIDERS -> {
                ComposeView(context).apply {
                    this.layoutParams = layoutParams
                    setContent {
                        WatchProviderFilter(
                            showsDistillationUiState = showsDistillationViewModel.showsDistillationUiState,
                            onProviderFilterChange = { provider: SgWatchProvider, checked: Boolean ->
                                showsDistillationViewModel.changeWatchProviderFilter(
                                    provider,
                                    checked
                                )
                            })
                    }
                }
            }

            DistillationPages.SORT -> {
                SortShowsView(context).apply {
                    this.layoutParams = layoutParams
                    setInitialSort(initialShowSortOrder)
                    setSortOrderListener(sortOrderListener)
                }
            }
        }
        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, pageObject: Any) {
        container.removeView(pageObject as View)
    }

    override fun isViewFromObject(view: View, pageObject: Any): Boolean {
        return view == pageObject
    }

    override fun getCount(): Int {
        return DistillationPages.values().size
    }

    override fun getPageTitle(position: Int): CharSequence {
        val page = DistillationPages.values()[position]
        return context.getString(page.titleRes)
    }

    enum class DistillationPages(@StringRes val titleRes: Int) {
        FILTER(R.string.action_shows_filter),
        FILTER_WATCH_PROVIDERS(R.string.action_stream),
        SORT(R.string.action_shows_sort)
    }
}