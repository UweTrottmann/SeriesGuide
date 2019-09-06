package com.battlelancer.seriesguide.tmdbapi

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.tmdb2.enumerations.ExternalSource

class TmdbTools2(context: Context) {

    private val tmdb = SgApp.getServicesComponent(context.applicationContext).tmdb()

    /**
     * Tries to find the TMDB id for the given show's TheTVDB id. Returns null on error or failure.
     */
    fun findShowTmdbId(showTvdbId: Int): Int? {
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

}