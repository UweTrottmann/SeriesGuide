package com.battlelancer.seriesguide.shows.tools

import android.content.Context
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.shows.database.SgEpisode2Helper
import com.battlelancer.seriesguide.shows.database.SgShow2Helper
import com.battlelancer.seriesguide.shows.database.SgShow2LastWatchedEpisode
import com.battlelancer.seriesguide.shows.database.SgShow2NextEpisodeUpdate
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import timber.log.Timber

class NextEpisodeUpdater(
    private val context: Context,
    private val showHelper: SgShow2Helper,
    private val episodeHelper: SgEpisode2Helper
) {

    constructor(context: Context) : this(
        context,
        SgRoomDatabase.getInstance(context).sgShow2Helper(),
        SgRoomDatabase.getInstance(context).sgEpisode2Helper()
    )

    /**
     * Update next episode field and unwatched episode count for the given show. If no show id is
     * passed, will update next episodes for all shows.
     *
     * @return If only one show was passed, the row id of the new next episode. May be 0 if there is
     * no next episode. On failure -1.
     */
    fun updateForShows(showIdOrNull: Long?): Long {
        // Get a list of shows and their last watched episodes.
        val shows: List<SgShow2LastWatchedEpisode>
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
        val currentTime = TimeTools.getCurrentTime(context)
        val preventSpoilers = DisplaySettings.preventSpoilers(context)

        // Received crashes on Android 5.1 where iterator is null,
        // though can't reproduce on emulator.
        @Suppress("RedundantNullableReturnType")
        val iterator: Iterator<SgShow2LastWatchedEpisode>? = shows.iterator()
        if (iterator == null) {
            Timber.e("iterator is null showIdOrNull=$showIdOrNull")
            return -1
        }

        for (show in iterator) {
            // STEP 1: get last watched episode details
            var season = show.seasonNumber
            var number = show.episodeNumber
            var releaseTime = show.episodeReleaseDateMs
            val plays = if (show.episodePlays == null || show.episodePlays == 0) {
                1
            } else show.episodePlays
            // Note: Due to LEFT JOIN query, episode values are null if no matching episode found
            // or there is no last watched episode ID stored.
            if (show.lastWatchedEpisodeId == 0L
                || season == null || number == null || releaseTime == null) {
                // If there is no info about a last watched episode
                // use the newest watched/skipped episode, if there is one.
                // This is useful when adding a show from Cloud/Trakt or restoring a backup where
                // there is no last watched episode set on the show.
                val newestWatched = episodeHelper.getNewestWatchedEpisodeOfShow(show.id)
                // Otherwise assume all episodes unwatched: include all starting with special 0.
                season = newestWatched?.season ?: -1
                number = newestWatched?.episodenumber ?: -1
                releaseTime = newestWatched?.firstReleasedMs ?: Long.MIN_VALUE
            }

            // STEP 2: get episode released closest afterwards
            // (next episode or special episode - important for Anime, also often special episodes
            // are not added in the order they are released, see e.g. Daemon Slayer);
            // or at the same time, but with a different number (if all episodes are released at once).
            // Note: there is a setting to exclude special episodes.
            val selectionArgs: Array<Any> = if (isNoReleasedEpisodes) {
                // restrict to episodes with future release date
                arrayOf(
                    plays, releaseTime, number, season, releaseTime, currentTime
                )
            } else {
                // restrict to episodes with any valid air date
                arrayOf(
                    plays, releaseTime, number, season, releaseTime
                )
            }
            val episodeOrNull = episodeHelper.getEpisodeInfo(
                show.id,
                nextEpisodeSelection,
                SORTORDER,
                selectionArgs
            )

            // STEP 3: get remaining episodes count
            val unwatchedEpisodesCount = episodeHelper
                .countNotWatchedEpisodesOfShow(show.id, currentTime)

            // STEP 4: build updated next episode values
            val update: SgShow2NextEpisodeUpdate
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
            // currently show list filters and UI expect a
            // valid release date if there is a next episode
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
         * Less plays, not skipped, released later
         * or has a different (to also match specials) number or season if released the same time.
         */
        private const val SELECT_NEXT =
            (SgEpisode2Columns.PLAYS + "<? AND " + SgEpisode2Columns.SELECTION_NOT_SKIPPED + " AND ("
                    + "(" + SgEpisode2Columns.FIRSTAIREDMS + "=? AND "
                    + "(" + SgEpisode2Columns.NUMBER + "!=? OR " + SgEpisode2Columns.SEASON + "!=?)) "
                    + "OR " + SgEpisode2Columns.FIRSTAIREDMS + ">?)")

        private const val SELECT_WITHAIRDATE = " AND " + SgEpisode2Columns.FIRSTAIREDMS + "!=-1"

        private const val SELECT_ONLYFUTURE = " AND " + SgEpisode2Columns.FIRSTAIREDMS + ">=?"

        /**
         * Oldest release date first, then lowest season, then lowest episode number.
         */
        private const val SORTORDER = (SgEpisode2Columns.FIRSTAIREDMS + " ASC,"
                + SgEpisode2Columns.SEASON + " ASC,"
                + SgEpisode2Columns.NUMBER + " ASC")
    }

}