// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide.shows.tools

import com.battlelancer.seriesguide.shows.database.SgShow2
import com.battlelancer.seriesguide.util.LanguageTools
import com.battlelancer.seriesguide.util.TimeTools

// Note there is a copy of this in the androidTest source set
object ShowTestHelper {

    fun showToInsert() =
        SgShow2(
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
            lastUpdatedMs = System.currentTimeMillis(), // now
            userNote = null,
            userNoteTraktId = null
        )

}