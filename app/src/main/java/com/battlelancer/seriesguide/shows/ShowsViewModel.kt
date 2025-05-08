// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2025 Uwe Trottmann

package com.battlelancer.seriesguide.shows

import android.app.Application
import android.text.format.DateUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SimpleSQLiteQuery
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.settings.AdvancedSettings
import com.battlelancer.seriesguide.shows.database.SgShow2ForLists
import com.battlelancer.seriesguide.streaming.SgWatchProvider
import com.battlelancer.seriesguide.util.TimeTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber

class ShowsViewModel(application: Application) : AndroidViewModel(application) {

    data class ShowsViewUiState(
        val showFilters: ShowsDistillationSettings.ShowFilters,
        val watchProvidersFilter: List<SgWatchProvider>,
        val showSortOrder: ShowsDistillationSettings.ShowSortOrder
    ) {
        val isFiltersActive: Boolean
            get() = showFilters.isAnyFilterEnabled() || watchProvidersFilter.isNotEmpty()
    }

    private val queryString = MutableLiveData<String>()
    private val sgShowsLiveData: LiveData<MutableList<SgShow2ForLists>> =
        queryString.switchMap { queryString ->
            SgRoomDatabase.getInstance(getApplication()).sgShow2Helper()
                .getShowsLiveData(SimpleSQLiteQuery(queryString, null))
        }
    val showItemsLiveData = MediatorLiveData<MutableList<ShowsAdapter.ShowItem>?>()
    private val showItemsLiveDataSemaphore = Semaphore(1)

    private val watchProvidersFilterSource =
        SgRoomDatabase.getInstance(getApplication()).sgWatchProviderHelper()
            .filterLocalWatchProviders(SgWatchProvider.Type.SHOWS.id)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = emptyList()
            )

    val uiState = MutableStateFlow(
        ShowsViewUiState(
            showFilters = ShowsDistillationSettings.ShowFilters.fromSettings(getApplication()),
            watchProvidersFilter = watchProvidersFilterSource.value,
            showSortOrder = ShowsDistillationSettings.ShowSortOrder.fromSettings(getApplication())
        )
    )

    private var waitingQueryJob: Job? = null

    init {
        showItemsLiveData.addSource(sgShowsLiveData) { sgShows ->
            // calculate actually displayed values on a background thread
            viewModelScope.launch(Dispatchers.IO) {
                // Use Semaphore with 1 permit to ensure results are delivered in order and never
                // processed in parallel.
                showItemsLiveDataSemaphore.withPermit {
                    val mapped = sgShows?.mapTo(ArrayList(sgShows.size)) {
                        ShowsAdapter.ShowItem.map(it, getApplication())
                    }
                    showItemsLiveData.postValue(mapped)
                }
            }
        }

        // watch for watch provider filter changes
        viewModelScope.launch {
            watchProvidersFilterSource.collect {
                uiState.value = uiState.value.copy(watchProvidersFilter = it)
                updateQuery()
            }
        }
    }

    private fun Boolean?.isTrue(): Boolean {
        return this ?: false
    }

    private fun Boolean?.isFalse(): Boolean {
        if (this == null) return false
        return !this
    }

    private fun Boolean?.isNullOrFalse(): Boolean {
        if (this == null) return true
        return !this
    }

    fun updateQuery() {
        // Debounce this, notably when initially displaying to wait for all input values
        waitingQueryJob?.cancel()
        waitingQueryJob = viewModelScope.launch(SgApp.SINGLE) {
            delay(200) // below 300ms to not be perceived as lag
            waitingQueryJob = null
            Timber.d("Running query update.")
            uiState.value.also {
                updateQuery(
                    it.showFilters,
                    it.watchProvidersFilter,
                    ShowsDistillationSettings.getSortQuery2(
                        it.showSortOrder.sortOrderId, it.showSortOrder.isSortFavoritesFirst,
                        it.showSortOrder.isSortIgnoreArticles
                    )
                )
            }
        }
    }

    private fun updateQuery(
        filter: ShowsDistillationSettings.ShowFilters,
        watchProvidersFilter: List<SgWatchProvider>,
        orderClause: String
    ) {
        val selection = StringBuilder()

        // include or exclude favorites?
        filter.isFilterFavorites?.let {
            if (it) {
                selection.append(SgShow2Columns.SELECTION_FAVORITES)
            } else {
                selection.append(SgShow2Columns.SELECTION_NOT_FAVORITES)
            }
        }

        // include or exclude continuing/upcoming/pilot/in production shows?
        filter.isFilterContinuing?.let {
            if (selection.isNotEmpty()) {
                selection.append(" AND ")
            }
            if (it) {
                selection.append(SgShow2Columns.SELECTION_STATUS_CONTINUING)
            } else {
                selection.append(SgShow2Columns.SELECTION_STATUS_NO_CONTINUING)
            }
        }

        // include or exclude hidden?
        filter.isFilterHidden?.let {
            if (selection.isNotEmpty()) {
                selection.append(" AND ")
            }
            if (it) {
                selection.append(SgShow2Columns.SELECTION_HIDDEN)
            } else {
                selection.append(SgShow2Columns.SELECTION_NO_HIDDEN)
            }
        }

        // unwatched (= next episode is released) and upcoming (= next episode upcoming) filters
        // assumes that no next episode == NextEpisodeUpdater.UNKNOWN_NEXT_RELEASE_DATE

        val timeInAnHour = TimeTools.getCurrentTime(getApplication()) + DateUtils.HOUR_IN_MILLIS
        // next episode upcoming within <limit> days + 1 hour, or all future
        val upcomingLimitInDays = AdvancedSettings.getUpcomingLimitInDays(getApplication())
        val maxTimeUpcoming = if (upcomingLimitInDays != -1) {
            timeInAnHour + upcomingLimitInDays * DateUtils.DAY_IN_MILLIS
        } else {
            -1 // any future release date
        }

        if (filter.isFilterUnwatched != null || filter.isFilterUpcoming != null) {
            if (selection.isNotEmpty()) {
                selection.append(" AND ")
            }
        }

        if (filter.isFilterUnwatched.isTrue() && filter.isFilterUpcoming.isTrue()) {
            // unwatched and upcoming
            selection.append(SgShow2Columns.SELECTION_HAS_NEXT_EPISODE)
            if (maxTimeUpcoming != -1L) {
                selection.append(" AND ")
                    .append(SgShow2Columns.NEXTAIRDATEMS).append("<=").append(maxTimeUpcoming)
            }
        } else if (
            filter.isFilterUnwatched.isTrue() && filter.isFilterUpcoming.isNullOrFalse()
        ) {
            // unwatched only
            selection
                .append(SgShow2Columns.SELECTION_HAS_NEXT_EPISODE)
                .append(" AND ")
                .append(SgShow2Columns.NEXTAIRDATEMS).append("<=").append(timeInAnHour)
        } else if (
            filter.isFilterUpcoming.isTrue() && filter.isFilterUnwatched.isNullOrFalse()
        ) {
            // upcoming only
            selection
                .append(SgShow2Columns.NEXTAIRDATEMS).append(">")
                .append(timeInAnHour)
            if (maxTimeUpcoming != -1L) {
                selection.append(" AND ")
                    .append(SgShow2Columns.NEXTAIRDATEMS).append("<=").append(maxTimeUpcoming)
            }
        } else if (filter.isFilterUnwatched.isFalse()) {
            if (filter.isFilterUpcoming == null) {
                // all released episodes watched (== anything in the future or no next episode)
                // Warning: use parentheses with OR to ensure precedence!
                selection
                    .append("(")
                    .append(SgShow2Columns.NEXTAIRDATEMS).append(">")
                    .append(timeInAnHour)
                    .append(" OR ")
                    .append(SgShow2Columns.SELECTION_NO_NEXT_EPISODE)
                    .append(")")
            } else if (!filter.isFilterUpcoming) {
                // all released episodes watched plus exclude any upcoming, ignoring upcoming range
                // (== no next episode)
                selection.append(SgShow2Columns.SELECTION_NO_NEXT_EPISODE)
            }
        } else if (filter.isFilterUpcoming.isFalse() && filter.isFilterUnwatched == null) {
            // exclude any upcoming, ignoring upcoming range
            selection.append(SgShow2Columns.NEXTAIRDATEMS).append("<=").append(timeInAnHour)
        }

        // Add watch provider filter last as it needs to add a GROUP BY
        val watchProvidersCondition = watchProvidersFilter.joinToString(separator = " OR ") {
            "provider_id=${it.provider_id}"
        }
        if (watchProvidersCondition.isNotEmpty()) {
            if (selection.isNotEmpty()) {
                selection.append(" AND ")
            }
            selection.append("(").append(watchProvidersCondition).append(")")
                .append(" GROUP BY _id")
        }

        val joins = StringBuilder()
        if (watchProvidersCondition.isNotEmpty()) {
            joins.append("JOIN sg_watch_provider_show_mappings ON _id=sg_watch_provider_show_mappings.show_id")
        }
        val whereAndGroupBy = if (selection.isNotEmpty()) "WHERE $selection" else ""

        val query =
            "SELECT sg_show.* FROM ${Tables.SG_SHOW} $joins $whereAndGroupBy ORDER BY $orderClause"
        queryString.postValue(query)
    }

}