package com.battlelancer.seriesguide.jobs

import android.content.Context
import com.battlelancer.seriesguide.backend.HexagonTools
import com.battlelancer.seriesguide.jobs.episodes.JobAction
import com.battlelancer.seriesguide.jobs.episodes.JobAction.MOVIE_COLLECTION_ADD
import com.battlelancer.seriesguide.jobs.episodes.JobAction.MOVIE_COLLECTION_REMOVE
import com.battlelancer.seriesguide.jobs.episodes.JobAction.MOVIE_WATCHLIST_ADD
import com.battlelancer.seriesguide.jobs.episodes.JobAction.MOVIE_WATCHLIST_REMOVE
import com.battlelancer.seriesguide.sync.NetworkJobProcessor
import com.google.api.client.http.HttpResponseException
import com.uwetrottmann.seriesguide.backend.movies.model.Movie
import com.uwetrottmann.seriesguide.backend.movies.model.MovieList
import java.io.IOException
import java.util.ArrayList

class HexagonMovieJob(
    private val hexagonTools: HexagonTools,
    action: JobAction,
    jobInfo: SgJobInfo
) : BaseNetworkMovieJob(action, jobInfo) {

    override fun execute(context: Context): NetworkJobProcessor.JobResult {
        val uploadWrapper = MovieList()
        uploadWrapper.movies = getMovieForHexagon()

        try {
            val moviesService = hexagonTools.moviesService ?: return buildResult(
                context,
                NetworkJob.ERROR_HEXAGON_AUTH
            )
            moviesService.save(uploadWrapper).execute()
        } catch (e: HttpResponseException) {
            HexagonTools.trackFailedRequest("save movie", e)
            val code = e.statusCode
            return if (code in 400..499) {
                buildResult(context, NetworkJob.ERROR_HEXAGON_CLIENT)
            } else {
                buildResult(context, NetworkJob.ERROR_HEXAGON_SERVER)
            }
        } catch (e: IOException) {
            HexagonTools.trackFailedRequest("save movie", e)
            return buildResult(context, NetworkJob.ERROR_CONNECTION)
        }

        return buildResult(context, NetworkJob.SUCCESS)
    }

    private fun getMovieForHexagon(): List<Movie> {
        val movie = Movie()
        movie.tmdbId = jobInfo.movieTmdbId()

        when (action) {
            MOVIE_COLLECTION_ADD -> movie.isInCollection = true
            MOVIE_COLLECTION_REMOVE -> movie.isInCollection = false
            MOVIE_WATCHLIST_ADD -> movie.isInWatchlist = true
            MOVIE_WATCHLIST_REMOVE -> movie.isInWatchlist = false
            else -> throw IllegalArgumentException("Action $action not supported.")
        }

        val movies = ArrayList<Movie>()
        movies.add(movie)
        return movies
    }
}
