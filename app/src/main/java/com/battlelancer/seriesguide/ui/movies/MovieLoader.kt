package com.battlelancer.seriesguide.ui.movies

import android.content.Context
import com.battlelancer.seriesguide.SgApp.Companion.getServicesComponent
import com.battlelancer.seriesguide.provider.SgRoomDatabase.Companion.getInstance
import com.uwetrottmann.androidutils.GenericSimpleLoader
import com.uwetrottmann.tmdb2.entities.Movie
import com.uwetrottmann.trakt5.entities.Ratings

/**
 * Tries to load current movie details from trakt and TMDb, if failing tries to fall back to local
 * database copy.
 */
internal class MovieLoader(
    context: Context,
    private val tmdbId: Int
) : GenericSimpleLoader<MovieDetails>(context) {

    override fun loadInBackground(): MovieDetails {
        // try loading from trakt and tmdb, this might return a cached response
        val movieTools = getServicesComponent(context).movieTools()
        val details = movieTools.getMovieDetails(tmdbId, true)

        // Update local database (no-op if movie not in database).
        movieTools.updateMovie(details, tmdbId)

        // Fill in details from local database.
        val dbMovieOrNull = getInstance(context)
            .movieHelper()
            .getMovie(tmdbId)
        if (dbMovieOrNull == null) {
            // ensure list flags and watched flag are false on failure
            // (assumption: movie not in db, it has the truth, so can't be in any lists or watched)
            details.isInCollection = false
            details.isInWatchlist = false
            details.isWatched = false
            details.plays = 0
            return details
        }

        // set local state for watched, collected and watchlist status
        // assumption: local db has the truth for these
        details.isInCollection = dbMovieOrNull.inCollection
        details.isInWatchlist = dbMovieOrNull.inWatchlist
        details.isWatched = dbMovieOrNull.watched
        details.plays = dbMovieOrNull.plays
        // also use local state of user rating
        details.userRating = dbMovieOrNull.ratingUser ?: 0

        // only overwrite other info if remote data failed to load
        if (details.traktRatings() == null) {
            val traktRatings = Ratings()
            traktRatings.rating = dbMovieOrNull.ratingTrakt?.toDouble() ?: 0.0
            traktRatings.votes = dbMovieOrNull.ratingVotesTrakt ?: 0
            details.traktRatings(traktRatings)
        }
        if (details.tmdbMovie() == null) {
            val tmdbMovie = Movie()
            tmdbMovie.imdb_id = dbMovieOrNull.imdbId
            tmdbMovie.title = dbMovieOrNull.title
            tmdbMovie.overview = dbMovieOrNull.overview
            tmdbMovie.poster_path = dbMovieOrNull.poster
            tmdbMovie.runtime = dbMovieOrNull.runtimeMinOrDefault
            tmdbMovie.vote_average = dbMovieOrNull.ratingTmdb ?: 0.0
            tmdbMovie.vote_count = dbMovieOrNull.ratingVotesTmdb ?: 0
            // if stored release date is Long.MAX, movie has no release date
            val releaseDateMs = dbMovieOrNull.releasedMsOrDefault
            tmdbMovie.release_date = MovieTools.movieReleaseDateFrom(releaseDateMs)
            details.tmdbMovie(tmdbMovie)
        }

        return details
    }
}