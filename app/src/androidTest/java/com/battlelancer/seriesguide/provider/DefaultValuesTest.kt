// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2025 Uwe Trottmann

package com.battlelancer.seriesguide.provider

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.dataliberation.ImportTools.toSgEpisodeForImport
import com.battlelancer.seriesguide.dataliberation.ImportTools.toSgSeasonForImport
import com.battlelancer.seriesguide.dataliberation.ImportTools.toSgShowForImport
import com.battlelancer.seriesguide.dataliberation.model.Episode
import com.battlelancer.seriesguide.dataliberation.model.List
import com.battlelancer.seriesguide.dataliberation.model.Season
import com.battlelancer.seriesguide.dataliberation.model.Show
import com.battlelancer.seriesguide.movies.details.MovieDetails
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies
import com.battlelancer.seriesguide.shows.database.SgShow2
import com.battlelancer.seriesguide.shows.episodes.EpisodeFlags
import com.battlelancer.seriesguide.util.tasks.AddListTask
import com.google.common.truth.Truth.assertThat
import com.uwetrottmann.tmdb2.entities.Movie
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DefaultValuesTest {

    private lateinit var resolver: ContentResolver

    companion object {
        private val SHOW = Show().apply {
            tmdb_id = 12
            tvdb_id = 12
        }
        private val SEASON = Season().apply {
            tmdb_id = "1234"
            tvdb_id = 1234
            season = 42
        }
        private val EPISODE = Episode().apply {
            tmdb_id = 123456
            tvdb_id = 123456
        }
        private val LIST = List().apply {
            name = "Test List"
            list_id = Lists.generateListId(name)
        }
        private val MOVIE = MovieDetails().apply {
            val tmdbMovie = Movie().apply { id = 12 }
            tmdbMovie(tmdbMovie)
        }
        private val MOVIE_I = com.battlelancer.seriesguide.dataliberation.model.Movie()
    }

    @Before
    fun switchToInMemoryDb() {
        // ProviderTestRule does not work with Room
        // so instead blatantly replace the instance with one that uses an in-memory database
        // and use the real ContentResolver
        val context = ApplicationProvider.getApplicationContext<Context>()
        SgRoomDatabase.switchToInMemory(context)
        resolver = context.contentResolver
    }

    @After
    fun closeDb() {
        SgRoomDatabase.getInstance(ApplicationProvider.getApplicationContext()).close()
    }

    @Test
    fun showDefaultValuesImport() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val showHelper = SgRoomDatabase.getInstance(context).sgShow2Helper()

        val sgShow = SHOW.toSgShowForImport()
        val showId = showHelper.insertShow(sgShow)

        val show = showHelper.getShow(showId)
        if (show == null) {
            fail("show is null")
            return
        }

        // Note: compare with SgShow2 and ImportTools.
        assertThat(show.tvdbId).isEqualTo(SHOW.tvdb_id)
        assertThat(show.title).isNotNull()
        assertThat(show.overview).isNotNull()
        assertThat(show.genres).isNotNull()
        assertThat(show.network).isNotNull()
        assertThat(show.runtime).isNotNull()
        assertThat(show.status).isNotNull()
        assertThat(show.contentRating).isNotNull()
        assertThat(show.nextEpisode).isNotNull()
        assertThat(show.poster).isNotNull()
        assertThat(show.posterSmall).isNotNull()
        assertThat(show.nextText).isNotNull()
        assertThat(show.imdbId).isNotNull()
        assertThat(show.traktId).isEqualTo(0)
        assertThat(show.favorite).isFalse()
        assertThat(show.hexagonMergeComplete).isTrue()
        assertThat(show.hidden).isFalse()
        assertThat(show.lastUpdatedMs).isEqualTo(0)
        assertThat(show.lastEditedSec).isEqualTo(0)
        assertThat(show.lastWatchedEpisodeId).isEqualTo(0)
        assertThat(show.lastWatchedMs).isEqualTo(0)
        assertThat(show.language).isNotNull()
        assertThat(show.unwatchedCount).isEqualTo(SgShow2.UNKNOWN_UNWATCHED_COUNT)
        assertThat(show.notify).isTrue()
        assertThat(show.customReleaseTime).isNull()
        assertThat(show.customReleaseDayOffset).isNull()
        assertThat(show.customReleaseTimeZone).isNull()
        assertThat(show.userNote).isNull()
        assertThat(show.userNoteTraktId).isNull()
    }

    @Test
    fun seasonDefaultValuesImport() {
        // with Room insert actually checks constraints, so add a matching show first
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = SgRoomDatabase.getInstance(context)

        val sgShow = SHOW.toSgShowForImport()
        val showId = database.sgShow2Helper().insertShow(sgShow)

        val sgSeason = SEASON.toSgSeasonForImport(showId)
        val seasonId = database.sgSeason2Helper().insertSeason(sgSeason)

        val season = database.sgSeason2Helper().getSeason(seasonId)
        if (season == null) {
            fail("season is null")
            return
        }

        assertThat(season.tmdbId).isEqualTo(SEASON.tmdb_id)
        assertThat(season.tvdbId).isEqualTo(SEASON.tvdb_id)
        assertThat(season.showId).isEqualTo(showId)
        assertThat(season.numberOrNull).isEqualTo(SEASON.season)
        // getInt returns 0 if NULL, so check explicitly
        assertThat(season.notWatchedReleasedOrNull).isEqualTo(0)
        assertThat(season.notWatchedToBeReleasedOrNull).isEqualTo(0)
        assertThat(season.notWatchedNoReleaseOrNull).isEqualTo(0)
        assertThat(season.totalOrNull).isEqualTo(0)
    }

    @Test
    fun episodeDefaultValuesImport() {
        // with Room insert actually checks constraints, so add a matching show and season first
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = SgRoomDatabase.getInstance(context)

        val sgShow = SHOW.toSgShowForImport()
        val showId = database.sgShow2Helper().insertShow(sgShow)

        val sgSeason = SEASON.toSgSeasonForImport(showId)
        val seasonId = database.sgSeason2Helper().insertSeason(sgSeason)

        val sgEpisode = EPISODE.toSgEpisodeForImport(showId, seasonId, sgSeason.number)
        val episodeId = database.sgEpisode2Helper().insertEpisode(sgEpisode)

        val episode = database.sgEpisode2Helper().getEpisode(episodeId)
        if (episode == null) {
            fail("episode is null")
            return
        }

        assertThat(episode.title).isNotNull()
        assertThat(episode.number).isEqualTo(0)
        assertThat(episode.watched).isEqualTo(EpisodeFlags.UNWATCHED)
        assertThat(episode.plays).isEqualTo(0)
        assertThat(episode.directors).isNotNull()
        assertThat(episode.guestStars).isNotNull()
        assertThat(episode.writers).isNotNull()
        assertThat(episode.image).isNotNull()
        assertThat(episode.collected).isFalse()
        assertThat(episode.imdbId).isNotNull()
        assertThat(episode.lastEditedSec).isEqualTo(0)
        assertThat(episode.lastUpdatedSec).isEqualTo(0)
    }

    @Test
    fun listDefaultValues() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = SgRoomDatabase.getInstance(context)

        val addListTask = AddListTask(ApplicationProvider.getApplicationContext(), LIST.name)
        addListTask.doDatabaseUpdate(resolver, addListTask.listId)

        val lists = database.sgListHelper().getListsForExport()
        // Initial data + new list from above; initial data asserted with RoomInitialDataTest.
        assertThat(lists).hasSize(2)
        assertThat(lists[1].name).isEqualTo(LIST.name)

        assertThat(lists[1].order).isEqualTo(0)
    }

    @Test
    fun listDefaultValuesImport() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = SgRoomDatabase.getInstance(context)
        // Delete initial list.
        database.sgListHelper().deleteAllLists()

        val values = LIST.toContentValues()

        val op = ContentProviderOperation.newInsert(Lists.CONTENT_URI)
            .withValues(values).build()

        val batch = ArrayList<ContentProviderOperation>()
        batch.add(op)
        resolver.applyBatch(SgApp.CONTENT_AUTHORITY, batch)

        val lists = database.sgListHelper().getListsForExport()
        assertThat(lists).hasSize(1)
        assertThat(lists[0].name).isEqualTo(LIST.name)

        assertThat(lists[0].order).isEqualTo(0)
    }

    @Test
    fun movieDefaultValues() {
        val values = MOVIE.toContentValuesInsert()
        resolver.insert(Movies.CONTENT_URI, values)

        assertMovie()
    }

    @Test
    fun movieDefaultValuesImport() {
        resolver.insert(Movies.CONTENT_URI, MOVIE_I.toContentValues())

        assertMovie()
    }

    private fun assertMovie() {
        val query = resolver.query(Movies.CONTENT_URI, null, null, null, null)
        assertThat(query).isNotNull()
        assertThat(query!!.count).isEqualTo(1)
        assertThat(query.moveToFirst()).isTrue()

        assertDefaultValue(query, Movies.RUNTIME_MIN, 0)
        assertDefaultValue(query, Movies.IN_COLLECTION, 0)
        assertDefaultValue(query, Movies.IN_WATCHLIST, 0)
        assertDefaultValue(query, Movies.PLAYS, 0)
        assertDefaultValue(query, Movies.WATCHED, 0)
        assertDefaultValue(query, Movies.RATING_TMDB, 0)
        assertDefaultValue(query, Movies.RATING_VOTES_TMDB, 0)
        assertDefaultValue(query, Movies.RATING_TRAKT, 0)
        assertDefaultValue(query, Movies.RATING_VOTES_TRAKT, 0)

        query.close()
    }

    private fun assertNotNullValue(query: Cursor, column: String) {
        assertThat(query.isNull(query.getColumnIndexOrThrow(column))).isFalse()
    }

    private fun assertDefaultValue(query: Cursor, column: String, defaultValue: Int) {
        assertNotNullValue(query, column)
        assertThat(query.getInt(query.getColumnIndexOrThrow(column))).isEqualTo(defaultValue)
    }
}
