// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2025 Uwe Trottmann

package com.battlelancer.seriesguide.shows

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.settings.DisplaySettings
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Provides settings used to filter and sort displayed shows in [ShowsFragment].
 */
class ShowsDistillationSettings(
    private val context: Context
) {

    /**
     * Initially the current value, emits when the filter settings were changed with [saveFilter].
     */
    val showFilter = MutableStateFlow(ShowFilter.fromSettings(context))

    /**
     * Initially the current value, emits when the sort order was changed with [saveSortOrder].
     */
    val sortOrder = MutableStateFlow(ShowSortOrder.fromSettings(context))

    fun saveFilter(showFilter: ShowFilter) {
        // Save setting
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putInt(KEY_FILTER_FAVORITES, showFilter.isFilterFavorites.mapFilterState())
            putInt(KEY_FILTER_UNWATCHED, showFilter.isFilterUnwatched.mapFilterState())
            putInt(KEY_FILTER_UPCOMING, showFilter.isFilterUpcoming.mapFilterState())
            putInt(KEY_FILTER_HIDDEN, showFilter.isFilterHidden.mapFilterState())
            putInt(KEY_FILTER_CONTINUING, showFilter.isFilterContinuing.mapFilterState())
        }
        // Broadcast new value
        this.showFilter.value = showFilter
    }

    fun saveSortOrder(showSortOrder: ShowSortOrder) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putInt(KEY_SORT_ORDER, showSortOrder.sortOrderId)
            putBoolean(
                KEY_SORT_FAVORITES_FIRST,
                showSortOrder.isSortFavoritesFirst
            )
            putBoolean(
                DisplaySettings.KEY_SORT_IGNORE_ARTICLE,
                showSortOrder.isSortIgnoreArticles
            )
        }

        // broadcast new sort order
        sortOrder.value = showSortOrder

        // Note: List widgets continue to share the ignore articles setting
        if (showSortOrder.changedIgnoreArticles) {
            // refresh all list widgets
            ListWidgetProvider.notifyDataChanged(context)
        }
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
            fun default(): ShowFilter {
                // Exclude hidden, all others disabled.
                return ShowFilter(null, null, null, false, null)
            }

            fun fromSettings(context: Context): ShowFilter {
                return ShowFilter(
                    isFilteringFavorites(context),
                    isFilteringUnwatched(context),
                    isFilteringUpcoming(context),
                    isFilteringHidden(context),
                    isFilteringContinuing(context)
                )
            }
        }
    }

    data class ShowSortOrder(
        val sortOrderId: Int,
        val isSortFavoritesFirst: Boolean,
        val isSortIgnoreArticles: Boolean,
        val changedIgnoreArticles: Boolean
    ) {
        companion object {

            const val TITLE_ID = 0
            // @deprecated Only supporting alphabetical sort order going forward.
            // int TITLE_REVERSE_ID = 1;
            const val OLDEST_EPISODE_ID = 2
            const val LATEST_EPISODE_ID = 3
            const val LAST_WATCHED_ID = 4
            const val LEAST_REMAINING_EPISODES_ID = 5
            const val STATUS = 6

            fun fromSettings(context: Context): ShowSortOrder {
                return ShowSortOrder(
                    getSortOrderId(context),
                    isSortFavoritesFirst(context),
                    DisplaySettings.isSortOrderIgnoringArticles(context),
                    false
                )
            }
        }
    }

    companion object {

        private const val KEY_SORT_ORDER = "com.battlelancer.seriesguide.sort.order"
        private const val KEY_SORT_FAVORITES_FIRST =
            "com.battlelancer.seriesguide.sort.favoritesfirst"
        private const val KEY_FILTER_FAVORITES = "seriesguide.show_filter.favorites"
        private const val KEY_FILTER_UNWATCHED = "seriesguide.show_filter.unwatched"
        private const val KEY_FILTER_UPCOMING = "seriesguide.show_filter.upcoming"
        private const val KEY_FILTER_HIDDEN = "seriesguide.show_filter.hidden"
        private const val KEY_FILTER_CONTINUING = "seriesguide.show_filter.continuing"

        private object SgShow2SortQuery {
            // by oldest next episode, then continued first (for no next episode)
            const val OLDEST_EPISODE =
                "${SgShow2Columns.NEXTAIRDATEMS} ASC,${SgShow2Columns.SORT_STATUS},"

            // by latest next episode, then continued first (for no next episode)
            const val LATEST_EPISODE = "${SgShow2Columns.SORT_LATEST_EPISODE_THEN_STATUS},"

            // by latest watched first
            const val LAST_WATCHED = "${SgShow2Columns.LASTWATCHED_MS} DESC,"

            // by least episodes remaining to watch, then continued first (for no remaining episode)
            const val REMAINING_EPISODES =
                "${SgShow2Columns.UNWATCHED_COUNT} ASC,${SgShow2Columns.SORT_STATUS},"

            const val STATUS = "${SgShow2Columns.STATUS} DESC,"

            // add as prefix to sort favorites first
            const val FAVORITES_FIRST = "${SgShow2Columns.FAVORITE} DESC,"
        }

        /**
         * Builds an appropriate SQL sort statement for sorting SgShow2 table results.
         */
        @JvmStatic
        fun getSortQuery2(
            sortOrderId: Int,
            isSortFavoritesFirst: Boolean,
            isSortIgnoreArticles: Boolean
        ): String {
            val query = StringBuilder()

            if (isSortFavoritesFirst) {
                query.append(SgShow2SortQuery.FAVORITES_FIRST)
            }

            when (sortOrderId) {
                ShowSortOrder.OLDEST_EPISODE_ID -> query.append(SgShow2SortQuery.OLDEST_EPISODE)
                ShowSortOrder.LATEST_EPISODE_ID -> query.append(SgShow2SortQuery.LATEST_EPISODE)
                ShowSortOrder.LAST_WATCHED_ID -> query.append(SgShow2SortQuery.LAST_WATCHED)
                ShowSortOrder.LEAST_REMAINING_EPISODES_ID -> query.append(SgShow2SortQuery.REMAINING_EPISODES)
                ShowSortOrder.STATUS -> query.append(SgShow2SortQuery.STATUS)
            }
            // always sort by title at last
            query.append(
                if (isSortIgnoreArticles) {
                    SgShow2Columns.SORT_TITLE_NOARTICLE
                } else {
                    SgShow2Columns.SORT_TITLE
                }
            )

            return query.toString()
        }

        /**
         * Returns the id as of [ShowsDistillationSettings.ShowSortOrder]
         * of the current show sort order.
         */
        internal fun getSortOrderId(context: Context): Int {
            return PreferenceManager.getDefaultSharedPreferences(context).getInt(KEY_SORT_ORDER, 0)
        }

        internal fun isSortFavoritesFirst(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_SORT_FAVORITES_FIRST, true)
        }

        internal fun isFilteringFavorites(context: Context): Boolean? {
            return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(KEY_FILTER_FAVORITES, FILTER_DISABLED)
                .mapFilterState()
        }

        internal fun isFilteringUnwatched(context: Context): Boolean? {
            return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(KEY_FILTER_UNWATCHED, FILTER_DISABLED)
                .mapFilterState()
        }

        internal fun isFilteringUpcoming(context: Context): Boolean? {
            return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(KEY_FILTER_UPCOMING, FILTER_DISABLED)
                .mapFilterState()
        }

        internal fun isFilteringHidden(context: Context): Boolean? {
            return PreferenceManager.getDefaultSharedPreferences(context)
                // exclude hidden shows by default
                .getInt(KEY_FILTER_HIDDEN, FILTER_EXCLUDE)
                .mapFilterState()
        }

        internal fun isFilteringContinuing(context: Context): Boolean? {
            return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(KEY_FILTER_CONTINUING, FILTER_DISABLED)
                .mapFilterState()
        }

        private const val FILTER_INCLUDE = 1
        private const val FILTER_EXCLUDE = -1
        private const val FILTER_DISABLED = 0

        private fun Int.mapFilterState(): Boolean? {
            return when (this) {
                FILTER_INCLUDE -> true
                FILTER_EXCLUDE -> false
                else -> null
            }
        }

        private fun Boolean?.mapFilterState(): Int {
            return when (this) {
                null -> FILTER_DISABLED
                true -> FILTER_INCLUDE
                false -> FILTER_EXCLUDE
            }
        }
    }
}
