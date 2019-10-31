package com.battlelancer.seriesguide.util

import android.content.Context
import android.database.Cursor
import android.text.format.DateUtils
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.shows.CalendarFragment2.CalendarType
import com.battlelancer.seriesguide.ui.shows.CalendarQuery

object CalendarUtils {

    private const val ACTIVITY_DAY_LIMIT = 30

    /**
     * Returns an array of size 3. The built query is stored in `[0][0]`, the built selection
     * args in `[1]` and the sort order in `[2][0]`.
     *
     * @param type A [CalendarType], defaults to UPCOMING.
     * @param isInfinite If false, limits the release time range of returned episodes to
     * [ACTIVITY_DAY_LIMIT] days from today.
     * @return Cursor with projection [CalendarQuery].
     */
    @JvmStatic
    fun calendarQuery(
        context: Context, type: CalendarType,
        isOnlyPremieres: Boolean,
        isOnlyCollected: Boolean,
        isOnlyFavorites: Boolean,
        isOnlyUnwatched: Boolean,
        isInfinite: Boolean
    ): Cursor? {
        // go an hour back in time, so episodes move to recent one hour late
        val recentThreshold = TimeTools.getCurrentTime(context) - DateUtils.HOUR_IN_MILLIS

        val query: StringBuilder
        val selectionArgs: Array<String>
        val sortOrder: String
        val timeThreshold: Long

        if (CalendarType.RECENT == type) {
            query = StringBuilder(CalendarQuery.QUERY_RECENT)
            sortOrder = CalendarQuery.SORTING_RECENT
            timeThreshold = if (isInfinite) {
                // to the past!
                java.lang.Long.MIN_VALUE
            } else {
                // last x days
                System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS * ACTIVITY_DAY_LIMIT
            }
        } else {
            query = StringBuilder(CalendarQuery.QUERY_UPCOMING)
            sortOrder = CalendarQuery.SORTING_UPCOMING
            timeThreshold = if (isInfinite) {
                // to the future!
                java.lang.Long.MAX_VALUE
            } else {
                // coming x days
                System.currentTimeMillis() + DateUtils.DAY_IN_MILLIS * ACTIVITY_DAY_LIMIT
            }
        }

        selectionArgs = arrayOf(recentThreshold.toString(), timeThreshold.toString())

        // append only favorites selection if necessary
        if (isOnlyFavorites) {
            query.append(" AND ").append(Shows.SELECTION_FAVORITES)
        }

        // append no specials selection if necessary
        val isNoSpecials = DisplaySettings.isHidingSpecials(context)
        if (isNoSpecials) {
            query.append(" AND ").append(Episodes.SELECTION_NO_SPECIALS)
        }

        // append unwatched selection if necessary
        if (isOnlyUnwatched) {
            query.append(" AND ").append(Episodes.SELECTION_UNWATCHED)
        }

        // only show collected episodes
        if (isOnlyCollected) {
            query.append(" AND ").append(Episodes.SELECTION_COLLECTED)
        }

        // Only premieres (first episodes).
        if (isOnlyPremieres) {
            query.append(" AND ").append(Episodes.SELECTION_ONLY_PREMIERES)
        }

        // build result array
        val args = arrayOf(
            arrayOf(query.toString()),
            selectionArgs,
            arrayOf(sortOrder)
        )

        return context.contentResolver.query(
            Episodes.CONTENT_URI_WITHSHOW,
            CalendarQuery.PROJECTION, args[0][0], args[1], args[2][0]
        )
    }

}