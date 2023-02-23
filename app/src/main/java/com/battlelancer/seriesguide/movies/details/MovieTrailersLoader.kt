package com.battlelancer.seriesguide.movies.details

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.movies.MoviesSettings.getMoviesLanguage
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.GenericSimpleLoader
import com.uwetrottmann.tmdb2.entities.Videos
import com.uwetrottmann.tmdb2.enumerations.VideoType
import timber.log.Timber

/**
 * Loads a YouTube movie trailer from TMDb. Tries to get a local trailer, if not falls back to
 * English.
 */
class MovieTrailersLoader(context: Context, private val tmdbId: Int) :
    GenericSimpleLoader<String?>(context) {

    override fun loadInBackground(): String? {
        // try to get a local trailer
        val trailer = getTrailerVideoId(
            getMoviesLanguage(context), "get local movie trailer"
        )
        if (trailer != null) {
            return trailer
        }
        Timber.d("Did not find a local movie trailer.")

        // fall back to default language trailer
        return getTrailerVideoId(null, "get default movie trailer")
    }

    private fun getTrailerVideoId(language: String?, action: String): String? {
        val moviesService = SgApp.getServicesComponent(context).moviesService()
        try {
            val response = moviesService.videos(tmdbId, language).execute()
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
}