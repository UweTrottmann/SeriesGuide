package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.preference.PreferenceManager
import androidx.lifecycle.MutableLiveData
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows

/**
 * Provides settings used to filter and sort displayed shows in [ShowsFragment].
 */
object ShowsDistillationSettings {

    internal val filterLiveData = MutableLiveData<FilterShowsView.ShowFilter>()
    internal val sortOrderLiveData = MutableLiveData<SortShowsView.ShowSortOrder>()

    internal const val KEY_SORT_ORDER = "com.battlelancer.seriesguide.sort.order"
    internal const val KEY_SORT_FAVORITES_FIRST = "com.battlelancer.seriesguide.sort.favoritesfirst"
    internal const val KEY_FILTER_FAVORITES = "com.battlelancer.seriesguide.filter.favorites"
    internal const val KEY_FILTER_UNWATCHED = "com.battlelancer.seriesguide.filter.unwatched"
    internal const val KEY_FILTER_UPCOMING = "com.battlelancer.seriesguide.filter.upcoming"
    internal const val KEY_FILTER_HIDDEN = "com.battlelancer.seriesguide.filter.hidden"

    /**
     * Builds an appropriate SQL sort statement for sorting shows.
     */
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

    internal fun isFilteringFavorites(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_FILTER_FAVORITES, false)
    }

    internal fun isFilteringUnwatched(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_FILTER_UNWATCHED, false)
    }

    internal fun isFilteringUpcoming(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_FILTER_UPCOMING, false)
    }

    internal fun isFilteringHidden(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_FILTER_HIDDEN, false)
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
