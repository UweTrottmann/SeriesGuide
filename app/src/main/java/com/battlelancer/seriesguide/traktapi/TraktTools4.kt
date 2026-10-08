// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.traktapi

import com.battlelancer.seriesguide.traktapi.TraktTools4.awaitTraktCall
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.entities.AddNoteRequest
import com.uwetrottmann.trakt5.entities.BaseMovie
import com.uwetrottmann.trakt5.entities.BaseShow
import com.uwetrottmann.trakt5.entities.MovieIds
import com.uwetrottmann.trakt5.entities.Note
import com.uwetrottmann.trakt5.entities.RatedEpisode
import com.uwetrottmann.trakt5.entities.RatedMovie
import com.uwetrottmann.trakt5.entities.RatedShow
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.entities.ShowIds
import com.uwetrottmann.trakt5.entities.SyncEpisode
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncMovie
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.entities.SyncSeason
import com.uwetrottmann.trakt5.entities.SyncShow
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.ExtendedShowsWatched
import com.uwetrottmann.trakt5.enums.IdType
import com.uwetrottmann.trakt5.enums.Rating
import com.uwetrottmann.trakt5.enums.RatingsFilter
import com.uwetrottmann.trakt5.enums.Specials
import com.uwetrottmann.trakt5.enums.Type
import retrofit2.Call
import retrofit2.awaitResponse
import timber.log.Timber

/**
 * Uses response classes inheriting from a Kotlin sealed interface.
 *
 * Functions don't require a [android.content.Context] and no longer rely on a third-party library
 * to handle results.
 */
object TraktTools4 {

    // 250 is the maximum limit according to the Trakt [Upcoming API Changes: Pagination & Sorting Updates](https://github.com/trakt/trakt-api/discussions/681)
    // discussion.
    private const val MAX_LIMIT = 250

    sealed interface TraktResponse<T> {
        data class Success<T>(
            /**
             * If T is [Void] this is always `null`.
             */
            val data: T?,
            /**
             * If returned, the number of available pages for a paginated endpoint.
             */
            val pageCount: Int?
        ) : TraktResponse<T>
    }

    sealed interface TraktNonNullResponse<T> {
        data class Success<T>(
            val data: T,
            /**
             * If returned, the number of available pages for a paginated endpoint.
             */
            val pageCount: Int?
        ) : TraktNonNullResponse<T>
    }

    sealed interface TraktErrorResponse {
        class IsNotVip<T> : TraktResponse<T>, TraktNonNullResponse<T>
        class IsUnauthorized<T> : TraktResponse<T>, TraktNonNullResponse<T>
        class IsAccountLimitExceeded<T> : TraktResponse<T>, TraktNonNullResponse<T>
        class IsAccountLocked<T> : TraktResponse<T>, TraktNonNullResponse<T>
        class Other<T> : TraktResponse<T>, TraktNonNullResponse<T>
    }

    /**
     * May return `null` data, for example if the movie wasn't found.
     */
    suspend fun getMovieIds(
        trakt: TraktV2,
        movieTmdbId: Int
    ): TraktNonNullResponse<MovieIds?> {
        val response = awaitTraktCallNonNull(
            trakt,
            trakt.search().idLookup(IdType.TMDB, movieTmdbId.toString(), Type.MOVIE, null, 1, 1),
            "movie trakt ids lookup",
            reportIsNotVip = true // Should work even if not VIP
        )
        return mapResponseData(response) {
            it.firstOrNull()?.movie?.ids
        }
    }

    /**
     * If [noSeasons] is `true`, only show info is available. Starting 2026-05-30 also full info.
     * If it's `false`, seasons and episodes are available. Starting 2026-05-30 also full info.
     *
     * See the Trakt [Upcoming API Changes: Watched Endpoints Pagination & Extended Defaults](https://github.com/trakt/trakt-api/discussions/775)
     * discussion about details and updates.
     */
    suspend fun getWatchedShows(
        trakt: TraktV2,
        noSeasons: Boolean
    ): TraktNonNullResponse<List<BaseShow>> {
        return fetchAllPages(
            trakt,
            action = "get watched shows",
            reportIsNotVip = true // Should work even if not VIP
        ) { page ->
            trakt.sync().watchedShows(
                page,
                MAX_LIMIT,
                if (noSeasons) {
                    // As of 2026-05-30 this should be the default, still request until then
                    // https://github.com/trakt/trakt-api/discussions/775
                    @Suppress("DEPRECATION")
                    ExtendedShowsWatched.NOSEASONS
                } else {
                    // This should only work starting 2026-05-30, but already request it
                    // https://github.com/trakt/trakt-api/discussions/775
                    ExtendedShowsWatched.PROGRESS
                },
                if (noSeasons) null else Specials.TRUE
            )
        }
    }

    suspend fun getWatchedShowsByTmdbId(
        trakt: TraktV2
    ): TraktNonNullResponse<Map<Int, BaseShow>> {
        val response = getWatchedShows(trakt, noSeasons = false)
        return mapResponseData(response) { mapByTmdbId(it) }
    }

    suspend fun getCollectedShows(
        trakt: TraktV2
    ): TraktNonNullResponse<List<BaseShow>> {
        return fetchAllPages(
            trakt,
            action = "get collected shows",
            reportIsNotVip = true // Should work even if not VIP
        ) { page ->
            trakt.sync().collectionShows(page, MAX_LIMIT, null)
        }
    }

    suspend fun getCollectedShowsByTmdbId(
        trakt: TraktV2
    ): TraktNonNullResponse<Map<Int, BaseShow>> {
        val response = getCollectedShows(trakt)
        return mapResponseData(response) { mapByTmdbId(it) }
    }

    /**
     * Fetches all pages from a paginated Trakt API endpoint.
     *
     * @param trakt Used to parse the Trakt error message for error reports
     * @param action Description of the action for error logging
     * @param reportIsNotVip Whether to report "not VIP" errors
     * @param callProvider Function that creates a Call for a given page number
     * @return All items from all pages combined, or an error response
     */
    private suspend fun <T> fetchAllPages(
        trakt: TraktV2,
        action: String,
        reportIsNotVip: Boolean = false,
        callProvider: (page: Int) -> Call<List<T>>
    ): TraktNonNullResponse<List<T>> {
        val allItems = mutableListOf<T>()
        var currentPage = 1
        var totalPageCount: Int?

        do {
            val response = awaitTraktCallNonNull(
                trakt,
                callProvider(currentPage),
                action,
                reportIsNotVip = reportIsNotVip
            )

            when (response) {
                is TraktNonNullResponse.Success -> {
                    allItems.addAll(response.data)
                    totalPageCount = response.pageCount
                    if (totalPageCount == null) {
                        Timber.w("Page count header not found for '$action'")
                    }
                    currentPage++
                }

                is TraktErrorResponse.IsNotVip -> return TraktErrorResponse.IsNotVip()
                is TraktErrorResponse.IsUnauthorized -> return TraktErrorResponse.IsUnauthorized()
                is TraktErrorResponse.IsAccountLimitExceeded -> return TraktErrorResponse.IsAccountLimitExceeded()
                is TraktErrorResponse.IsAccountLocked -> return TraktErrorResponse.IsAccountLocked()
                is TraktErrorResponse.Other -> return TraktErrorResponse.Other()
            }
        } while (totalPageCount != null && currentPage <= totalPageCount)

        return TraktNonNullResponse.Success(allItems, totalPageCount)
    }

    private fun mapByTmdbId(traktShows: List<BaseShow>): Map<Int, BaseShow> {
        val traktShowsMap = HashMap<Int, BaseShow>(traktShows.size)
        for (traktShow in traktShows) {
            val tmdbId = traktShow.show?.ids?.tmdb
            if (tmdbId == null || traktShow.seasons.isNullOrEmpty()) {
                continue  // trakt show misses required data, skip.
            }
            traktShowsMap[tmdbId] = traktShow
        }
        return traktShowsMap
    }

    suspend fun getShowsOnWatchlist(
        trakt: TraktV2
    ): TraktNonNullResponse<List<BaseShow>> {
        return fetchAllPages(
            trakt,
            action = "get shows on watchlist",
            reportIsNotVip = true // Should work even if not VIP
        ) { page ->
            // Use Extended.FULL to get show metadata
            trakt.sync().watchlistShows(page, MAX_LIMIT, Extended.FULL)
        }
    }

    /**
     * Adds the show to the watchlist.
     *
     * Check the response with [isNotFound]. See [awaitTraktCall] for details.
     */
    suspend fun addShowToWatchlist(
        trakt: TraktV2,
        showTmdbId: Int
    ): TraktNonNullResponse<SyncResponse> {
        return awaitTraktCallNonNull(
            trakt,
            trakt.sync().addItemsToWatchlist(buildWatchlistItems(showTmdbId)),
            "add show to watchlist",
            reportIsNotVip = true // Should work even if not VIP
        )
    }

    /**
     * Removes the show from the watchlist.
     *
     * Check the response with [isNotFound]. See [awaitTraktCall] for details.
     */
    suspend fun removeShowFromWatchlist(
        trakt: TraktV2,
        showTmdbId: Int
    ): TraktNonNullResponse<SyncResponse> {
        return awaitTraktCallNonNull(
            trakt,
            trakt.sync().deleteItemsFromWatchlist(buildWatchlistItems(showTmdbId)),
            "remove show from watchlist",
            reportIsNotVip = true // Should work even if not VIP
        )
    }

    private fun buildWatchlistItems(showTmdbId: Int): SyncItems {
        return SyncItems().shows(SyncShow().id(ShowIds.tmdb(showTmdbId)))
    }

    suspend fun getRatingsOfShows(
        trakt: TraktV2
    ): TraktNonNullResponse<List<RatedShow>> {
        return fetchAllPages(
            trakt,
            action = "get show ratings",
            reportIsNotVip = true // Should work even if not VIP
        ) { page ->
            trakt.sync().ratingsShows(RatingsFilter.ALL, null, page, MAX_LIMIT)
        }
    }

    suspend fun getRatingsOfEpisodes(
        trakt: TraktV2
    ): TraktNonNullResponse<List<RatedEpisode>> {
        return fetchAllPages(
            trakt,
            action = "get episode ratings",
            reportIsNotVip = true // Should work even if not VIP
        ) { page ->
            trakt.sync().ratingsEpisodes(RatingsFilter.ALL, null, page, MAX_LIMIT)
        }
    }

    /**
     * Adds the [rating] to the show. If [rating] is null, removes the rating.
     *
     * Check the response with [isNotFound]. See [awaitTraktCall] for details.
     */
    suspend fun rateShow(
        trakt: TraktV2,
        showTmdbId: Int,
        rating: Rating?
    ): TraktNonNullResponse<SyncResponse> {
        val items = SyncItems()
            .shows(SyncShow().id(ShowIds.tmdb(showTmdbId)).rating(rating))
        return sendRatings(trakt, items, rating, "rate show")
    }

    /**
     * Like [rateShow], but for an episode.
     */
    suspend fun rateEpisode(
        trakt: TraktV2,
        showTmdbId: Int,
        season: Int,
        episode: Int,
        rating: Rating?
    ): TraktNonNullResponse<SyncResponse> {
        val items = SyncItems()
            .shows(
                SyncShow().id(ShowIds.tmdb(showTmdbId))
                    .seasons(
                        SyncSeason().number(season)
                            .episodes(
                                SyncEpisode().number(episode)
                                    .rating(rating)
                            )
                    )
            )
        return sendRatings(trakt, items, rating, "rate episode")
    }

    /**
     * Adds the ratings, or removes them if [rating] is null.
     */
    private suspend fun sendRatings(
        trakt: TraktV2,
        items: SyncItems,
        rating: Rating?,
        action: String
    ): TraktNonNullResponse<SyncResponse> {
        val call = if (rating != null) {
            trakt.sync().addRatings(items)
        } else {
            trakt.sync().deleteRatings(items)
        }
        return awaitTraktCallNonNull(
            trakt,
            call,
            action,
            reportIsNotVip = true // Should work even if not VIP
        )
    }

    suspend fun getWatchedMoviesByTmdbId(
        trakt: TraktV2
    ): TraktNonNullResponse<MutableMap<Int, Int>> {
        val response = fetchAllPages(
            trakt,
            action = "get watched movies",
            reportIsNotVip = true // Should work even if not VIP
        ) { page ->
            trakt.sync().watchedMovies(page, MAX_LIMIT, null)
        }
        return mapResponseData(response) { mapMoviesToTmdbIdWithPlays(it) }
    }

    private fun mapMoviesToTmdbIdWithPlays(traktMovies: List<BaseMovie>): MutableMap<Int, Int> {
        val map: MutableMap<Int, Int> = HashMap(traktMovies.size)
        for (movie in traktMovies) {
            val tmdbId = movie.movie?.ids?.tmdb
                ?: continue // skip invalid values
            map[tmdbId] = movie.plays
        }
        return map
    }

    suspend fun getCollectedMoviesByTmdbId(
        trakt: TraktV2
    ): TraktNonNullResponse<MutableSet<Int>> {
        val response = fetchAllPages(
            trakt,
            action = "get collected movies",
            reportIsNotVip = true // Should work even if not VIP
        ) { page ->
            trakt.sync().collectionMovies(page, MAX_LIMIT, null)
        }
        return mapResponseData(response) { mapMoviesToTmdbIdSet(it) }
    }

    suspend fun getMoviesOnWatchlistByTmdbId(
        trakt: TraktV2
    ): TraktNonNullResponse<MutableSet<Int>> {
        val response = fetchAllPages(
            trakt,
            action = "get movie watchlist",
            reportIsNotVip = true // Should work even if not VIP
        ) { page ->
            trakt.sync().watchlistMovies(page, MAX_LIMIT, null)
        }
        return mapResponseData(response) { mapMoviesToTmdbIdSet(it) }
    }

    private fun mapMoviesToTmdbIdSet(traktMovies: List<BaseMovie>): MutableSet<Int> {
        val tmdbIdSet: MutableSet<Int> = HashSet(traktMovies.size)
        for (movie in traktMovies) {
            val tmdbId = movie.movie?.ids?.tmdb
                ?: continue // skip invalid values
            tmdbIdSet.add(tmdbId)
        }
        return tmdbIdSet
    }

    suspend fun getRatingsOfMovies(
        trakt: TraktV2
    ): TraktNonNullResponse<List<RatedMovie>> {
        return fetchAllPages(
            trakt,
            action = "get movie ratings",
            reportIsNotVip = true // Should work even if not VIP
        ) { page ->
            trakt.sync().ratingsMovies(RatingsFilter.ALL, null, page, MAX_LIMIT)
        }
    }

    /**
     * Like [rateShow], but for a movie.
     */
    suspend fun rateMovie(
        trakt: TraktV2,
        movieTmdbId: Int,
        rating: Rating?
    ): TraktNonNullResponse<SyncResponse> {
        val items = SyncItems()
            .movies(SyncMovie().id(MovieIds.tmdb(movieTmdbId)).rating(rating))
        return sendRatings(trakt, items, rating, "rate movie")
    }

    /**
     * Adds or updates the note for the given show.
     *
     * See [awaitTraktCall] for details.
     */
    suspend fun saveNoteForShow(
        trakt: TraktV2,
        showTmdbId: Int,
        noteText: String
    ): TraktNonNullResponse<Note> {
        // Note: calling the add endpoint for an existing note will update it
        return awaitTraktCallNonNull(
            trakt,
            trakt.notes().addNote(
                AddNoteRequest(
                    Show().apply {
                        ids = ShowIds.tmdb(showTmdbId)
                    },
                    noteText
                )
            ),
            "update note",
            reportIsNotVip = true // Should work even if not VIP
        )
    }

    /**
     * See [awaitTraktCall] for details.
     */
    suspend fun deleteNote(
        trakt: TraktV2,
        noteId: Long
    ): TraktResponse<Void> {
        return awaitTraktCall(
            trakt,
            trakt.notes().deleteNote(noteId),
            "delete note",
            reportIsNotVip = true // Should work even if not VIP
        )
    }

    /**
     * Returns `true` if Trakt could not find any of the movies, shows or episodes of a sync
     * request.
     */
    fun isNotFound(response: SyncResponse): Boolean {
        val notFound = response.not_found ?: return false
        return !notFound.movies.isNullOrEmpty()
                || !notFound.shows.isNullOrEmpty()
                || !notFound.episodes.isNullOrEmpty()
    }

    /**
     * Makes the call and returns [TraktResponse.Success] with the body if successful or one of
     * [TraktErrorResponse] otherwise.
     *
     * If there is an error, logs and reports it. Except for [TraktErrorResponse.IsNotVip] and
     * [TraktErrorResponse.IsUnauthorized].
     *
     * Use [reportIsNotVip] to report this error if it is unexpected.
     */
    private suspend fun <T> awaitTraktCall(
        trakt: TraktV2,
        call: Call<T>,
        action: String,
        reportIsNotVip: Boolean = false,
        logErrorOnNullBody: Boolean = false
    ): TraktResponse<T> {
        val response = try {
            call.awaitResponse()
        } catch (e: Exception) {
            Errors.logAndReport(action, e)
            return TraktErrorResponse.Other()
        }

        if (!response.isSuccessful) {
            // The error body can only be read once, so only parse it when reporting
            fun report() = Errors.logAndReport(
                action,
                response,
                SgTrakt.checkForTraktError(trakt, response)
            )

            return when {
                TraktV2.isAccountLimitExceeded(response) -> {
                    report()
                    TraktErrorResponse.IsAccountLimitExceeded()
                }

                TraktV2.isAccountLocked(response) -> {
                    report()
                    TraktErrorResponse.IsAccountLocked()
                }

                TraktV2.isNotVip(response) -> {
                    if (reportIsNotVip) report()
                    TraktErrorResponse.IsNotVip()
                }

                TraktV2.isUnauthorized(response) -> TraktErrorResponse.IsUnauthorized()
                else -> {
                    report()
                    TraktErrorResponse.Other()
                }
            }
        }

        val body = response.body()

        // Report if there might be a bigger API change
        if (logErrorOnNullBody && body == null) {
            Errors.logAndReport(action, response, "body is null")
        }

        // Only returned when using pagination
        val pageCountOrNull = TraktV2.getPageCount(response)

        return TraktResponse.Success(body, pageCountOrNull)
    }

    /**
     * Like [awaitTraktCall], but ensures the response data is not null.
     */
    private suspend fun <T> awaitTraktCallNonNull(
        trakt: TraktV2,
        call: Call<T>,
        action: String,
        reportIsNotVip: Boolean = false
    ): TraktNonNullResponse<T> {
        return when (val response =
            awaitTraktCall(trakt, call, action, reportIsNotVip, logErrorOnNullBody = true)) {
            is TraktErrorResponse.Other -> response
            is TraktErrorResponse.IsAccountLimitExceeded -> response
            is TraktErrorResponse.IsAccountLocked -> response
            is TraktErrorResponse.IsNotVip -> response
            is TraktErrorResponse.IsUnauthorized -> response
            is TraktResponse.Success -> {
                val data = response.data
                if (data == null) {
                    TraktErrorResponse.Other()
                } else {
                    TraktNonNullResponse.Success(data, response.pageCount)
                }
            }
        }
    }

    private fun <T, R> mapResponseData(
        response: TraktNonNullResponse<T>,
        transform: (T) -> R
    ): TraktNonNullResponse<R> {
        return when (response) {
            is TraktNonNullResponse.Success -> TraktNonNullResponse.Success(
                transform(response.data),
                response.pageCount
            )

            is TraktErrorResponse.IsNotVip -> TraktErrorResponse.IsNotVip()
            is TraktErrorResponse.IsUnauthorized -> TraktErrorResponse.IsUnauthorized()
            is TraktErrorResponse.IsAccountLimitExceeded -> TraktErrorResponse.IsAccountLimitExceeded()
            is TraktErrorResponse.IsAccountLocked -> TraktErrorResponse.IsAccountLocked()
            is TraktErrorResponse.Other -> TraktErrorResponse.Other()
        }
    }

}