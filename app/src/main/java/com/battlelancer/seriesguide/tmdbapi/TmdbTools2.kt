// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2025 Uwe Trottmann

package com.battlelancer.seriesguide.tmdbapi

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.movies.MoviesSettings
import com.battlelancer.seriesguide.people.Credits
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.util.Errors
import com.github.michaelbull.result.get
import com.uwetrottmann.tmdb2.DiscoverTvBuilder
import com.uwetrottmann.tmdb2.Tmdb
import com.uwetrottmann.tmdb2.entities.Collection
import com.uwetrottmann.tmdb2.entities.DiscoverFilter
import com.uwetrottmann.tmdb2.entities.DiscoverFilter.Separator.OR
import com.uwetrottmann.tmdb2.entities.Person
import com.uwetrottmann.tmdb2.entities.TmdbDate
import com.uwetrottmann.tmdb2.entities.TvShow
import com.uwetrottmann.tmdb2.entities.TvShowResultsPage
import com.uwetrottmann.tmdb2.entities.Videos
import com.uwetrottmann.tmdb2.entities.WatchProviders
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
                    TmdbTools3.findShowTmdbId(context, showIds.tvdbId).get()
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

    data class WatchInfo(
        val url: String?,
        val topProvider: String?,
        val countMore: Int,
        val subscription: List<String>,
        val free: List<String>,
        val withAds: List<String>,
        val buy: List<String>,
        val rent: List<String>
    )

    fun buildWatchInfo(providers: WatchProviders.CountryInfo?): WatchInfo {
        if (providers == null) {
            return WatchInfo(
                url = null,
                topProvider = null,
                countMore = 0,
                subscription = emptyList(),
                free = emptyList(),
                withAds = emptyList(),
                buy = emptyList(),
                rent = emptyList()
            )
        }
        // Technically display_priority can be null, so default to largest value as lowest value is
        // highest priority.
        val topProvider = providers.flatrate.minByOrNull { it.display_priority ?: Int.MAX_VALUE }
            ?: providers.free.minByOrNull { it.display_priority ?: Int.MAX_VALUE }
            ?: providers.ads.minByOrNull { it.display_priority ?: Int.MAX_VALUE }
            ?: providers.buy.minByOrNull { it.display_priority ?: Int.MAX_VALUE }
            ?: providers.rent.minByOrNull { it.display_priority ?: Int.MAX_VALUE }
        val count = providers.flatrate.size +
                providers.free.size +
                providers.ads.size +
                providers.buy.size +
                providers.rent.size
        // For season-level watch provider info TMDB returns a link to a season-specific website
        // that doesn't exist. So remove the /season/<number> part of the URL to end up with the
        // existing show-level page. For example:
        // https://www.themoviedb.org/tv/10283-archer/season/14/watch?locale=DE ->
        // https://www.themoviedb.org/tv/10283-archer/watch?locale=DE
        return WatchInfo(
            url = providers.link?.replace(WATCH_PROVIDER_SEASON_PATH_REGEX, ""),
            topProvider = topProvider?.provider_name,
            countMore = (count - 1).coerceAtLeast(0),
            subscription = providers.flatrate.toSortedNames(),
            free = providers.free.toSortedNames(),
            withAds = providers.ads.toSortedNames(),
            buy = providers.buy.toSortedNames(),
            rent = providers.rent.toSortedNames()
        )
    }

    private fun List<WatchProviders.WatchProvider>.toSortedNames(): List<String> =
        mapNotNull { it.provider_name }.sortedBy { it.lowercase() }

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

    suspend fun getWatchProvidersForSeason(
        tmdb: Tmdb,
        showTmdbId: Int,
        seasonNumber: Int,
        region: String
    ): WatchInfo {
        return tmdb
            .tvSeasonsService()
            .watchProviders(showTmdbId, seasonNumber)
            .awaitResponse("providers season")
            ?.results?.get(region)
            .let { buildWatchInfo(it) }
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
        val WATCH_PROVIDER_SEASON_PATH_REGEX = "/season/[0-9]+".toRegex()
        // In UI, crew is currently not grouped by department, but for easier scanning sort by it,
        // then job (description).
        private val byDepartmentJobAndName: Comparator<SgPerson> =
            compareBy({ it.department }, { it.description }, { it.name })

        fun extractTrailer(videos: Videos?): String? {
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
    }
}
