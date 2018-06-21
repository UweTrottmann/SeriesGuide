package com.battlelancer.seriesguide.sync

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import com.battlelancer.seriesguide.settings.TmdbSettings
import com.battlelancer.seriesguide.tmdbapi.SgTmdb
import com.battlelancer.seriesguide.ui.movies.MovieTools
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.tmdb2.services.ConfigurationService
import java.util.ArrayList

class TmdbSync internal constructor(private val context: Context,
        private val configurationService: ConfigurationService,
        private val movieTools: MovieTools) {

    /**
     * Downloads and stores the latest image url configuration from themoviedb.org.
     */
    fun updateConfiguration(prefs: SharedPreferences): Boolean {
        try {
            val response = configurationService.configuration().execute()
            if (response.isSuccessful) {
                val config = response.body()
                if (config != null && config.images != null
                        && !TextUtils.isEmpty(config.images.secure_base_url)) {
                    prefs.edit()
                            .putString(TmdbSettings.KEY_TMDB_BASE_URL,
                                    config.images.secure_base_url)
                            .apply()
                    return true
                }
            } else {
                SgTmdb.trackFailedRequest(context, "get config", response)
            }
        } catch (e: Exception) {
            SgTmdb.trackFailedRequest(context, "get config", e)
        }

        return false
    }

    /**
     * Regularly updates to be released movies (or those without a release date) with data from
     * themoviedb.org.
     */
    fun updateMovies(): Boolean {
        val tmdbIds = ArrayList<Int>()

        for (tmdbId in tmdbIds) {
            if (!AndroidUtils.isNetworkConnected(context)) {
                return false // stop updates: no network connection
            }

            // try loading details from tmdb
            val details = movieTools.getMovieDetails(tmdbId, false)
            if (details.tmdbMovie() != null) {
                // update local database
                movieTools.updateMovie(details, tmdbId)
            }
        }

        return true // successful
    }
}
