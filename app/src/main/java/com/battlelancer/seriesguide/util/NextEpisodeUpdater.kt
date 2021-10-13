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
            val plays = if (show.episodePlays == null || show.episodePlays == 0) {
                1
            } else show.episodePlays
            // Note: Due to LEFT JOIN query, episode values are null if no matching episode found.
            if (show.lastWatchedEpisodeId == 0L || season == null || number == null) {
                // by default: no watched episodes, include all starting with specials season 0
                season = -1
                number = -1
            }

            // STEP 2: get episode with less plays and closest higher number,
            // otherwise first of higher season.
            // Note: previously this selected the next episode based
            // on it having a higher release date. This was done to handle special episodes
            // releasing in between regular episodes. However, when e.g. skipping all special
            // episodes this may select an unexpected regular episode; it also failed when
            // the next episode does not have a release date, yet.
            val selectionArgs: Array<Any> = if (isNoReleasedEpisodes) {
                // restrict to episodes with future release date
                arrayOf(
                    plays, season, number, season, currentTime
                )
            } else {
                // any episodes
                arrayOf(
                    plays, season, number, season
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
        }
        // Otherwise include any, even without release date (== UNKNOWN_NEXT_RELEASE_DATE),
        // sometimes release dates get added rather late or never. However, currently
        // show list filters for upcoming/unwatched would exclude these.
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
         * Less plays, not skipped, if in the same season has a higher number,
         * otherwise has a higher season.
         */
        private const val SELECT_NEXT =
            (SgEpisode2Columns.PLAYS + "<? AND " + SgEpisode2Columns.SELECTION_NOT_SKIPPED + " AND ("
                    + "(" + SgEpisode2Columns.SEASON + "=? AND " + SgEpisode2Columns.NUMBER + ">?) "
                    + "OR " + SgEpisode2Columns.SEASON + ">?"
                    + ")")

        private const val SELECT_ONLYFUTURE = " AND " + SgEpisode2Columns.FIRSTAIREDMS + ">=?"

        /**
         * Lowest season first, or if identical lowest episode number.
         */
        private const val SORTORDER = (SgEpisode2Columns.SEASON + " ASC,"
                + SgEpisode2Columns.NUMBER + " ASC")
    }

}