package com.battlelancer.seriesguide.sync

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import android.text.format.DateUtils
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.settings.TmdbSettings
import com.battlelancer.seriesguide.tmdbapi.SgTmdb
import com.battlelancer.seriesguide.ui.movies.MovieTools
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.tmdb2.services.ConfigurationService
import timber.log.Timber

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
     * Regularly updates current and future movies (or those without a release date) with data from
     * themoviedb.org. All other movies are updated rarely.
     */
    fun updateMovies(): Boolean {
        val currentTimeMillis = System.currentTimeMillis()
        // update movies released 6 months ago or newer, should cover most edits
        val releasedAfter = currentTimeMillis - RELEASED_AFTER_DAYS
        // exclude movies updated in the last 7 days
        val updatedBefore = currentTimeMillis - UPDATED_BEFORE_DAYS
        val updatedBeforeOther = currentTimeMillis - UPDATED_BEFORE_HALF_YEAR
        val movies = SgRoomDatabase.getInstance(context).movieHelper()
                .getMoviesToUpdate(releasedAfter, updatedBefore, updatedBeforeOther)
        Timber.d("Updating %d movie(s)...", movies.size)

        for (movie in movies) {
            if (!AndroidUtils.isNetworkConnected(context)) {
                return false // stop updates: no network connection
            }
            if (movie.tmdbId == 0) {
                continue // skip invalid id
            }

            // try loading details from tmdb
            val details = movieTools.getMovieDetails(movie.tmdbId, false)
            if (details.tmdbMovie() != null) {
                // update local database
                movieTools.updateMovie(details, movie.tmdbId)
            } else {
                Timber.e("Failed to update movie with TMDB id %d", movie.tmdbId)
            }
        }

        return true // successful
    }

    companion object {
        @JvmField
        val RELEASED_AFTER_DAYS = 6 * 30 * DateUtils.DAY_IN_MILLIS
        @JvmField
        val UPDATED_BEFORE_DAYS = 7 * DateUtils.DAY_IN_MILLIS
        @JvmField
        val UPDATED_BEFORE_HALF_YEAR = 6 * 30 * DateUtils.DAY_IN_MILLIS
    }
}
