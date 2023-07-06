package com.battlelancer.seriesguide.dataliberation

import com.battlelancer.seriesguide.dataliberation.model.Episode
import com.battlelancer.seriesguide.dataliberation.model.Season
import com.battlelancer.seriesguide.dataliberation.model.Show
import com.battlelancer.seriesguide.shows.database.SgEpisode2
import com.battlelancer.seriesguide.shows.database.SgSeason2
import com.battlelancer.seriesguide.shows.database.SgShow2
import com.battlelancer.seriesguide.shows.episodes.EpisodeFlags
import com.battlelancer.seriesguide.util.LanguageTools
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import kotlin.math.absoluteValue

object ImportTools {

    @JvmStatic
    fun Show.toSgShowForImport(): SgShow2 {
        return SgShow2(
            tmdbId = tmdb_id,
            tvdbId = tvdb_id,
            traktId = trakt_id ?: 0,
            title = title ?: "",
            titleNoArticle = TextTools.trimLeadingArticle(title),
            overview = overview ?: "",
            releaseTime = release_time,
            releaseWeekDay = if (release_weekday >= -1 && release_weekday <= 7) release_weekday else TimeTools.RELEASE_WEEKDAY_UNKNOWN,
            releaseCountry = country,
            releaseTimeZone = release_timezone,
            // Note: do net set default values for custom time, set to null instead. This avoids
            // restoring an old backup overwriting values in Cloud on next sync.
            customReleaseTime = custom_release_time?.takeIf { it in 0..2359 },
            customReleaseDayOffset = custom_release_day_offset?.takeIf { it.absoluteValue <= SgShow2.MAX_CUSTOM_DAY_OFFSET },
            customReleaseTimeZone = custom_release_timezone,
            firstRelease = first_aired,
            ratingGlobal = if (rating in 0.0..10.0) rating else 0.0,
            ratingVotes = if (rating_votes >= 0) rating_votes else 0,
            genres = genres ?: "",
            network = network ?: "",
            imdbId = imdb_id ?: "",
            runtime = if (runtime >= 0) runtime else 0,
            status = DataLiberationTools.encodeShowStatus(status),
            poster = poster ?: "",
            posterSmall = poster ?: "",
            language = language ?: LanguageTools.LANGUAGE_EN,
            lastUpdatedMs = 0, // never, e.g. update next.
            favorite = favorite,
            notify = notify ?: true,
            hidden = hidden,
            lastWatchedMs = last_watched_ms,
            ratingUser = rating_user
        )
    }

    @JvmStatic
    fun Season.toSgSeasonForImport(showId: Long): SgSeason2 {
        return SgSeason2(
            showId = showId,
            tmdbId = tmdb_id,
            tvdbId = tvdbId,
            numberOrNull = season,
            order = season,
            name = null
        )
    }

    @JvmStatic
    fun Episode.toSgEpisodeForImport(showId: Long, seasonId: Long, seasonNumber: Int): SgEpisode2 {
        val ratingUser = rating_user?.let { if (it in 0..10) it else 0 } ?: 0
        val ratingGlobal = rating?.let { if (it in 0.0..10.0) it else 0.0 } ?: 0.0
        return SgEpisode2(
            showId = showId,
            seasonId = seasonId,
            tmdbId = tmdb_id,
            tvdbId = tvdbId,
            title = title ?: "",
            overview = overview,
            number = episode,
            absoluteNumber = episodeAbsolute,
            dvdNumber = episodeDvd,
            order = episode,
            season = seasonNumber,
            image = image ?: "",
            firstReleasedMs = firstAired,
            directors = directors ?: "",
            guestStars = gueststars ?: "",
            writers = writers ?: "",
            watched = if (skipped) EpisodeFlags.SKIPPED else if (watched) EpisodeFlags.WATCHED else EpisodeFlags.UNWATCHED,
            collected = collected,
            plays = if (watched && plays >= 1) plays else if (watched) 1 else 0,
            ratingUser = ratingUser,
            ratingGlobal = ratingGlobal,
            ratingVotes = rating_votes?.let { if (it >= 0) it else 0 } ?: 0
        )
    }

}