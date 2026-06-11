// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2022 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.movies.tools

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.battlelancer.seriesguide.EmptyTestApplication
import com.battlelancer.seriesguide.movies.database.MovieHelper
import com.battlelancer.seriesguide.movies.database.SgMovie
import com.battlelancer.seriesguide.movies.details.MovieDetails
import com.battlelancer.seriesguide.movies.tools.MovieTools.Lists
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.google.common.truth.Truth.assertThat
import com.uwetrottmann.tmdb2.entities.Movie
import com.uwetrottmann.tmdb2.entities.ReleaseDate
import com.uwetrottmann.tmdb2.entities.ReleaseDatesResult
import com.uwetrottmann.tmdb2.entities.ReleaseDatesResults
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(application = EmptyTestApplication::class)
class MovieToolsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var testDb: SgRoomDatabase

    @Before
    fun switchToInMemoryDb() {
        // Use an in-memory database for testing with Room
        SgRoomDatabase.switchToInMemory(context)
        testDb = SgRoomDatabase.getInstance(context)
    }

    @After
    fun closeDb() {
        testDb.close()
    }

    class MovieToolsTestEnv(context: Context, db: SgRoomDatabase) {
        val databaseHelper: MovieHelper = db.movieHelper()
        val downloader: MovieDownloader = mock()

        val movieTools = MovieTools(
            context,
            databaseHelper,
            downloader
        )

        fun insertTestMovie() {
            databaseHelper.insertMovie(TEST_MOVIE)
        }
    }

    private suspend fun addToListAndAssert(testEnv: MovieToolsTestEnv, list: Lists) {
        assertThat(
            testEnv.movieTools
                .addToList(TEST_MOVIE_TMDBID, list)
        ).isTrue()

        // Verify movie is in the database with expected list boolean set true
        val movieInDb = testEnv.databaseHelper.getMovie(TEST_MOVIE_TMDBID)
        assertThat(movieInDb).isNotNull()
        assertThat(movieInDb!!.tmdbId).isEqualTo(TEST_MOVIE_TMDBID)

        assertThat(movieInDb.inCollection).isEqualTo(list == Lists.COLLECTION)
        assertThat(movieInDb.inWatchlist).isEqualTo(list == Lists.WATCHLIST)
        assertThat(movieInDb.watched).isEqualTo(list == Lists.WATCHED)
        assertThat(movieInDb.plays).isEqualTo(if (list == Lists.WATCHED) 1 else 0)
    }

    private fun addToList_isInDatabase_isUpdated(list: Lists) =
        runTest {
            val testEnv = MovieToolsTestEnv(context, testDb)
                .apply {
                    // So isMovieInDatabase returns true
                    insertTestMovie()
                }

            addToListAndAssert(testEnv, list)
        }

    @Test
    fun addToList_collection_isInDatabase_isUpdated() {
        addToList_isInDatabase_isUpdated(Lists.COLLECTION)
    }

    @Test
    fun addToList_watchlist_isInDatabase_isUpdated() {
        addToList_isInDatabase_isUpdated(Lists.WATCHLIST)
    }

    @Test
    fun addToList_watched_isInDatabase_isUpdated() {
        addToList_isInDatabase_isUpdated(Lists.WATCHED)
    }

    private fun addToList_notInDatabase_isAdded(list: Lists) =
        runTest {
            val testEnv = MovieToolsTestEnv(context, testDb)
                .apply {
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

            addToListAndAssert(testEnv, list)
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
        private val TEST_MOVIE = SgMovie(
            tmdbId = TEST_MOVIE_TMDBID
        )
    }
}