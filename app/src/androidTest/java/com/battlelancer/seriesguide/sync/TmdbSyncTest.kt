// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2025 Uwe Trottmann

package com.battlelancer.seriesguide.sync

import android.content.ContentResolver
import android.content.Context
import android.text.format.DateUtils
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.battlelancer.seriesguide.modules.AppModule
import com.battlelancer.seriesguide.modules.DaggerTestServicesComponent
import com.battlelancer.seriesguide.modules.TestHttpClientModule
import com.battlelancer.seriesguide.modules.TestServicesComponent
import com.battlelancer.seriesguide.modules.TestTmdbModule
import com.battlelancer.seriesguide.modules.TestTraktModule
import com.battlelancer.seriesguide.movies.database.MovieHelper
import com.battlelancer.seriesguide.movies.database.SgMovie
import com.battlelancer.seriesguide.movies.details.MovieDetails
import com.battlelancer.seriesguide.movies.tools.MovieTools
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.uwetrottmann.tmdb2.entities.Movie
import com.uwetrottmann.tmdb2.services.ConfigurationService
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
class TmdbSyncTest {

    @Inject
    lateinit var tmdbConfigService: ConfigurationService

    @Inject
    lateinit var movieTools: MovieTools

    private lateinit var resolver: ContentResolver
    private lateinit var db: SgRoomDatabase
    private lateinit var movieHelper: MovieHelper

    @Before
    fun setup() {
        /*
        ProviderTestRule does not work with Room, so instead blatantly replace the instance with one
        that uses an in-memory database and use the real ContentResolver.
        */
        val context = ApplicationProvider.getApplicationContext<Context>()
        SgRoomDatabase.switchToInMemory(context)
        resolver = context.contentResolver
        db = SgRoomDatabase.getInstance(context)
        movieHelper = db.movieHelper()

        val component: TestServicesComponent = DaggerTestServicesComponent.builder()
            .appModule(AppModule(context))
            .httpClientModule(TestHttpClientModule())
            .traktModule(TestTraktModule())
            .tmdbModule(TestTmdbModule())
            .build()
        component.inject(this)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun updatesMoviesLessFrequentIfOlder() {
        val lastUpdatedCurrent =
            System.currentTimeMillis()
        val lastUpdatedOutdated =
            System.currentTimeMillis() - TmdbSync.UPDATED_BEFORE_DAYS - DateUtils.DAY_IN_MILLIS
        val lastUpdatedVeryOutdated =
            System.currentTimeMillis() - TmdbSync.UPDATED_BEFORE_90_DAYS - DateUtils.DAY_IN_MILLIS
        val releaseDateCurrent =
            System.currentTimeMillis()
        val releaseDateOld =
            System.currentTimeMillis() - TmdbSync.RELEASED_AFTER_DAYS - DateUtils.DAY_IN_MILLIS

        // released today
        insertMovie(10, releaseDateCurrent, lastUpdatedCurrent)
        insertMovie(11, releaseDateCurrent, lastUpdatedOutdated)
        // released a while ago
        insertMovie(12, releaseDateOld, lastUpdatedCurrent)
        insertMovie(13, releaseDateOld, lastUpdatedOutdated)
        insertMovie(14, releaseDateOld, lastUpdatedVeryOutdated)

        doUpdateAndAssertSuccess()

        // only the recently released outdated and the older very outdated movie should have been updated
        val movies = movieHelper.getAllMovies()
        assertThat(findMovieWithId(movies, 10).lastUpdated).isEqualTo(lastUpdatedCurrent)
        assertThat(lastUpdatedOutdated < findMovieWithId(movies, 11).lastUpdated).isTrue()

        assertThat(findMovieWithId(movies, 12).lastUpdated).isEqualTo(lastUpdatedCurrent)
        assertThat(findMovieWithId(movies, 13).lastUpdated).isEqualTo(lastUpdatedOutdated)
        assertThat(lastUpdatedVeryOutdated < findMovieWithId(movies, 14).lastUpdated).isTrue()
    }

    @Test
    fun updatesMovieWithLastUpdatedIsNull() {
        // released today + last updated IS NULL
        insertMovie(12, System.currentTimeMillis(), null)

        doUpdateAndAssertSuccess()

        // the movie should have been updated
        val movies = movieHelper.getAllMovies()
        val dbMovie = findMovieWithId(movies, 12)
        assertThat(dbMovie.lastUpdated).isNotNull()
        assertThat(dbMovie.lastUpdated).isNotEqualTo(0)
    }

    private fun findMovieWithId(movies: List<SgMovie>, tmdbId: Int): SgMovie {
        for (movie in movies) {
            if (movie.tmdbId == tmdbId) {
                return movie
            }
        }
        assertWithMessage("Did not find movie with TMDB id $tmdbId").fail()
        throw IllegalArgumentException()
    }

    private fun insertMovie(tmdbId: Int, releaseDateMs: Long, lastUpdatedMs: Long?) {
        val movie = Movie().apply {
            id = tmdbId
            release_date = Date(releaseDateMs)
        }

        val details = MovieDetails().apply {
            isInCollection = true
            isInWatchlist = true
            tmdbMovie(movie)
        }

        val values = details.toContentValuesInsert()
        if (lastUpdatedMs == null) {
            values.remove(Movies.LAST_UPDATED)
        } else {
            values.put(Movies.LAST_UPDATED, lastUpdatedMs)
        }
        resolver.insert(Movies.CONTENT_URI, values)
    }

    private fun doUpdateAndAssertSuccess() {
        val tmdbSync = TmdbSync(
            ApplicationProvider.getApplicationContext(),
            tmdbConfigService,
            movieTools
        )
        val successful = tmdbSync.updateMovies(SyncProgress())
        assertThat(successful).isTrue()
    }
}
