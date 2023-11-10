// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows.tools

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.episodes.EpisodeFlags
import com.battlelancer.seriesguide.shows.tools.ShowTestHelper.episodeToInsert
import com.battlelancer.seriesguide.shows.tools.ShowTestHelper.seasonToInsert
import com.battlelancer.seriesguide.shows.tools.ShowTestHelper.showToInsert
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NextEpisodeUpdaterTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: SgRoomDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SgRoomDatabase::class.java)
            .addCallback(SgRoomDatabase.SgRoomCallback(context))
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun updateForShows_noLastWatched_picksAfterNewestWatched() {
        val showHelper = db.sgShow2Helper()
        val episodeHelper = db.sgEpisode2Helper()

        // Insert show with no last watched episode ID set.
        val showId = showHelper.insertShow(showToInsert())
        val season = seasonToInsert(showId, 1)
        val seasonId = db.sgSeason2Helper().insertSeason(season)
        // There is a watched episode after an unwatched episode.
        val episode1 = season.episodeToInsert(seasonId, 1, 11000)
            .copy(watched = EpisodeFlags.UNWATCHED)
        val episode2 = season.episodeToInsert(seasonId, 2, 12000)
            .copy(watched = EpisodeFlags.WATCHED, plays = 1)
        val episode3 = season.episodeToInsert(seasonId, 3, 13000)
            .copy(watched = EpisodeFlags.UNWATCHED)
        val episodeIds = episodeHelper.insertEpisodes(listOf(episode1, episode2, episode3))

        val nextEpisodeId =
            NextEpisodeUpdater(context, showHelper, episodeHelper).updateForShows(showId)
        // Should choose the one released after the newest one watched.
        assertThat(nextEpisodeId).isEqualTo(episodeIds[2])
    }

    @Test
    fun updateForShows_releasedAfterButLowerNumbers_isPickedOverHigherNumber() {
        val showHelper = db.sgShow2Helper()
        val episodeHelper = db.sgEpisode2Helper()

        // Insert show with no last watched episode ID set.
        val showId = showHelper.insertShow(showToInsert())
        val specials = seasonToInsert(showId, 0)
        val specialsId = db.sgSeason2Helper().insertSeason(specials)
        val season1 = seasonToInsert(showId, 1)
        val season1Id = db.sgSeason2Helper().insertSeason(season1)
        // Unwatched special with release date after, but before regular episode with higher number.
        val special1 = specials.episodeToInsert(specialsId, 1, 12000)
            .copy(watched = EpisodeFlags.UNWATCHED)
        val episode1 = season1.episodeToInsert(season1Id, 1, 11000)
            .copy(watched = EpisodeFlags.WATCHED, plays = 1)
        val episode2 = season1.episodeToInsert(season1Id, 2, 13000)
            .copy(watched = EpisodeFlags.UNWATCHED)
        val episodeIds = episodeHelper.insertEpisodes(listOf(special1, episode1, episode2))
        // Set last watched ID to episode1.
        showHelper.updateLastWatchedEpisodeId(showId, episodeIds[1])

        val nextEpisodeId =
            NextEpisodeUpdater(context, showHelper, episodeHelper).updateForShows(showId)
        // Should pick the special released just afterwards instead of episode with higher number.
        assertThat(nextEpisodeId).isEqualTo(episodeIds[0])
    }

    @Test
    fun updateForShows_rewatchPicksWatchedWithLessPlays() {
        val showHelper = db.sgShow2Helper()
        val episodeHelper = db.sgEpisode2Helper()

        // Insert show with no last watched episode ID set.
        val showId = showHelper.insertShow(showToInsert())
        val season = seasonToInsert(showId, 1)
        val seasonId = db.sgSeason2Helper().insertSeason(season)
        // There is an episode with one play before an unwatched episode with zero plays.
        val episode1 = season.episodeToInsert(seasonId, 1, 11000)
            .copy(watched = EpisodeFlags.WATCHED, plays = 2)
        val episode2 = season.episodeToInsert(seasonId, 2, 12000)
            .copy(watched = EpisodeFlags.WATCHED, plays = 1)
        val episode3 = season.episodeToInsert(seasonId, 3, 13000)
            .copy(watched = EpisodeFlags.UNWATCHED)
        val episodeIds = episodeHelper.insertEpisodes(listOf(episode1, episode2, episode3))
        // Set last watched ID to episode1.
        showHelper.updateLastWatchedEpisodeId(showId, episodeIds[0])

        val nextEpisodeId =
            NextEpisodeUpdater(context, showHelper, episodeHelper).updateForShows(showId)
        // Should pick the one that has a single play and not the unwatched one.
        assertThat(nextEpisodeId).isEqualTo(episodeIds[1])
    }

    @Test
    fun updateForShows_showUpdatedWithExpectedValues() {
        val showHelper = db.sgShow2Helper()
        val episodeHelper = db.sgEpisode2Helper()

        // Insert show with no last watched episode ID set.
        val showId = showHelper.insertShow(showToInsert())
        val season1 = seasonToInsert(showId, 1)
        val season1Id = db.sgSeason2Helper().insertSeason(season1)
        val episode1 = season1.episodeToInsert(season1Id, 1, 11000)
            .copy(watched = EpisodeFlags.WATCHED, plays = 1)
        val episode1Id = episodeHelper.insertEpisode(episode1)
        // Set last watched ID to episode1.
        showHelper.updateLastWatchedEpisodeId(showId, episode1Id)

        val nextEpisodeUpdater = NextEpisodeUpdater(context, showHelper, episodeHelper)
        val noNextEpisodeId = nextEpisodeUpdater.updateForShows(showId)
        // There should be no next episode.
        assertThat(noNextEpisodeId).isEqualTo(0)
        val showNoNext = showHelper.getShow(showId)!!
        assertThat(showNoNext.nextEpisode).isEmpty()
        assertThat(showNoNext.nextAirdateMs).isEqualTo(NextEpisodeUpdater.UNKNOWN_NEXT_RELEASE_DATE)
        assertThat(showNoNext.nextText).isEmpty()
        assertThat(showNoNext.unwatchedCount).isEqualTo(0)

        // Add an unwatched episode
        val episode2 = season1.episodeToInsert(season1Id, 2, 12000)
            .copy(watched = EpisodeFlags.UNWATCHED)
        val episode2Id = episodeHelper.insertEpisode(episode2)

        val nextEpisodeId = nextEpisodeUpdater.updateForShows(showId)
        // Added episode should be the next one now.
        assertThat(nextEpisodeId).isEqualTo(episode2Id)
        val showWithNext = showHelper.getShow(showId)!!
        assertThat(showWithNext.nextEpisode).isEqualTo(episode2Id.toString())
        assertThat(showWithNext.nextAirdateMs).isEqualTo(episode2.firstReleasedMs)
        assertThat(showWithNext.nextText).isEqualTo("1x02 Episode 2")
        assertThat(showWithNext.unwatchedCount).isEqualTo(1)
    }

}