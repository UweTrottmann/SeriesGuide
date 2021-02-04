package com.battlelancer.seriesguide.tmdbapi

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.ui.search.SearchResult
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.tmdb2.entities.BaseTvShow
import com.uwetrottmann.tmdb2.entities.Credits
import com.uwetrottmann.tmdb2.enumerations.ExternalSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TmdbTools2 {

    /**
     * Tries to find the TMDB id for the given show's TheTVDB id. Returns null on error or failure.
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
            } else {
                Errors.logAndReport("find tvdb show", response)
            }
        } catch (e: Exception) {
            Errors.logAndReport("find tvdb show", e)
        }

        return null
    }

    /**
     * Maps TMDB TV shows to search results.
     */
    suspend fun mapTvShowsToSearchResults(
        languageCode: String,
        results: List<BaseTvShow>
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        return@withContext results.mapNotNull { tvShow ->
            val tmdbId = tvShow.id ?: return@mapNotNull null
            SearchResult().also {
                it.tmdbId = tmdbId
                it.title = tvShow.name
                it.overview = tvShow.overview
                it.language = languageCode
                it.posterPath = tvShow.poster_path
            }
        }
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
                } ?: return@withContext null

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

}