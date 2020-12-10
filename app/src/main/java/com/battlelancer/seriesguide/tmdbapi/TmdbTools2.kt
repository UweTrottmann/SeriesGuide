package com.battlelancer.seriesguide.tmdbapi

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.ui.search.SearchResult
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.tmdb2.entities.BaseTvShow
import com.uwetrottmann.tmdb2.enumerations.ExternalSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
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
     * Resolves the TheTVDB id using a network call on the calling thread!
     * Excludes shows where no TheTVDB id could be resolved (for any reason).
     */
    suspend fun mapTvShowsToSearchResults(
        context: Context,
        languageCode: String,
        results: List<BaseTvShow>
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val tvService = SgApp.getServicesComponent(context.applicationContext)
            .tmdb()
            .tvService()
        return@withContext results.mapNotNull { tvShow ->
            if (!isActive) {
                return@mapNotNull null // do not bother fetching ids for remaining results
            }

            // Find TheTVDB id.
            val idResponse = tvShow.id?.let {
                try {
                    tvService.externalIds(it, null).execute()
                } catch (e: Exception) {
                    null
                }
            }

            // On TMDB the TheTVDB id might be 0, ignore those shows, too.
            val externalIds = idResponse?.body()
            if (idResponse == null || !idResponse.isSuccessful
                || externalIds == null || externalIds.tvdb_id == null
                || externalIds.tvdb_id == 0) {
                null // Ignore this show.
            } else {
                SearchResult().apply {
                    tvdbid = externalIds.tvdb_id!!
                    title = tvShow.name
                    overview = tvShow.overview
                    language = languageCode
                }
            }
        }
    }

}