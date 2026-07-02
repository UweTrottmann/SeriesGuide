// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2018 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.provider

import android.content.ContentResolver
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.battlelancer.seriesguide.dataliberation.ImportTools.toSgEpisodeForImport
import com.battlelancer.seriesguide.dataliberation.ImportTools.toSgListForImport
import com.battlelancer.seriesguide.dataliberation.ImportTools.toSgListItemForImport
import com.battlelancer.seriesguide.dataliberation.ImportTools.toSgMovieForImport
import com.battlelancer.seriesguide.dataliberation.ImportTools.toSgSeasonForImport
import com.battlelancer.seriesguide.dataliberation.ImportTools.toSgShowForImport
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.ListItemTypesExport
import com.battlelancer.seriesguide.dataliberation.model.Episode
import com.battlelancer.seriesguide.dataliberation.model.List
import com.battlelancer.seriesguide.dataliberation.model.ListItem
import com.battlelancer.seriesguide.dataliberation.model.Season
import com.battlelancer.seriesguide.dataliberation.model.Show
import com.battlelancer.seriesguide.lists.ListsTools
import com.battlelancer.seriesguide.lists.database.SgList
import com.battlelancer.seriesguide.movies.database.SgMovie
import com.battlelancer.seriesguide.movies.database.toSgMovieForInsert
import com.battlelancer.seriesguide.movies.details.MovieDetails
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems
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
    private lateinit var testDb: SgRoomDatabase

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
        private const val TEST_LIST_NAME = "Test List"
        private val TEST_LIST_ID = ListsTools.generateListId(TEST_LIST_NAME)!!
        private val LIST = List().apply {
            name = TEST_LIST_NAME
            list_id = TEST_LIST_ID
        }
        private const val TEST_LIST_ITEM_EXTERNAL_ID = "test-external-id"
        private val LIST_ITEM = ListItem().apply {
            list_item_id = ListItems.generateListItemId(
                TEST_LIST_ITEM_EXTERNAL_ID,
                ListItemTypes.TMDB_SHOW,
                TEST_LIST_ID
            )
            externalId = TEST_LIST_ITEM_EXTERNAL_ID
            type = ListItemTypesExport.TMDB_SHOW
        }
        private const val TEST_MOVIE_TMDB_ID = 12
        private val MOVIE = MovieDetails().apply {
            tmdbMovie(Movie())
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
        testDb = SgRoomDatabase.getInstance(context)
        resolver = context.contentResolver
    }

    @After
    fun closeDb() {
        testDb.close()
    }

    @Test
    fun showDefaultValuesImport() {
        val showHelper = testDb.sgShow2Helper()

        val sgShow = SHOW.toSgShowForImport()
        val showId = showHelper.insertShow(sgShow)

        val show = showHelper.getShow(showId)
        if (show == null) {
            fail("show is null")
            return
        }

        // Check a primary key was assigned
        assertThat(show.id).isGreaterThan(0)

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
        val sgShow = SHOW.toSgShowForImport()
        val showId = testDb.sgShow2Helper().insertShow(sgShow)

        val sgSeason = SEASON.toSgSeasonForImport(showId)
        val seasonId = testDb.sgSeason2Helper().insertSeason(sgSeason)

        val season = testDb.sgSeason2Helper().getSeason(seasonId)
        if (season == null) {
            fail("season is null")
            return
        }

        // Check a primary key was assigned
        assertThat(season.id).isGreaterThan(0)

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
        val sgShow = SHOW.toSgShowForImport()
        val showId = testDb.sgShow2Helper().insertShow(sgShow)

        val sgSeason = SEASON.toSgSeasonForImport(showId)
        val seasonId = testDb.sgSeason2Helper().insertSeason(sgSeason)

        val sgEpisode = EPISODE.toSgEpisodeForImport(showId, seasonId, sgSeason.number)
        val episodeId = testDb.sgEpisode2Helper().insertEpisode(sgEpisode)

        val episode = testDb.sgEpisode2Helper().getEpisode(episodeId)
        if (episode == null) {
            fail("episode is null")
            return
        }

        // Check a primary key was assigned
        assertThat(episode.id).isGreaterThan(0)

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
        val addListTask = AddListTask(ApplicationProvider.getApplicationContext(), LIST.name)
        addListTask.doDatabaseUpdate(resolver, addListTask.listId)

        val lists = testDb.sgListHelper().getListsForExport()
        // Initial data + new list from above; initial data asserted with RoomInitialDataTest.
        assertThat(lists).hasSize(2)
        assertTestList(lists[1])
    }

    @Test
    fun listDefaultValuesImport() {
        val listHelper = testDb.sgListHelper()
        // By default, the database inserts a first list when being created: delete it
        listHelper.deleteAllLists()

        // List
        val sgList = LIST.toSgListForImport()

        listHelper.insertList(sgList)

        val lists = listHelper.getListsForExport()
        assertThat(lists).hasSize(1)
        assertTestList(lists[0])

        // List item
        val sgListItem = LIST_ITEM.toSgListItemForImport(TEST_LIST_ID)!!

        listHelper.insertListItems(listOf(sgListItem))

        val listItems = listHelper.getListItemsForExport(TEST_LIST_ID)
        assertThat(listItems).hasSize(1)
        val listItem = listItems[0]

        // Check a primary key was assigned
        assertThat(listItem.id).isGreaterThan(0)

        assertThat(listItem.listItemId).isEqualTo(LIST_ITEM.list_item_id)
        assertThat(listItem.type).isEqualTo(ListItemTypes.TMDB_SHOW)
        assertThat(listItem.itemRefId).isEqualTo(LIST_ITEM.externalId)
        assertThat(listItem.listId).isEqualTo(TEST_LIST_ID)
    }

    private fun assertTestList(actualList: SgList) {
        // Check a primary key was assigned
        assertThat(actualList.id).isGreaterThan(0)

        assertThat(actualList.name).isEqualTo(LIST.name)
        assertThat(actualList.order).isEqualTo(0)
    }

    @Test
    fun movieDefaultValues() {
        val movieHelper = testDb.movieHelper()

        val sgMovie = MOVIE.toSgMovieForInsert(TEST_MOVIE_TMDB_ID)

        movieHelper.insertMovie(sgMovie)

        assertMovie(movieHelper.getMovie(TEST_MOVIE_TMDB_ID))
    }

    @Test
    fun movieDefaultValuesImport() {
        val movieHelper = testDb.movieHelper()

        movieHelper.insertMovie(MOVIE_I.toSgMovieForImport())

        assertMovie(movieHelper.getAllMovies()[0])
    }

    private fun assertMovie(movie: SgMovie?) {
        assertThat(movie).isNotNull()

        // Check a primary key was assigned
        assertThat(movie!!.id).isGreaterThan(0)

        assertThat(movie.runtimeMin).isEqualTo(0)
        assertThat(movie.inCollection).isEqualTo(false)
        assertThat(movie.inWatchlist).isEqualTo(false)
        assertThat(movie.plays).isEqualTo(0)
        assertThat(movie.watched).isEqualTo(false)
        assertThat(movie.ratingTmdb).isEqualTo(0.0)
        assertThat(movie.ratingVotesTmdb).isEqualTo(0)
        assertThat(movie.ratingTrakt).isEqualTo(0)
        assertThat(movie.ratingVotesTrakt).isEqualTo(0)
    }

}
