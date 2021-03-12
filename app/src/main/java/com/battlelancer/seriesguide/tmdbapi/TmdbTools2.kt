package com.battlelancer.seriesguide.tmdbapi

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.ui.shows.ShowTools2.ShowResult
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.tmdb2.entities.AppendToResponse
import com.uwetrottmann.tmdb2.entities.BaseTvShow
import com.uwetrottmann.tmdb2.entities.Credits
import com.uwetrottmann.tmdb2.entities.TmdbDate
import com.uwetrottmann.tmdb2.entities.TvSeason
import com.uwetrottmann.tmdb2.entities.TvShow
import com.uwetrottmann.tmdb2.entities.WatchProviders
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem
import com.uwetrottmann.tmdb2.enumerations.ExternalSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.util.Calendar
import java.util.Date

class TmdbTools2 {

    /**
     * Tries to find the TMDB id for the given show's TheTVDB id. Returns -1 if not found,
     * returns null on error or failure.
     */
    fun findShowTmdbId(context: Context, showTvdbId: Int): Int? {
        val tmdb = SgApp.getServicesComponent(context.applicationContext).tmdb()
        try {
            val response = tmdb.findService()
                .find(showTvdbId, ExternalSource.TVDB_ID, null)
                .execute()
            if (response.isSuccessful) {
                val tvResults = response.body()?.tv_results
                if (!tvResults.isNullOrEmpty()) {
                    val showId = tvResults[0].id
                    showId?.let {
                        return it // found it!
                    }
                }
                return -1 // not found
            } else {
                Errors.logAndReport("find tvdb show", response)
            }
        } catch (e: Exception) {
            Errors.logAndReport("find tvdb show", e)
        }

        return null
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
     * Returns true if the show is null because it no longer exists (TMDB returned HTTP 404).
     */
    fun getShowAndExternalIds(
        showTmdbId: Int,
        language: String,
        context: Context
    ): Pair<TvShow?, ShowResult> {
        val tmdb = SgApp.getServicesComponent(context.applicationContext).tmdb()
        try {
            val response = tmdb.tvService()
                .tv(showTmdbId, language, AppendToResponse(AppendToResponseItem.EXTERNAL_IDS))
                .execute()
            if (response.isSuccessful) {
                val results = response.body()
                if (results != null) return Pair(results, ShowResult.SUCCESS)
            } else {
                // Explicitly indicate if result is null because show no longer exists.
                if (response.code() == 404) {
                    return Pair(null, ShowResult.DOES_NOT_EXIST)
                }
                Errors.logAndReport("show n ids", response)
            }
        } catch (e: Exception) {
            Errors.logAndReport("show n ids", e)
            if (e is SocketTimeoutException) return Pair(null, ShowResult.TIMEOUT_ERROR)
        }
        return Pair(null, ShowResult.TMDB_ERROR)
    }

    /**
     * Returns null if network call fails.
     */
    fun searchShows(query: String, language: String, context: Context): List<BaseTvShow>? {
        val tmdb = SgApp.getServicesComponent(context.applicationContext).tmdb()
        try {
            val response = tmdb.searchService()
                .tv(query, null, language, null)
                .execute()
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
    fun getShowsWithNewEpisodes(language: String, context: Context): List<BaseTvShow>? {
        val tmdb = SgApp.getServicesComponent(context.applicationContext).tmdb()
        try {
            val response = tmdb.discoverTv()
                .air_date_lte(dateNow)
                .air_date_gte(dateOneWeekAgo)
                .language(language)
                .build()
                .execute()
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

    suspend fun loadCreditsForShow(context: Context, showRowId: Long): Credits? =
        withContext(Dispatchers.IO) {
            // Get TMDB id from database, or look up via legacy TVDB id.
            val showIds = SgRoomDatabase.getInstance(context).sgShow2Helper().getShowIds(showRowId)
                ?: return@withContext null
            val tmdbId = showIds.tmdbId
                ?: if (showIds.tvdbId != null) {
                    findShowTmdbId(context, showIds.tvdbId)
                } else {
                    null
                }
            if (tmdbId == null || tmdbId < 0) return@withContext null

            // get credits for that show
            try {
                val response = SgApp.getServicesComponent(context).tmdb().tvService()
                    .credits(tmdbId, null)
                    .execute()
                if (response.isSuccessful) {
                    return@withContext response.body()
                } else {
                    Errors.logAndReport("get show credits", response)
                }
            } catch (e: Exception) {
                Errors.logAndReport("get show credits", e)
            }
            return@withContext null
        }

    fun getSeason(
        showTmdbId: Int,
        seasonNumber: Int,
        language: String,
        context: Context
    ): TvSeason? {
        val tmdb = SgApp.getServicesComponent(context).tmdb()
        try {
            val response = tmdb.tvSeasonsService()
                .season(showTmdbId, seasonNumber, language)
                .execute()
            if (response.isSuccessful) {
                return response.body()
            } else {
                Errors.logAndReport("get shows w new episodes", response)
            }
        } catch (e: Exception) {
            Errors.logAndReport("get shows w new episodes", e)
        }
        return null
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

    fun getWatchProvidersForShow(
        showTmdbId: Int,
        region: String,
        context: Context
    ): WatchProviders.CountryInfo? {
        try {
            val response = SgApp.getServicesComponent(context).tmdb().tvService()
                .watchProviders(showTmdbId)
                .execute()
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

    fun getWatchProvidersForMovie(
        movieTmdbId: Int,
        region: String,
        context: Context
    ): WatchProviders.CountryInfo? {
        try {
            val response = SgApp.getServicesComponent(context).tmdb().moviesService()
                .watchProviders(movieTmdbId)
                .execute()
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
}