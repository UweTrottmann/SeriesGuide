package com.battlelancer.seriesguide.ui.movies

import com.google.common.truth.Truth.assertThat
import com.uwetrottmann.tmdb2.entities.Movie
import com.uwetrottmann.tmdb2.entities.ReleaseDate
import com.uwetrottmann.tmdb2.entities.ReleaseDatesResult
import com.uwetrottmann.tmdb2.entities.ReleaseDatesResults
import org.junit.Test
import java.util.Date

class MovieToolsTest {

    @Test
    fun updateReleaseDateForRegion() {
        val movieTools = MovieTools2()
        val movie = Movie()
            .apply { release_date = Date(1) }

        movieTools.updateReleaseDateForRegion(movie, null, "DE")
        assertThat(movie.release_date).isEqualTo(Date(1)) // not updated.

        val releaseDates = ReleaseDatesResults().apply {
            results = listOf(
                ReleaseDatesResult().apply {
                    iso_3166_1 = "US"
                    release_dates = listOf(
                        ReleaseDate().apply {
                            type = ReleaseDate.TYPE_THEATRICAL_LIMITED
                            release_date = Date(123456)
                        }
                    )
                },
                ReleaseDatesResult().apply {
                    iso_3166_1 = "DE"
                    release_dates = listOf(
                        ReleaseDate().apply {
                            type = ReleaseDate.TYPE_THEATRICAL
                            release_date = Date(12345)
                        },
                        ReleaseDate().apply {
                            type = ReleaseDate.TYPE_PHYSICAL
                            release_date = Date(1234567)
                        },
                        ReleaseDate().apply {
                            type = ReleaseDate.TYPE_THEATRICAL
                            release_date = Date(1234)
                        }
                    )
                }
            )
        }

        movieTools.updateReleaseDateForRegion(movie, releaseDates, "DE")
        // Picks oldest DE theatrical release date.
        assertThat(movie.release_date).isEqualTo(Date(1234))

        movieTools.updateReleaseDateForRegion(movie, releaseDates, "US")
        // Picks single US date.
        assertThat(movie.release_date).isEqualTo(Date(123456))
    }
}