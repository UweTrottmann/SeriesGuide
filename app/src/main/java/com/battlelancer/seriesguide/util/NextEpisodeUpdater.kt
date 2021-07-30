package com.battlelancer.seriesguide.util

import android.content.Context
import androidx.sqlite.db.SimpleSQLiteQuery
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.provider.SgShow2LastWatchedEpisode
import com.battlelancer.seriesguide.provider.SgShow2NextEpisodeUpdate
import com.battlelancer.seriesguide.settings.DisplaySettings
import timber.log.Timber
import java.util.ArrayList

class NextEpisodeUpdater {

    /**
     * Update next episode field and unwatched episode count for the given show. If no show id is
     * passed, will update next episodes for all shows.
     *
     * @return If only one show was passed, the row id of the new next episode. Otherwise -1.
     */
    fun updateForShows(context: Context, showIdOrNull: Long?): Long {
        // Get a list of shows and their last watched episodes.
        val shows: List<SgShow2LastWatchedEpisode>
        val database = SgRoomDatabase.getInstance(context)
        val showHelper = database.sgShow2Helper()
        if (showIdOrNull != null) {
            val show = showHelper.getShowWithLastWatchedEpisode(showIdOrNull)
            if (show == null) {
                Timber.e("Failed to update next episode, show does not exist: %s", showIdOrNull)
                return -1 // Fail, show does not (longer) exist.
            }
            shows = ArrayList()
            shows.add(show)
        } else {
            shows = showHelper.getShowsWithLastWatchedEpisode()
        }

        // pre-build next episode selection
        val isNoReleasedEpisodes = DisplaySettings.isNoReleasedEpisodes(context)
        val nextEpisodeSelection = buildNextEpisodeSelection(
            DisplaySettings.isHidingSpecials(context), isNoReleasedEpisodes
        )

        // build updated next episode values for each show
        val batch: MutableList<SgShow2NextEpisodeUpdate> = ArrayList()
        var nextEpisodeIdResult: Long = -1
        val episodeHelper = database.sgEpisode2Helper()
        val currentTime = TimeTools.getCurrentTime(context)
        val preventSpoilers = DisplaySettings.preventSpoilers(context)
        for (show in shows) {
            // STEP 1: get last watched episode details
            var season = show.seasonNumber
            var number = show.episodeNumber
            var releaseTime = show.episodeReleaseDateMs
            // Note: Due to LEFT JOIN query, episode values are null if no matching episode found.
            if (show.lastWatchedEpisodeId == 0L
                || season == null || number == null || releaseTime == null) {
                // by default: no watched episodes, include all starting with special 0
                season = -1
                number = -1
                releaseTime = Long.MIN_VALUE
            }

            // STEP 2: get episode released closest afterwards; or at the same time,
            // but with a higher number
            val selectionArgs: Array<Any> = if (isNoReleasedEpisodes) {
                // restrict to episodes with future release date
                arrayOf(
                    releaseTime, number, season, releaseTime, currentTime
                )
            } else {
                // restrict to episodes with any valid air date
                arrayOf(
                    releaseTime, number, season, releaseTime
                )
            }
            val episodeOrNull = episodeHelper
                .getEpisodeInfo(
                    SimpleSQLiteQuery(
                        "SELECT * FROM " + SeriesGuideDatabase.Tables.SG_EPISODE
                                + " WHERE " + SgShow2Columns.REF_SHOW_ID + " = " + show.id
                                + " AND " + nextEpisodeSelection
                                + " ORDER BY " + SORTORDER
                                + " LIMIT 1",
                        selectionArgs
                    )
                )

            // STEP 3: get remaining episodes count
            val unwatchedEpisodesCount = episodeHelper
                .countNotWatchedEpisodesOfShow(show.id, currentTime)

            // STEP 4: build updated next episode values
            var update: SgShow2NextEpisodeUpdate
            if (episodeOrNull != null) {
                val nextEpisodeString: String = TextTools.getNextEpisodeString(
                    context,
                    episodeOrNull.season,
                    episodeOrNull.episodenumber,
                    if (preventSpoilers) {
                        null // just the number, like '0x12 Episode 12'
                    } else {
                        // next episode text, like '0x12 Episode Name'
                        episodeOrNull.title
                    }
                )
                // next release date text, e.g. "in 15 mins (Fri)"
                val releaseTimeNext = episodeOrNull.firstReleasedMs

                nextEpisodeIdResult = episodeOrNull.id
                update = SgShow2NextEpisodeUpdate(
                    show.id, nextEpisodeIdResult.toString(),
                    releaseTimeNext,
                    nextEpisodeString,
                    unwatchedEpisodesCount
                )
            } else {
                // no next episode, set empty values
                nextEpisodeIdResult = 0
                update = SgShow2NextEpisodeUpdate(
                    show.id,
                    "",
                    UNKNOWN_NEXT_RELEASE_DATE,
                    "",
                    unwatchedEpisodesCount
                )
            }
            batch.add(update)
        }

        // Update shows in database with new next episode values.
        val rowsUpdated = showHelper.updateShowNextEpisode(batch)
        if (rowsUpdated < 0) {
            Timber.e("Failed to apply show next episode db update.")
            return -1
        }

        return nextEpisodeIdResult
    }

    private fun buildNextEpisodeSelection(
        isHidingSpecials: Boolean,
        isNoReleasedEpisodes: Boolean
    ): String {
        val nextEpisodeSelectionBuilder = StringBuilder(SELECT_NEXT)
        if (isHidingSpecials) {
            // do not take specials into account
            nextEpisodeSelectionBuilder.append(" AND ")
                .append(SgEpisode2Columns.SELECTION_NO_SPECIALS)
        }
        if (isNoReleasedEpisodes) {
            // restrict to episodes with future release date
            nextEpisodeSelectionBuilder.append(SELECT_ONLYFUTURE)
        } else {
            // restrict to episodes with any valid air date
            nextEpisodeSelectionBuilder.append(SELECT_WITHAIRDATE)
        }
        return nextEpisodeSelectionBuilder.toString()
    }

    companion object {
        /**
         * Used for show next episode time value, see [SgShow2Columns.NEXTAIRDATEMS].
         * Ensures these shows are sorted last if sorting by oldest episode first,
         * and first if sorting by latest episode. Also affects filter settings.
         * See [com.battlelancer.seriesguide.ui.shows.ShowsViewModel] and
         * [com.battlelancer.seriesguide.ui.shows.ShowsDistillationSettings].
         */
        const val UNKNOWN_NEXT_RELEASE_DATE = Long.MAX_VALUE

        /**
         * Unwatched, airing later or has a different number or season if airing the same time.
         */
        var SELECT_NEXT = (SgEpisode2Columns.WATCHED + "=0 AND ("
                + "(" + SgEpisode2Columns.FIRSTAIREDMS + "=? AND "
                + "(" + SgEpisode2Columns.NUMBER + "!=? OR " + SgEpisode2Columns.SEASON + "!=?)) "
                + "OR " + SgEpisode2Columns.FIRSTAIREDMS + ">?)")

        var SELECT_WITHAIRDATE = " AND " + SgEpisode2Columns.FIRSTAIREDMS + "!=-1"

        var SELECT_ONLYFUTURE = " AND " + SgEpisode2Columns.FIRSTAIREDMS + ">=?"

        /**
         * Air time, then lowest season, or if identical lowest episode number.
         */
        var SORTORDER = (SgEpisode2Columns.FIRSTAIREDMS + " ASC,"
                + SgEpisode2Columns.SEASON + " ASC,"
                + SgEpisode2Columns.NUMBER + " ASC")
    }

}