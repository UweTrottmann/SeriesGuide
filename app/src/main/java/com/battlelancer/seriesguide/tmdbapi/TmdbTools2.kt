// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2024 Uwe Trottmann

package com.battlelancer.seriesguide.tmdbapi

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.movies.MoviesSettings
import com.battlelancer.seriesguide.people.Credits
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.util.Errors
import com.battlelancer.seriesguide.util.isRetryError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.get
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.uwetrottmann.tmdb2.DiscoverTvBuilder
import com.uwetrottmann.tmdb2.Tmdb
import com.uwetrottmann.tmdb2.entities.AppendToResponse
import com.uwetrottmann.tmdb2.entities.Collection
import com.uwetrottmann.tmdb2.entities.DiscoverFilter
import com.uwetrottmann.tmdb2.entities.DiscoverFilter.Separator.OR
import com.uwetrottmann.tmdb2.entities.Person
import com.uwetrottmann.tmdb2.entities.TmdbDate
import com.uwetrottmann.tmdb2.entities.TvEpisode
import com.uwetrottmann.tmdb2.entities.TvShow
import com.uwetrottmann.tmdb2.entities.TvShowResultsPage
import com.uwetrottmann.tmdb2.entities.Videos
import com.uwetrottmann.tmdb2.entities.WatchProviders
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem
import com.uwetrottmann.tmdb2.enumerations.ExternalSource
import com.uwetrottmann.tmdb2.enumerations.SortBy
import com.uwetrottmann.tmdb2.enumerations.VideoType
import com.uwetrottmann.tmdb2.services.PeopleService
import com.uwetrottmann.tmdb2.services.TvEpisodesService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.awaitResponse
import retrofit2.create
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import com.battlelancer.seriesguide.people.Person as SgPerson

class TmdbTools2 {

    /**
     * Tries to find the TMDB id for the given show's TheTVDB id. Returns null value if not found.
     */
    fun findShowTmdbId(context: Context, showTvdbId: Int): Result<Int?, TmdbError> {
        val action = "find tvdb show"
        val tmdb = SgApp.getServicesComponent(context.applicationContext).tmdb()
        return runCatching {
            tmdb.findService()
                .find(showTvdbId, ExternalSource.TVDB_ID, null)
                .execute()
        }.mapError {
            Errors.logAndReport(action, it)
            if (it.isRetryError()) TmdbRetry else TmdbStop
        }.andThen {
            if (it.isSuccessful) {
                val tvResults = it.body()?.tv_results
                if (tvResults != null) {
                    if (tvResults.isNotEmpty()) {
                        val showId = tvResults[0].id
                        if (showId != null && showId > 0) {
                            return@andThen Ok(showId) // found it!
                        } else {
                            Errors.logAndReport(action, it, "show id is invalid")
                        }
                    } else {
                        return@andThen Ok(null) // not found
                    }
                } else {
                    Errors.logAndReport(action, it, "tv_results is null")
                }
            } else {
                Errors.logAndReport(action, it)
            }
            return@andThen Err(TmdbStop)
        }
    }

    fun getShowDetails(showTmdbId: Int, language: String, context: Context): TvShow? {
        val tmdb = SgApp.getServicesComponent(context.applicationContext).tmdb()
        try {
            val response = tmdb.tvService()
                .tv(showTmdbId, language)
                .execute()
            if (response.isSuccessful) {
                val results = response.body()
                if (results != null) return results
            } else {
                Errors.logAndReport("show details", response)
            }
        } catch (e: Exception) {
            Errors.logAndReport("show details", e)
        }
        return null
    }

    /**
     * Returns null value if the show no longer exists (TMDB returned HTTP 404).
     */
    fun getShowAndExternalIds(
        showTmdbId: Int,
        language: String,
        context: Context
    ): Result<TvShow?, TmdbError> {
        val action = "show n ids"
        val tmdb = SgApp.getServicesComponent(context.applicationContext).tmdb()
        return runCatching {
            tmdb.tvService()
                .tv(showTmdbId, language, AppendToResponse(AppendToResponseItem.EXTERNAL_IDS))
                .execute()
        }.mapError {
            Errors.logAndReport(action, it)
            if (it.isRetryError()) TmdbRetry else TmdbStop
        }.andThen {
            if (it.isSuccessful) {
                val tvShow = it.body()
                if (tvShow != null) {
                    return@andThen Ok(tvShow)
                } else {
                    Errors.logAndReport(action, it, "show is null")
                }
            } else {
                // Explicitly indicate if result is null because show no longer exists.
                if (it.code() == 404) return@andThen Ok(null)
                Errors.logAndReport(action, it)
            }
            return@andThen Err(TmdbStop)
        }
    }

    /**
     * Returns null if network call fails.
     */
    private suspend fun <T> Call<T>.awaitResponse(action: String): T? {
        try {
            val response = awaitResponse()
            if (response.isSuccessful) {
                return response.body()
            } else {
                Errors.logAndReport(action, response)
            }
        } catch (e: Exception) {
            Errors.logAndReport(action, e)
        }
        return null
    }

    suspend fun searchShows(
        tmdb: Tmdb,
        query: String,
        language: String,
        firstReleaseYear: Int?,
        page: Int
    ): TvShowResultsPage? {
        return tmdb.searchService()
            .tv(query, page, language, firstReleaseYear, false)
            .awaitResponse("search shows")
    }

    private fun discoverTvBuilder(
        tmdb: Tmdb,
        language: String,
        page: Int,
        firstReleaseYear: Int?,
        originalLanguage: String?,
        watchProviderIds: List<Int>?,
        watchRegion: String?
    ): DiscoverTvBuilder {
        val builder = tmdb.discoverTv()
            .language(language)
            .page(page)
        if (firstReleaseYear != null) {
            builder.first_air_date_year(firstReleaseYear)
        }
        if (originalLanguage != null) {
            builder.with_original_language(originalLanguage)
        }
        if (!watchProviderIds.isNullOrEmpty() && watchRegion != null) {
            builder
                .with_watch_providers(DiscoverFilter(OR, *watchProviderIds.toTypedArray()))
                .watch_region(watchRegion)
        }
        return builder
    }

    private val dateNow: TmdbDate
        get() = TmdbDate(Date())

    private val dateOneWeekAgo: TmdbDate
        get() {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, -7)
            return TmdbDate(calendar.time)
        }

    /**
     * Returns null if network call fails.
     */
    suspend fun getShowsWithNewEpisodes(
        tmdb: Tmdb,
        language: String,
        page: Int,
        firstReleaseYear: Int?,
        originalLanguage: String?,
        watchProviderIds: List<Int>?,
        watchRegion: String?
    ): TvShowResultsPage? {
        val builder = discoverTvBuilder(
            tmdb,
            language,
            page,
            firstReleaseYear,
            originalLanguage,
            watchProviderIds,
            watchRegion
        )
            .air_date_lte(dateNow)
            .air_date_gte(dateOneWeekAgo)
        return builder.build()
            .awaitResponse("get shows w new episodes")
    }

    suspend fun getPopularShows(
        tmdb: Tmdb,
        language: String,
        page: Int,
        firstReleaseYear: Int?,
        originalLanguage: String?,
        watchProviderIds: List<Int>?,
        watchRegion: String?
    ): TvShowResultsPage? {
        val builder = discoverTvBuilder(
            tmdb,
            language,
            page,
            firstReleaseYear,
            originalLanguage,
            watchProviderIds,
            watchRegion
        )
            .sort_by(SortBy.POPULARITY_DESC)
        if (firstReleaseYear != null) {
            builder.first_air_date_year(firstReleaseYear)
        }
        if (originalLanguage != null) {
            builder.with_original_language(originalLanguage)
        }
        if (!watchProviderIds.isNullOrEmpty() && watchRegion != null) {
            builder
                .with_watch_providers(DiscoverFilter(OR, *watchProviderIds.toTypedArray()))
                .watch_region(watchRegion)
        }
        return builder.build()
            .awaitResponse("load popular shows")
    }

    fun getShowTrailerYoutubeId(
        context: Context,
        showTmdbId: Int,
        languageCode: String
    ): Result<String?, TmdbError> {
        val action = "get show trailer"
        val tmdb = SgApp.getServicesComponent(context.applicationContext).tmdb()
        return runCatching {
            tmdb.tvService()
                .videos(showTmdbId, languageCode)
                .execute()
        }.mapError {
            Errors.logAndReport(action, it)
            if (it.isRetryError()) TmdbRetry else TmdbStop
        }.andThen {
            if (it.isSuccessful) {
                val results = it.body()?.results
                if (results != null) {
                    return@andThen Ok(extractTrailer(it.body()))
                } else {
                    Errors.logAndReport(action, it, "results is null")
                }
            } else {
                Errors.logAndReport(action, it)
            }
            return@andThen Err(TmdbStop)
        }
    }

    /**
     * Loads a YouTube movie trailer from TMDb. Tries to get a local trailer, if not falls back to
     * English.
     */
    fun getMovieTrailerYoutubeId(
        context: Context,
        movieTmdbId: Int
    ): String? {
        // try to get a local trailer
        val trailer = getMovieTrailerYoutubeId(
            context,
            movieTmdbId,
            MoviesSettings.getMoviesLanguage(context),
            "get local movie trailer"
        )
        if (trailer != null) {
            return trailer
        }
        Timber.d("Did not find a local movie trailer.")

        // fall back to default language trailer
        return getMovieTrailerYoutubeId(
            context, movieTmdbId, null, "get default movie trailer"
        )
    }

    private fun getMovieTrailerYoutubeId(
        context: Context,
        movieTmdbId: Int,
        languageCode: String?,
        action: String
    ): String? {
        val moviesService = SgApp.getServicesComponent(context).moviesService()
        try {
            val response = moviesService.videos(movieTmdbId, languageCode).execute()
            if (response.isSuccessful) {
                return extractTrailer(response.body())
            } else {
                Errors.logAndReport(action, response)
            }
        } catch (e: Exception) {
            Errors.logAndReport(action, e)
        }
        return null
    }

    private fun extractTrailer(videos: Videos?): String? {
        val results = videos?.results
        if (results == null || results.size == 0) {
            return null
        }

        // Pick the first YouTube trailer
        for (video in results) {
            val videoId = video.key
            if (video.type == VideoType.TRAILER && "YouTube" == video.site
                && !videoId.isNullOrEmpty()) {
                return videoId
            }
        }
        return null
    }

    suspend fun getShowWatchProviders(
        tmdb: Tmdb,
        language: String,
        watchRegion: String
    ): List<WatchProviders.WatchProvider>? {
        return try {
            (tmdb as SgTmdb).retrofit.create<WatchProvidersService>()
                .tv(language, watchRegion)
                .results
        } catch (e: Exception) {
            Errors.logAndReport("get show watch providers", e)
            null
        }
    }

    suspend fun getMovieWatchProviders(
        tmdb: Tmdb,
        language: String,
        watchRegion: String
    ): List<WatchProviders.WatchProvider>? {
        return try {
            (tmdb as SgTmdb).retrofit.create<WatchProvidersService>()
                .movie(language, watchRegion)
                .results
        } catch (e: Exception) {
            Errors.logAndReport("get movie watch providers", e)
            null
        }
    }

    suspend fun loadCreditsForShow(context: Context, showRowId: Long): Credits? =
        withContext(Dispatchers.IO) {
            // Get TMDB id from database, or look up via legacy TVDB id.
            val showIds = SgRoomDatabase.getInstance(context).sgShow2Helper().getShowIds(showRowId)
                ?: return@withContext null
            val tmdbId = showIds.tmdbId
                ?: if (showIds.tvdbId != null) {
                    findShowTmdbId(context, showIds.tvdbId).get()
                } else {
                    null
                }
            if (tmdbId == null || tmdbId < 0) {
                return@withContext null
            }
            return@withContext getCreditsForShow(context, tmdbId)
        }

    suspend fun getCreditsForShow(context: Context, tmdbId: Int): Credits? {
        // Note: sending a language does not seem to make a difference (e.g. roles are still in English)
        return SgApp.getServicesComponent(context).tmdb()
            .tvService()
            .aggregateCredits(tmdbId, null)
            .awaitResponse("get show credits")
            ?.let { credits ->
                val crew = credits.crew?.mapNotNull { credit ->
                    val id = credit.id ?: return@mapNotNull null
                    val name = credit.name ?: return@mapNotNull null
                    SgPerson(
                        tmdbId = id,
                        name = name,
                        profilePath = credit.profile_path,
                        description = credit.jobs
                            ?.mapNotNull { it.job }
                            ?.joinToString { it },
                        department = credit.department
                    )
                }?.sortedWith(byDepartmentJobAndName) ?: emptyList()

                // After sorting, move writers first
                val (writers, otherCrew) = crew.partition { it.department == "Writing" }

                // Combining all characters/jobs for the description
                Credits(
                    tmdbId = tmdbId,
                    cast = credits.cast?.mapNotNull { credit ->
                        val id = credit.id ?: return@mapNotNull null
                        val name = credit.name ?: return@mapNotNull null
                        SgPerson(
                            tmdbId = id,
                            name = name,
                            profilePath = credit.profile_path,
                            description = credit.roles
                                ?.mapNotNull { it.character }
                                ?.joinToString { it }
                        )
                    } ?: emptyList(),
                    crew = writers + otherCrew
                )
            }
    }

    suspend fun getCreditsForMovie(context: Context, tmdbId: Int): Credits? {
        return SgApp.getServicesComponent(context)
            .moviesService()
            .credits(tmdbId)
            .awaitResponse("get movie credits")
            ?.let { credits ->
                val crew = credits.crew?.mapNotNull { credit ->
                    val id = credit.id ?: return@mapNotNull null
                    val name = credit.name ?: return@mapNotNull null
                    SgPerson(
                        tmdbId = id,
                        name = name,
                        profilePath = credit.profile_path,
                        description = credit.job,
                        department = credit.department
                    )
                }?.sortedWith(byDepartmentJobAndName) ?: emptyList()

                // After sorting, move directors and writers first.
                // As the list is already ordered by department then job, just pick in order
                // if either job as director or department of writing.
                val (directorsAndWriters, otherCrew) = crew.partition {
                    it.description == "Director" || it.department == "Writing"
                }

                Credits(
                    tmdbId = tmdbId,
                    cast = credits.cast?.mapNotNull { credit ->
                        val id = credit.id ?: return@mapNotNull null
                        val name = credit.name ?: return@mapNotNull null
                        SgPerson(
                            tmdbId = id,
                            name = name,
                            profilePath = credit.profile_path,
                            description = credit.character
                        )
                    } ?: emptyList(),
                    crew = directorsAndWriters + otherCrew
                )
            }
    }

    suspend fun getPerson(
        peopleService: PeopleService,
        tmdbId: Int,
        language: String
    ): Person? {
        return peopleService
            .summary(tmdbId, language)
            .awaitResponse("get person summary")
    }

    fun getSeason(
        showTmdbId: Int,
        seasonNumber: Int,
        language: String,
        context: Context
    ): Result<List<TvEpisode>, TmdbError> {
        val action = "get season"
        val tmdb = SgApp.getServicesComponent(context).tmdb()
        return runCatching {
            tmdb.tvSeasonsService()
                .season(showTmdbId, seasonNumber, language)
                .execute()
        }.mapError {
            Errors.logAndReport(action, it)
            if (it.isRetryError()) TmdbRetry else TmdbStop
        }.andThen {
            if (it.isSuccessful) {
                val tvSeason = it.body()?.episodes
                if (tvSeason != null) {
                    return@andThen Ok(tvSeason)
                } else {
                    Errors.logAndReport(action, it, "episodes is null")
                }
            } else {
                Errors.logAndReport(action, it)
            }
            return@andThen Err(TmdbStop)
        }
    }

    data class WatchInfo(
        val url: String?,
        val provider: WatchProviders.WatchProvider?,
        val countMore: Int
    )

    fun getTopWatchProvider(providers: WatchProviders.CountryInfo?): WatchInfo {
        if (providers == null) return WatchInfo(null, null, 0)
        val topProvider = providers.flatrate.minByOrNull { it.display_priority }
            ?: providers.free.minByOrNull { it.display_priority }
            ?: providers.ads.minByOrNull { it.display_priority }
            ?: providers.buy.minByOrNull { it.display_priority }
        val count = providers.flatrate.size +
                providers.free.size +
                providers.ads.size +
                providers.buy.size
        return WatchInfo(providers.link, topProvider, (count - 1).coerceAtLeast(0))
    }

    suspend fun getWatchProvidersForShow(
        showTmdbId: Int,
        region: String,
        context: Context
    ): WatchProviders.CountryInfo? {
        return SgApp.getServicesComponent(context).tmdb()
            .tvService()
            .watchProviders(showTmdbId)
            .awaitResponse("providers show")
            ?.results?.get(region)
    }

    suspend fun getWatchProvidersForMovie(
        movieTmdbId: Int,
        region: String,
        context: Context
    ): WatchProviders.CountryInfo? {
        return SgApp.getServicesComponent(context).tmdb()
            .moviesService()
            .watchProviders(movieTmdbId)
            .awaitResponse("providers movie")
            ?.results?.get(region)
    }

    suspend fun getImdbIdForEpisode(
        tvEpisodesService: TvEpisodesService,
        showTmdbId: Int,
        seasonNumber: Int,
        episodeNumber: Int
    ): String? {
        return tvEpisodesService
            .externalIds(showTmdbId, seasonNumber, episodeNumber)
            .awaitResponse("episode imdb id")
            ?.imdb_id
    }

    suspend fun getMovieCollection(
        tmdb: Tmdb,
        collectionId: Int,
        languageCode: String?
    ): Collection? {
        return tmdb.collectionService()
            .summary(collectionId, languageCode)
            .awaitResponse("movie collection")
    }

    companion object {
        // In UI, crew is currently not grouped by department, but for easier scanning sort by it,
        // then job (description).
        private val byDepartmentJobAndName: Comparator<SgPerson> =
            compareBy({ it.department }, { it.description }, { it.name })
    }
}

sealed class TmdbError

/**
 * The API request might succeed if tried again after a brief delay
 * (e.g. time outs or other temporary network issues).
 */
object TmdbRetry : TmdbError()

/**
 * The API request is unlikely to succeed if retried, at least right now
 * (e.g. API bugs or changes).
 */
object TmdbStop : TmdbError()
