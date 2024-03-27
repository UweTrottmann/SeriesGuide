// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2020, 2022, 2024 Uwe Trottmann

package com.battlelancer.seriesguide.sync

import android.content.Context
import android.text.format.DateUtils
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.movies.MoviesSettings
import com.battlelancer.seriesguide.movies.tools.MovieTools
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.settings.TmdbSettings
import com.battlelancer.seriesguide.streaming.SgWatchProvider
import com.battlelancer.seriesguide.streaming.StreamingSearch
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.tmdb2.services.ConfigurationService
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class TmdbSync internal constructor(
    private val context: Context,
    private val configurationService: ConfigurationService,
    private val movieTools: MovieTools
) {

    @Throws(InterruptedException::class)
    fun updateConfigurationAndWatchProviders(progress: SyncProgress) {
        if (TmdbSettings.isConfigurationUpToDate(context)) {
            return
        }
        // No need to abort on failure, can use default or last fetched config.
        var hadError = false

        // Image URL configuration
        if (!updateConfiguration()) {
            hadError = true
        }

        // Show watch providers
        StreamingSearch.getCurrentRegionOrNull(context)?.also {
            // Note: only updating for shows to keep local watch provider filter up-to-date
            // If this thread is interrupted throws InterruptedException
            val providersUpdated = runBlocking(SgApp.SINGLE) {
                StreamingSearch
                    .updateWatchProviders(context, SgWatchProvider.Type.SHOWS, it)
            }
            if (!providersUpdated) {
                hadError = true
            }
        }

        if (hadError) {
            progress.recordError()
        } else {
            TmdbSettings.setConfigurationLastUpdatedNow(context)
        }
    }

    /**
     * Downloads and stores the latest image url configuration from themoviedb.org.
     */
    private fun updateConfiguration(): Boolean {
        try {
            val response = configurationService.configuration().execute()
            if (response.isSuccessful) {
                val config = response.body()
                val baseUrl = config?.images?.secure_base_url
                if (baseUrl != null) {
                    TmdbSettings.setImageBaseUrl(context, baseUrl)
                    return true
                }
            } else {
                Errors.logAndReport("get config", response)
            }
        } catch (e: Exception) {
            Errors.logAndReport("get config", e)
        }

        return false
    }

    /**
     * Regularly updates current and future movies (or those without a release date) with data from
     * themoviedb.org. All other movies are updated rarely.
     */
    fun updateMovies(progress: SyncProgress): Boolean {
        val currentTimeMillis = System.currentTimeMillis()
        // update movies released 6 months ago or newer, should cover most edits
        val releasedAfter = currentTimeMillis - RELEASED_AFTER_DAYS
        // exclude movies updated in the last 7 days
        val updatedBefore = currentTimeMillis - UPDATED_BEFORE_DAYS
        val updatedBeforeOther = currentTimeMillis - UPDATED_BEFORE_90_DAYS
        val movies = SgRoomDatabase.getInstance(context).movieHelper()
            .getMoviesToUpdate(releasedAfter, updatedBefore, updatedBeforeOther)
        Timber.d("Updating %d movie(s)...", movies.size)

        val languageCode = MoviesSettings.getMoviesLanguage(context)
        val regionCode = MoviesSettings.getMoviesRegion(context)

        var result = true
        for (movie in movies) {
            if (!AndroidUtils.isNetworkConnected(context)) {
                return false // stop updates: no network connection
            }
            if (movie.tmdbId == 0) {
                continue // skip invalid id
            }

            // try loading details from tmdb
            val details = movieTools.getMovieDetails(
                languageCode, regionCode, movie.tmdbId, false
            )
            if (details.tmdbMovie() != null) {
                // update local database
                movieTools.updateMovie(details, movie.tmdbId)
            } else {
                // Treat as failure if updating at least one fails.
                result = false

                val movieTitle = SgRoomDatabase.getInstance(context)
                    .movieHelper()
                    .getMovieTitle(movie.tmdbId)
                val message = "Failed to update movie ('${movieTitle}', TMDB id ${movie.tmdbId})."
                progress.setImportantErrorIfNone(message)
                Timber.e(message)
            }
        }

        return result
    }

    companion object {
        const val RELEASED_AFTER_DAYS = 6 * 30 * DateUtils.DAY_IN_MILLIS
        const val UPDATED_BEFORE_DAYS = 7 * DateUtils.DAY_IN_MILLIS
        const val UPDATED_BEFORE_90_DAYS = 3 * 30 * DateUtils.DAY_IN_MILLIS
    }
}
