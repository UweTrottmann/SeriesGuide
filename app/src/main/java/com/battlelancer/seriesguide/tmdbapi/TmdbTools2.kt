package com.battlelancer.seriesguide.tmdbapi

import android.content.Context
import com.battlelancer.seriesguide.SgApp
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
import com.uwetrottmann.tmdb2.Tmdb
import com.uwetrottmann.tmdb2.entities.AppendToResponse
import com.uwetrottmann.tmdb2.entities.BaseTvShow
import com.uwetrottmann.tmdb2.entities.Credits
import com.uwetrottmann.tmdb2.entities.DiscoverFilter
import com.uwetrottmann.tmdb2.entities.DiscoverFilter.Separator.OR
import com.uwetrottmann.tmdb2.entities.Person
import com.uwetrottmann.tmdb2.entities.TmdbDate
import com.uwetrottmann.tmdb2.entities.TvEpisode
import com.uwetrottmann.tmdb2.entities.TvShow
import com.uwetrottmann.tmdb2.entities.TvShowResultsPage
import com.uwetrottmann.tmdb2.entities.WatchProviders
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem
import com.uwetrottmann.tmdb2.enumerations.ExternalSource
import com.uwetrottmann.tmdb2.enumerations.SortBy
import com.uwetrottmann.tmdb2.services.PeopleService
import com.uwetrottmann.tmdb2.services.TvEpisodesService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse
import retrofit2.create
import java.util.Calendar
import java.util.Date

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
    suspend fun searchShows(query: String, language: String, context: Context): List<BaseTvShow>? {
        val tmdb = SgApp.getServicesComponent(context.applicationContext).tmdb()
        try {
            val response = tmdb.searchService()
                .tv(query, null, language, null, false)
                .awaitResponse()
            if (response.isSuccessful) {
                val results = response.body()?.results
                if (results != null) return results
            } else {
                Errors.logAndReport("search shows", response)
            }
        } catch (e: Exception) {
            Errors.logAndReport("search shows", e)
        }
        return null
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
        watchProviderIds: List<Int>?,
        watchRegion: String?
    ): List<BaseTvShow>? {
        val builder = tmdb.discoverTv()
            .air_date_lte(dateNow)
            .air_date_gte(dateOneWeekAgo)
            .language(language)
        if (!watchProviderIds.isNullOrEmpty() && watchRegion != null) {
            builder
                .with_watch_providers(DiscoverFilter(OR, *watchProviderIds.toTypedArray()))
                .watch_region(watchRegion)
        }

        try {
            val response = builder.build().awaitResponse()
            if (response.isSuccessful) {
                val results = response.body()?.results
                if (results != null) return results
            } else {
                Errors.logAndReport("get shows w new episodes", response)
            }
        } catch (e: Exception) {
            Errors.logAndReport("get shows w new episodes", e)
        }
        return null
    }

    suspend fun getPopularShows(
        tmdb: Tmdb,
        language: String,
        page: Int,
        watchProviderIds: List<Int>?,
        watchRegion: String?
    ): TvShowResultsPage? {
        val builder = tmdb.discoverTv()
            .language(language)
            .sort_by(SortBy.POPULARITY_DESC)
            .page(page)
        if (!watchProviderIds.isNullOrEmpty() && watchRegion != null) {
            builder
                .with_watch_providers(DiscoverFilter(OR, *watchProviderIds.toTypedArray()))
                .watch_region(watchRegion)
        }

        try {
            val response = builder.build().awaitResponse()
            if (response.isSuccessful) {
                return response.body()
            } else {
                Errors.logAndReport("load popular shows", response)
            }
        } catch (e: Exception) {
            Errors.logAndReport("load popular shows", e)
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
        try {
            val response = SgApp.getServicesComponent(context).tmdb().tvService()
                .credits(tmdbId, null)
                .awaitResponse()
            if (response.isSuccessful) {
                return response.body()
            } else {
                Errors.logAndReport("get show credits", response)
            }
        } catch (e: Exception) {
            Errors.logAndReport("get show credits", e)
        }
        return null
    }

    suspend fun getCreditsForMovie(context: Context, tmdbId: Int): Credits? {
        try {
            val response = SgApp.getServicesComponent(context).moviesService()
                .credits(tmdbId)
                .awaitResponse()
            if (response.isSuccessful) {
                return response.body()
            } else {
                Errors.logAndReport("get movie credits", response)
            }
        } catch (e: Exception) {
            Errors.logAndReport("get movie credits", e)
        }
        return null
    }

    suspend fun getPerson(
        peopleService: PeopleService,
        tmdbId: Int,
        language: String
    ): Person? {
        try {
            val response = peopleService.summary(tmdbId, language).awaitResponse()
            if (response.isSuccessful) {
                return response.body()
            } else {
                Errors.logAndReport("get person summary", response)
            }
        } catch (e: Exception) {
            Errors.logAndReport("get person summary", e)
        }
        return null
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
        try {
            val response = SgApp.getServicesComponent(context).tmdb().tvService()
                .watchProviders(showTmdbId)
                .awaitResponse()
            if (response.isSuccessful) {
                return response.body()?.results?.get(region)
            } else {
                Errors.logAndReport("providers show", response)
            }
        } catch (e: Exception) {
            Errors.logAndReport("providers show", e)
        }
        return null
    }

    suspend fun getWatchProvidersForMovie(
        movieTmdbId: Int,
        region: String,
        context: Context
    ): WatchProviders.CountryInfo? {
        try {
            val response = SgApp.getServicesComponent(context).tmdb().moviesService()
                .watchProviders(movieTmdbId)
                .awaitResponse()
            if (response.isSuccessful) {
                return response.body()?.results?.get(region)
            } else {
                Errors.logAndReport("providers movie", response)
            }
        } catch (e: Exception) {
            Errors.logAndReport("providers show", e)
        }
        return null
    }

    suspend fun getImdbIdForEpisode(
        tvEpisodesService: TvEpisodesService,
        showTmdbId: Int,
        seasonNumber: Int,
        episodeNumber: Int
    ): String? {
        try {
            val response = tvEpisodesService
                .externalIds(showTmdbId, seasonNumber, episodeNumber)
                .awaitResponse()
            if (response.isSuccessful) {
                return response.body()?.imdb_id
            } else {
                Errors.logAndReport("providers movie", response)
            }
        } catch (e: Exception) {
            Errors.logAndReport("providers show", e)
        }
        return null
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
