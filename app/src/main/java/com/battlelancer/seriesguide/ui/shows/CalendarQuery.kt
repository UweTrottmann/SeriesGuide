package com.battlelancer.seriesguide.ui.shows

import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase

interface CalendarQuery {
    companion object {

        val PROJECTION = arrayOf(
            SeriesGuideDatabase.Tables.EPISODES + "." + Episodes._ID,
            Episodes.TITLE,
            Episodes.NUMBER,
            Episodes.SEASON,
            Episodes.FIRSTAIREDMS,
            Episodes.WATCHED,
            Episodes.COLLECTED,
            Shows.REF_SHOW_ID,
            Shows.TITLE,
            Shows.NETWORK,
            Shows.POSTER
        )

        const val QUERY_UPCOMING = "${Episodes.FIRSTAIREDMS}>=? AND ${Episodes.FIRSTAIREDMS}<? " +
                "AND ${Shows.SELECTION_NO_HIDDEN}"

        const val QUERY_RECENT = "${Episodes.SELECTION_HAS_RELEASE_DATE} " +
                "AND ${Episodes.FIRSTAIREDMS}<? AND ${Episodes.FIRSTAIREDMS}>? " +
                "AND ${Shows.SELECTION_NO_HIDDEN}"

        const val SORTING_UPCOMING = "${Episodes.FIRSTAIREDMS} ASC," +
                "${Shows.SORT_TITLE},${Episodes.NUMBER} ASC"

        const val SORTING_RECENT = "${Episodes.FIRSTAIREDMS} DESC," +
                "${Shows.SORT_TITLE},${Episodes.NUMBER} DESC"

        const val EPISODE_TVDB_ID = 0
        const val TITLE = 1
        const val NUMBER = 2
        const val SEASON = 3
        const val RELEASE_TIME_MS = 4
        const val WATCHED = 5
        const val COLLECTED = 6
        const val SHOW_ID = 7
        const val SHOW_TITLE = 8
        const val SHOW_NETWORK = 9
        const val SHOW_POSTER_PATH = 10
    }
}
