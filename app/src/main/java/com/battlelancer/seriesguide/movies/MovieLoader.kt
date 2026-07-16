// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2020 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.movies

import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp.Companion.getServicesComponent
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.movies.database.SgMovie
import com.battlelancer.seriesguide.movies.details.UiMovieDetails
import com.battlelancer.seriesguide.movies.tools.MovieDetails
import com.battlelancer.seriesguide.movies.tools.MovieDownloader.MovieDetailsResult
import com.battlelancer.seriesguide.movies.tools.MovieTools
import com.battlelancer.seriesguide.provider.SgRoomDatabase.Companion.getInstance
import com.battlelancer.seriesguide.tmdbapi.TmdbTools
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.traktapi.TraktTools
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.RatingsTools
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.threeten.bp.Instant
import org.threeten.bp.ZonedDateTime

/**
 * Tries to load current movie details from Trakt and TMDB, on failure tries to fall back to a local
 * database copy.
 */
class MovieLoader(
    private val context: Context,
    private val tmdbId: Int
) {

    sealed interface Result {
        data class Success(val details: UiMovieDetails) : Result
        object Error : Result
    }

    suspend fun loadInBackground(): Result = withContext(Dispatchers.Default) {
        load()
    }

    private suspend fun load(): Result {
        val movieTools = getServicesComponent(context).movieTools()

        // try loading from trakt and tmdb, this might return a cached response
        val detailsResult =
            movieTools.downloader
                .getMovieDetailsWithDefaults(tmdbId, true)

        val details: MovieDetails? = when (detailsResult) {
            is MovieDetailsResult.Success -> detailsResult.movieDetails
            is MovieDetailsResult.Error -> null
        }

        if (details != null) {
            // Update local database (no-op if movie not in database).
            movieTools.updateMovieWithTmdbId(tmdbId, details)
        }

        // Fill in or use cached details from local database
        val dbMovieOrNull = getInstance(context)
            .movieHelper()
            .getMovie(tmdbId)

        // Need at least details from either TMDB or the database
        if (details == null && dbMovieOrNull == null) {
            return Result.Error
        }

        return Result.Success(
            mapToUiMovieDetails(
                tmdbId,
                details,
                dbMovieOrNull,
                context
            )
        )
    }

    /**
     * Assumes at least one of [movieDetails] or [dbMovie] is not null.
     */
    fun mapToUiMovieDetails(
        tmdbId: Int,
        movieDetails: MovieDetails?,
        dbMovie: SgMovie?,
        context: Context
    ): UiMovieDetails {
        val tmdbMovie = movieDetails?.tmdbMovie

        val title = tmdbMovie?.title ?: dbMovie?.title

        // Metacritic only has English titles so mostly English-speaking users will use it,
        // so it's likely the original language of the movie is English.
        val titleForMetacritic = if (tmdbMovie?.original_language == "en") {
            tmdbMovie.original_title
        } else title

        val isWatched = dbMovie?.watchedOrDefault ?: false
        val plays = dbMovie?.playsOrDefault ?: 0

        // Release date and running time
        val runningTime = tmdbMovie?.runtime ?: dbMovie?.runtimeMinOrDefault
        val releaseDate = tmdbMovie?.release_date
            ?: dbMovie
                ?.let { MovieTools.movieReleaseDateFrom(it.releasedMsOrDefault) }
        val releaseDateAndRunningTime = TextTools.dotSeparate(
            releaseDate?.let { TimeTools.formatToLocalDate(context, it) },
            runningTime?.let { TimeTools.formatToHoursAndMinutes(context.resources, it) }
        )

        val lastUpdatedMillis = dbMovie?.lastUpdated ?: 0

        // hide check-in if not connected to trakt or hexagon is enabled
        val isConnectedToTrakt = TraktCredentials.get(context).hasCredentials()
        val hideCheckIn = !isConnectedToTrakt || HexagonSettings.isEnabled(context)

        val collection = tmdbMovie?.belongs_to_collection

        // Genres
        val genres = tmdbMovie?.genres?.let { TmdbTools.buildGenresString(it) }
            ?: dbMovie?.genres
        val genresOrUnknown = if (genres.isNullOrEmpty()) {
            context.getString(R.string.unknown)
        } else genres

        // Poster URLs
        val posterPath = tmdbMovie?.poster_path ?: dbMovie?.poster
        val hasPosterPath = !posterPath.isNullOrEmpty()
        val posterSmallSizeImageUrl: String?
        val posterOriginalSizeImageUrl: String?
        if (hasPosterPath) {
            posterSmallSizeImageUrl =
                TmdbTools.buildLargePosterUrl(context, posterPath)
                    .let { ImageTools.buildImageCacheUrl(it) }
            posterOriginalSizeImageUrl =
                TmdbTools.buildOriginalSizeImageUrl(context, posterPath)
                    .let { ImageTools.buildImageCacheUrl(it) }
        } else {
            posterSmallSizeImageUrl = null
            posterOriginalSizeImageUrl = null
        }

        val traktRatings = movieDetails?.traktRatings

        return UiMovieDetails(
            imdbId = tmdbMovie?.imdb_id ?: dbMovie?.imdbId,
            title = title,
            titleForMetacritic = titleForMetacritic,
            // No need to set no translation available message if empty, movie downloader does
            overview = tmdbMovie?.overview ?: dbMovie?.overview,
            // Ensure lists, watched flag are false and plays 0 if db movie is not in database
            inCollection = dbMovie?.inCollectionOrDefault ?: false,
            inWatchlist = dbMovie?.inWatchlistOrDefault ?: false,
            watched = isWatched,
            plays = plays,
            releaseDate = releaseDate,
            releaseDateAndRunningTime = releaseDateAndRunningTime,
            lastUpdatedText = TimeTools.formatToLocalDateAndTime(context, lastUpdatedMillis),
            isShareButtonEnabled = title != null,
            // Hide create event button if release date is yesterday or older
            isCalendarButtonGone = releaseDate == null
                    || Instant.ofEpochMilli(releaseDate.time)
                .isBefore(ZonedDateTime.now().minusDays(1).toInstant()),
            isCheckInButtonGone = hideCheckIn,
            watchedButtonText = TextTools.getWatchedButtonText(context, isWatched, plays),
            tmdbCollectionId = collection?.id,
            tmdbCollectionName = collection?.name,
            tmdbRating = RatingsTools.buildRatingString(
                tmdbMovie?.vote_average ?: dbMovie?.ratingTmdb
            ),
            tmdbVotes = RatingsTools.buildRatingVotesString(
                context,
                tmdbMovie?.vote_count ?: dbMovie?.ratingVotesTmdb
            ),
            traktRating = RatingsTools.buildRatingString(
                traktRatings?.rating ?: dbMovie?.ratingTrakt?.toDouble()
            ),
            traktVotes = RatingsTools.buildRatingVotesString(
                context,
                traktRatings?.votes ?: dbMovie?.ratingVotesTrakt
            ),
            userRating = dbMovie?.ratingUser,
            userRatingText = TraktTools.buildUserRatingString(context, dbMovie?.ratingUser),
            genres = genresOrUnknown,
            tmdbUrl = TmdbTools.buildMovieUrl(tmdbId),
            traktUrl = TraktTools.buildMovieUrl(tmdbId),
            posterSmallSizeImageUrl = posterSmallSizeImageUrl,
            posterOriginalSizeImageUrl = posterOriginalSizeImageUrl
        )
    }
}