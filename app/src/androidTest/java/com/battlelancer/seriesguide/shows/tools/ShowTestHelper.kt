// SPDX-License-Identifier: Apache-2.0
// Copyright 2022-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.tools

import com.battlelancer.seriesguide.shows.database.SgEpisode2
import com.battlelancer.seriesguide.shows.database.SgSeason2
import com.battlelancer.seriesguide.shows.database.SgShow2
import com.battlelancer.seriesguide.util.LanguageTools
import com.battlelancer.seriesguide.util.TimeTools

object ShowTestHelper {

    fun showToInsert(): SgShow2 {
        return SgShow2(
            tmdbId = 5159,
            tvdbId = null,
            traktId = null,
            title = "The Test Show",
            titleNoArticle = "Test Show",
            overview = "Test Show Overview",
            releaseTime = 1415,
            releaseWeekDay = TimeTools.parseShowReleaseWeekDay("Friday"),
            releaseCountry = "de",
            releaseTimeZone = "Europe/Berlin",
            customReleaseTime = null,
            customReleaseDayOffset = null,
            customReleaseTimeZone = null,
            firstRelease = "2022-05-20T14:15:00.0Z",
            ratingTmdb = null,
            ratingTmdbVotes = null,
            ratingTrakt = null,
            ratingTraktVotes = null,
            ratingUser = null,
            genres = "Genre",
            network = "Network",
            imdbId = "imdbid",
            runtime = 45,
            status = ShowStatus.RETURNING,
            poster = "poster.jpg",
            posterSmall = "poster.jpg",
            // set desired language, might not be the content language if fallback used above.
            language = LanguageTools.LANGUAGE_EN,
            lastUpdatedMs = System.currentTimeMillis() // now
        )
    }

    fun seasonToInsert(showId: Long, number: Int): SgSeason2 {
        return SgSeason2(
            showId = showId,
            tmdbId = "1",
            numberOrNull = number,
            order = number,
            name = "Season $number"
        )
    }

    fun SgSeason2.episodeToInsert(seasonId: Long, number: Int, releaseDateTimeMs: Long): SgEpisode2 {
        return SgEpisode2(
            showId = showId,
            seasonId = seasonId,
            tmdbId = 1,
            title = "Episode $number",
            overview = "Episode $number description",
            number = number,
            order = number,
            season = this.number,
            image = "episode_image.jpg",
            firstReleasedMs = releaseDateTimeMs,
            directors = "directors",
            guestStars = "guest stars",
            writers = "writers",
            ratingTmdb = null,
            ratingTmdbVotes = null,
            ratingTrakt = null,
            ratingTraktVotes = null,
            ratingUser = null
        )
    }

}