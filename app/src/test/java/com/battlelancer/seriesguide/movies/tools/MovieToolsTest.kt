// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2022 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.movies.tools

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.battlelancer.seriesguide.EmptyTestApplication
import com.battlelancer.seriesguide.movies.database.MovieHelper
import com.battlelancer.seriesguide.movies.details.MovieDetails
import com.battlelancer.seriesguide.movies.tools.MovieTools.Lists
import com.google.common.truth.Truth.assertThat
import com.uwetrottmann.tmdb2.entities.Movie
import com.uwetrottmann.tmdb2.entities.ReleaseDate
import com.uwetrottmann.tmdb2.entities.ReleaseDatesResult
import com.uwetrottmann.tmdb2.entities.ReleaseDatesResults
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(application = EmptyTestApplication::class)
class MovieToolsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    class MovieToolsTestEnv(context: Context) {
        val databaseHelper: MovieHelper = mock()
        val downloader: MovieDownloader = mock()

        val movieTools = MovieTools(
            context,
            databaseHelper,
            downloader
        )
    }

    private fun addToList_isInDatabase_isUpdated(list: Lists, verify: (MovieHelper) -> Unit) =
        runTest {
            val testEnv = MovieToolsTestEnv(context)
                .apply {
                    // So isMovieInDatabase returns true
                    `when`(databaseHelper.getCount(TEST_MOVIE_TMDBID))
                        .thenReturn(1)
                    // So updateMovie returns true
                    `when`(databaseHelper.updateInCollection(TEST_MOVIE_TMDBID, true))
                        .thenReturn(1)
                    `when`(databaseHelper.updateInWatchlist(TEST_MOVIE_TMDBID, true))
                        .thenReturn(1)
                    `when`(databaseHelper.setWatchedAndAddPlay(TEST_MOVIE_TMDBID))
                        .thenReturn(1)
                }

            assertThat(
                testEnv.movieTools
                    .addToList(TEST_MOVIE_TMDBID, list)
            ).isTrue()

            verify(testEnv.databaseHelper)
        }

    @Test
    fun addToList_collection_isInDatabase_isUpdated() {
        addToList_isInDatabase_isUpdated(Lists.COLLECTION) {
            verify(it).updateInCollection(TEST_MOVIE_TMDBID, true)
            verify(it, never()).updateInWatchlist(anyInt(), anyBoolean())
            verify(it, never()).setWatchedAndAddPlay(anyInt())
        }
    }

    @Test
    fun addToList_watchlist_isInDatabase_isUpdated() {
        addToList_isInDatabase_isUpdated(Lists.WATCHLIST) {
            verify(it).updateInWatchlist(TEST_MOVIE_TMDBID, true)
            verify(it, never()).updateInCollection(anyInt(), anyBoolean())
            verify(it, never()).setWatchedAndAddPlay(anyInt())
        }
    }

    @Test
    fun addToList_watched_isInDatabase_isUpdated() {
        addToList_isInDatabase_isUpdated(Lists.WATCHED) {
            verify(it).setWatchedAndAddPlay(TEST_MOVIE_TMDBID)
            verify(it, never()).updateInCollection(anyInt(), anyBoolean())
            verify(it, never()).updateInWatchlist(anyInt(), anyBoolean())
        }
    }

    private fun addToList_notInDatabase_isAdded(list: Lists) =
        runTest {
            val testEnv = MovieToolsTestEnv(context)
                .apply {
                    // So isMovieInDatabase returns false
                    `when`(databaseHelper.getCount(TEST_MOVIE_TMDBID))
                        .thenReturn(0)

                    // So addMovie returns true
                    `when`(downloader.getMovieDetailsWithDefaults(TEST_MOVIE_TMDBID, false))
                        .thenReturn(
                            MovieDownloader.MovieDetailsResult(
                                MovieDetails().apply {
                                    tmdbMovie(Movie())
                                },
                                isNotFoundOnTmdb = false
                            )
                        )
                }

            assertThat(
                testEnv.movieTools
                    .addToList(TEST_MOVIE_TMDBID, list)
            ).isTrue()

            // TODO Verify movie is added to database with expected list boolean set true
//            verify(testEnv.databaseHelper)

            // Verify never updated
            verify(testEnv.databaseHelper, never()).setWatchedAndAddPlay(anyInt())
            verify(testEnv.databaseHelper, never()).updateInCollection(anyInt(), anyBoolean())
            verify(testEnv.databaseHelper, never()).updateInWatchlist(anyInt(), anyBoolean())
        }

    @Test
    fun addToList_collection_notInDatabase_isAdded() {
        addToList_notInDatabase_isAdded(Lists.COLLECTION)
    }

    @Test
    fun addToList_watchlist_notInDatabase_isAdded() {
        addToList_notInDatabase_isAdded(Lists.WATCHLIST)
    }

    @Test
    fun addToList_watched_notInDatabase_isAdded() {
        addToList_notInDatabase_isAdded(Lists.WATCHED)
    }

    @Test
    fun updateReleaseDateForRegion() {
        val movie = Movie()
            .apply { release_date = Date(1) }

        MovieTools.updateReleaseDateForRegion(movie, null, "DE")
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

        MovieTools.updateReleaseDateForRegion(movie, releaseDates, "DE")
        // Picks oldest DE theatrical release date.
        assertThat(movie.release_date).isEqualTo(Date(1234))

        MovieTools.updateReleaseDateForRegion(movie, releaseDates, "US")
        // Picks single US date.
        assertThat(movie.release_date).isEqualTo(Date(123456))
    }

    companion object {
        private const val TEST_MOVIE_TMDBID = 12345
    }
}