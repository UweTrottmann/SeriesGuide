// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2022 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.movies.tools

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.battlelancer.seriesguide.EmptyTestApplication
import com.battlelancer.seriesguide.lists.database.SgListHelper
import com.battlelancer.seriesguide.movies.database.MovieHelper
import com.battlelancer.seriesguide.movies.database.SgMovie
import com.battlelancer.seriesguide.movies.details.MovieDetails
import com.battlelancer.seriesguide.movies.tools.MovieTools.Lists
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
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
        val listDatabaseHelper: SgListHelper = mock()
        val downloader: MovieDownloader = mock()

        val movieTools = MovieTools(
            context,
            databaseHelper,
            listDatabaseHelper,
            downloader
        )

        fun insertTestMovie() {
            databaseHelper.insertMovie(TEST_MOVIE)
        }

        suspend fun downloaderReturnsTestMovie() {
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

        fun getListItemsWithTmdbIdCount(returns: Int) {
            `when`(
                listDatabaseHelper
                    .getListItemsWithTmdbIdCount(TEST_MOVIE_TMDBID, ListItemTypes.TMDB_MOVIE)
            ).thenReturn(returns)
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
                    downloaderReturnsTestMovie()
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
    fun removeFromList_notOnAnyList_isRemoved() = runTest {
        val testEnv = MovieToolsTestEnv(context, testDb)
            .apply {
                // So addMovie returns true
                downloaderReturnsTestMovie()
                movieTools.addToList(TEST_MOVIE_TMDBID, Lists.COLLECTION)
            }

        assertThat(
            testEnv.movieTools.removeFromList(TEST_MOVIE_TMDBID, Lists.COLLECTION)
        ).isTrue()

        val deletedMovie = testEnv.databaseHelper.getMovie(TEST_MOVIE_TMDBID)
        assertThat(deletedMovie).isNull()
    }

    @Test
    fun removeFromList_stillOnCustomList_isUpdated() = runTest {
        val testEnv = MovieToolsTestEnv(context, testDb)
            .apply {
                insertTestMovie()
                getListItemsWithTmdbIdCount(returns = 1)
                movieTools.addToList(TEST_MOVIE_TMDBID, Lists.COLLECTION)
            }

        assertThat(
            testEnv.movieTools.removeFromList(TEST_MOVIE_TMDBID, Lists.COLLECTION)
        ).isTrue()

        // Not deleted, but updated instead
        val updatedMovie = testEnv.databaseHelper.getMovie(TEST_MOVIE_TMDBID)
        assertThat(updatedMovie).isNotNull()
        assertThat(updatedMovie!!.inCollection).isEqualTo(false)
    }

    private fun removeFromList_stillOnBuiltInList_isUpdated(removeFromList: Lists) = runTest {
        val testEnv = MovieToolsTestEnv(context, testDb)
            .apply {
                // So addMovie returns true
                downloaderReturnsTestMovie()
                // For simplicity add to all lists
                movieTools.addToList(TEST_MOVIE_TMDBID, Lists.COLLECTION)
                movieTools.addToList(TEST_MOVIE_TMDBID, Lists.WATCHLIST)
                movieTools.addToList(TEST_MOVIE_TMDBID, Lists.WATCHED)
            }

        assertThat(
            testEnv.movieTools.removeFromList(TEST_MOVIE_TMDBID, removeFromList)
        ).isTrue()

        val updatedMovie = testEnv.databaseHelper.getMovie(TEST_MOVIE_TMDBID)
        assertThat(updatedMovie).isNotNull()
        assertThat(updatedMovie!!.inCollection).isEqualTo(removeFromList != Lists.COLLECTION)
        assertThat(updatedMovie.inWatchlist).isEqualTo(removeFromList != Lists.WATCHLIST)
        assertThat(updatedMovie.watched).isEqualTo(removeFromList != Lists.WATCHED)
        assertThat(updatedMovie.plays).isEqualTo(if (removeFromList != Lists.WATCHED) 1 else 0)
    }

    @Test
    fun removeFromList_collection_stillOnOtherList_isUpdated() {
        removeFromList_stillOnBuiltInList_isUpdated(Lists.COLLECTION)
    }

    @Test
    fun removeFromList_watchlist_stillOnOtherList_isUpdated() {
        removeFromList_stillOnBuiltInList_isUpdated(Lists.WATCHLIST)
    }

    @Test
    fun removeFromList_watched_stillOnOtherList_isUpdated() {
        removeFromList_stillOnBuiltInList_isUpdated(Lists.WATCHED)
    }

    @Test
    fun addToOrDeleteFromDatabaseAfterCustomListChange_notInCustomOrBuiltInList_isDeleted() =
        runTest {
            val testEnv = MovieToolsTestEnv(context, testDb)
                .apply {
                    insertTestMovie()
                    getListItemsWithTmdbIdCount(returns = 0)
                }

            assertThat(
                testEnv.movieTools
                    .addToOrDeleteFromDatabaseAfterCustomListChange(TEST_MOVIE_TMDBID)
            ).isTrue()

            val deletedMovie = testEnv.databaseHelper.getMovie(TEST_MOVIE_TMDBID)
            assertThat(deletedMovie).isNull()
        }

    private suspend fun addToOrDeleteFromDatabaseAfterCustomListChange_assertNotDeleted(testEnv: MovieToolsTestEnv) {
        assertThat(
            testEnv.movieTools
                .addToOrDeleteFromDatabaseAfterCustomListChange(TEST_MOVIE_TMDBID)
        ).isTrue()

        val untouchedMovie = testEnv.databaseHelper.getMovie(TEST_MOVIE_TMDBID)
        assertThat(untouchedMovie).isNotNull()

        // Indirectly verify that movie is not added, because addMovie would call the downloader
        verify(testEnv.downloader, never())
            .getMovieDetailsWithDefaults(anyInt(), anyBoolean())
    }

    private fun addToOrDeleteFromDatabaseAfterCustomListChange_notDeleted(
        addToList: Lists
    ) = runTest {
        val testEnv = MovieToolsTestEnv(context, testDb)
            .apply {
                insertTestMovie() // To be able to add to built-in list without "downloading"
                getListItemsWithTmdbIdCount(returns = 0)
                movieTools.addToList(TEST_MOVIE_TMDBID, addToList)
            }

        addToOrDeleteFromDatabaseAfterCustomListChange_assertNotDeleted(testEnv)
    }

    @Test
    fun addToOrDeleteFromDatabaseAfterCustomListChange_inCollection_notDeleted() {
        addToOrDeleteFromDatabaseAfterCustomListChange_notDeleted(Lists.COLLECTION)
    }

    @Test
    fun addToOrDeleteFromDatabaseAfterCustomListChange_onWatchlist_notDeleted() {
        addToOrDeleteFromDatabaseAfterCustomListChange_notDeleted(Lists.WATCHLIST)
    }

    @Test
    fun addToOrDeleteFromDatabaseAfterCustomListChange_isWatched_notDeleted() {
        addToOrDeleteFromDatabaseAfterCustomListChange_notDeleted(Lists.WATCHED)
    }

    @Test
    fun addToOrDeleteFromDatabaseAfterCustomListChange_inCustomList_notDeleted() = runTest {
        val testEnv = MovieToolsTestEnv(context, testDb)
            .apply {
                insertTestMovie()
                getListItemsWithTmdbIdCount(returns = 1)
            }

        addToOrDeleteFromDatabaseAfterCustomListChange_assertNotDeleted(testEnv)
    }

    @Test
    fun addToOrDeleteFromDatabaseAfterCustomListChange_inCustomListNotInDatabase_isAdded() = runTest {
        val testEnv = MovieToolsTestEnv(context, testDb)
            .apply {
                getListItemsWithTmdbIdCount(returns = 1)
                downloaderReturnsTestMovie()
            }

        assertThat(
            testEnv.movieTools
                .addToOrDeleteFromDatabaseAfterCustomListChange(TEST_MOVIE_TMDBID)
        ).isTrue()

        // Verify movie was added to the database, but not to any built-in lists
        val movieInDb = testEnv.databaseHelper.getMovie(TEST_MOVIE_TMDBID)
        assertThat(movieInDb).isNotNull()
        assertThat(movieInDb!!.tmdbId).isEqualTo(TEST_MOVIE_TMDBID)

        assertThat(movieInDb.inCollection).isFalse()
        assertThat(movieInDb.inWatchlist).isFalse()
        assertThat(movieInDb.watched).isFalse()
        assertThat(movieInDb.plays).isEqualTo(0)
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