package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.preference.PreferenceManager
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows

/**
 * Provides settings used to filter and sort displayed shows in [ShowsFragment].
 */
object ShowsDistillationSettings {

    @JvmField
    val filterLiveData = MutableLiveData<FilterShowsView.ShowFilter>()
    @JvmField
    val sortOrderLiveData = MutableLiveData<SortShowsView.ShowSortOrder>()

    internal const val KEY_SORT_ORDER = "com.battlelancer.seriesguide.sort.order"
    internal const val KEY_SORT_FAVORITES_FIRST = "com.battlelancer.seriesguide.sort.favoritesfirst"
    private const val KEY_FILTER_FAVORITES = "seriesguide.show_filter.favorites"
    private const val KEY_FILTER_UNWATCHED = "seriesguide.show_filter.unwatched"
    private const val KEY_FILTER_UPCOMING = "seriesguide.show_filter.upcoming"
    private const val KEY_FILTER_HIDDEN = "seriesguide.show_filter.hidden"

    /**
     * Builds an appropriate SQL sort statement for sorting shows.
     */
    @JvmStatic
    fun getSortQuery(
        sortOrderId: Int, isSortFavoritesFirst: Boolean,
        isSortIgnoreArticles: Boolean
    ): String {
        val query = StringBuilder()

        if (isSortFavoritesFirst) {
            query.append(ShowsSortQuery.FAVORITES_FIRST)
        }

        when (sortOrderId) {
            ShowsSortOrder.OLDEST_EPISODE_ID -> query.append(ShowsSortQuery.OLDEST_EPISODE)
            ShowsSortOrder.LATEST_EPISODE_ID -> query.append(ShowsSortQuery.LATEST_EPISODE)
            ShowsSortOrder.LAST_WATCHED_ID -> query.append(ShowsSortQuery.LAST_WATCHED)
            ShowsSortOrder.LEAST_REMAINING_EPISODES_ID -> query.append(ShowsSortQuery.REMAINING_EPISODES)
        }
        // always sort by title at last
        query.append(
            if (isSortIgnoreArticles) {
                Shows.SORT_TITLE_NOARTICLE
            } else {
                Shows.SORT_TITLE
            }
        )

        return query.toString()
    }

    /**
     * Returns the id as of [ShowsDistillationSettings.ShowsSortOrder]
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

    @JvmStatic
    fun saveFilter(
        context: Context,
        isFilteringFavorites: Boolean?,
        isFilteringUnwatched: Boolean?,
        isFilteringUpcoming: Boolean?,
        isFilteringHidden: Boolean?
    ) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putInt(KEY_FILTER_FAVORITES, isFilteringFavorites.mapFilterState())
            putInt(KEY_FILTER_UNWATCHED, isFilteringUnwatched.mapFilterState())
            putInt(KEY_FILTER_UPCOMING, isFilteringUpcoming.mapFilterState())
            putInt(KEY_FILTER_HIDDEN, isFilteringHidden.mapFilterState())
        }
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

    private interface ShowsSortQuery {
        companion object {
            // by oldest next episode, then continued first
            const val OLDEST_EPISODE = (Shows.NEXTAIRDATEMS + " ASC,"
                    + Shows.SORT_STATUS + ",")
            // by latest next episode, then continued first
            const val LATEST_EPISODE = Shows.SORT_LATEST_EPISODE + ","
            // by latest watched first
            const val LAST_WATCHED = Shows.LASTWATCHED_MS + " DESC,"
            // by least episodes remaining to watch, then continued first
            const val REMAINING_EPISODES = (Shows.UNWATCHED_COUNT + " ASC,"
                    + Shows.SORT_STATUS + ",")
            // add as prefix to sort favorites first
            const val FAVORITES_FIRST = Shows.FAVORITE + " DESC,"
        }
    }

    /**
     * Used by [ShowsFragment] loader to sort the list of
     * shows.
     */
    interface ShowsSortOrder {
        companion object {
            const val TITLE_ID = 0
            // @deprecated Only supporting alphabetical sort order going forward.
            // int TITLE_REVERSE_ID = 1;
            const val OLDEST_EPISODE_ID = 2
            const val LATEST_EPISODE_ID = 3
            const val LAST_WATCHED_ID = 4
            const val LEAST_REMAINING_EPISODES_ID = 5
        }
    }
}
