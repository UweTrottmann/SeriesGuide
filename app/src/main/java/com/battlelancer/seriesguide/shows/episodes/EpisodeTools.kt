// SPDX-License-Identifier: Apache-2.0
// Copyright 2013-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.episodes

import android.content.Context
import com.battlelancer.seriesguide.jobs.FlagJobExecutor
import com.battlelancer.seriesguide.jobs.episodes.EpisodeCollectedJob
import com.battlelancer.seriesguide.jobs.episodes.EpisodeWatchedJob
import com.battlelancer.seriesguide.jobs.episodes.EpisodeWatchedUpToJob
import com.battlelancer.seriesguide.jobs.episodes.SeasonCollectedJob
import com.battlelancer.seriesguide.jobs.episodes.SeasonWatchedJob
import com.battlelancer.seriesguide.jobs.episodes.ShowCollectedJob
import com.battlelancer.seriesguide.jobs.episodes.ShowWatchedJob
import com.battlelancer.seriesguide.util.TimeTools

object EpisodeTools {

    fun isCollected(collectedFlag: Int): Boolean =
        collectedFlag == 1

    fun isSkipped(episodeFlags: Int): Boolean =
        episodeFlags == EpisodeFlags.SKIPPED

    fun isUnwatched(episodeFlags: Int): Boolean =
        episodeFlags == EpisodeFlags.UNWATCHED

    fun isWatched(episodeFlags: Int): Boolean =
        episodeFlags == EpisodeFlags.WATCHED

    fun isValidEpisodeFlag(episodeFlags: Int): Boolean =
        isUnwatched(episodeFlags) || isSkipped(episodeFlags) || isWatched(episodeFlags)

    fun validateFlags(episodeFlags: Int) {
        require(isValidEpisodeFlag(episodeFlags)) {
            "Did not pass a valid episode flag. See EpisodeFlags class for details."
        }
    }

    fun episodeWatched(context: Context, episodeId: Long, episodeFlags: Int) {
        validateFlags(episodeFlags)
        FlagJobExecutor.execute(
            context,
            EpisodeWatchedJob(episodeId, episodeFlags)
        )
    }

    fun episodeWatchedIfNotZero(context: Context, episodeIdOrZero: Long) {
        if (episodeIdOrZero > 0) {
            episodeWatched(context, episodeIdOrZero, EpisodeFlags.WATCHED)
        }
    }

    fun episodeCollected(context: Context, episodeId: Long, isCollected: Boolean) {
        FlagJobExecutor.execute(context, EpisodeCollectedJob(episodeId, isCollected))
    }

    /**
     * See [EpisodeWatchedUpToJob].
     */
    fun episodeWatchedUpTo(
        context: Context,
        showId: Long,
        episodeFirstAired: Long,
        episodeNumber: Int
    ) {
        FlagJobExecutor.execute(
            context,
            EpisodeWatchedUpToJob(showId, episodeFirstAired, episodeNumber)
        )
    }

    fun seasonWatched(context: Context, seasonId: Long, episodeFlags: Int) {
        validateFlags(episodeFlags)
        FlagJobExecutor.execute(
            context,
            SeasonWatchedJob(seasonId, episodeFlags)
        )
    }

    fun seasonCollected(context: Context, seasonId: Long, isCollected: Boolean) {
        FlagJobExecutor.execute(
            context,
            SeasonCollectedJob(seasonId, isCollected)
        )
    }

    fun showWatched(context: Context, showId: Long, isFlag: Boolean) {
        FlagJobExecutor.execute(
            context,
            ShowWatchedJob(
                showId,
                if (isFlag) EpisodeFlags.WATCHED else EpisodeFlags.UNWATCHED,
                TimeTools.getCurrentTime(context)
            )
        )
    }

    fun showCollected(context: Context, showId: Long, isCollected: Boolean) {
        FlagJobExecutor.execute(
            context,
            ShowCollectedJob(showId, isCollected)
        )
    }

}
