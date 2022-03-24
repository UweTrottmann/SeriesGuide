package com.battlelancer.seriesguide.ui.movies

import com.uwetrottmann.tmdb2.entities.Movie
import com.uwetrottmann.tmdb2.entities.ReleaseDate
import com.uwetrottmann.tmdb2.entities.ReleaseDatesResults

class MovieTools2 {

    /**
     * Replaces the release date of the movie with one of the given region, if available.
     * Picks the theatrical release or if not available the first date for that region.
     * This is not always the best approach, e.g. when viewing disc or digital releases this might
     * not display the correct date. But this is the best possible right now.
     */
    fun updateReleaseDateForRegion(
        movie: Movie,
        results: ReleaseDatesResults?,
        regionCode: String
    ) {
        results?.results?.find {
            it.iso_3166_1 == regionCode
        }?.let { region ->
            val releaseDates = region.release_dates ?: return // No release dates.

            // Only one date? Pick it.
            if (releaseDates.size == 1) {
                releaseDates[0].release_date?.let { date ->
                    movie.release_date = date
                }
                return
            }

            // Pick the oldest theatrical release, if available.
            val theatricalRelease = releaseDates
                .filter { it.type == ReleaseDate.TYPE_THEATRICAL }
                .minOfOrNull { it.release_date }
            if (theatricalRelease != null) {
                movie.release_date = theatricalRelease
            } else {
                // Otherwise just get the first one, if available.
                releaseDates[0]?.release_date?.let { date ->
                    movie.release_date = date
                }
            }
        }
    }

}