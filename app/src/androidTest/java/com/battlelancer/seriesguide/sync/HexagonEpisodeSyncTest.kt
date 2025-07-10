// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.battlelancer.seriesguide.backend.HexagonTools
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgEpisode2
import com.battlelancer.seriesguide.shows.episodes.EpisodeFlags
import com.battlelancer.seriesguide.shows.tools.ShowTestHelper
import com.battlelancer.seriesguide.shows.tools.ShowTestHelper.episodeToInsert
import com.google.api.client.util.DateTime
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.uwetrottmann.seriesguide.backend.episodes.model.SgCloudEpisode
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class HexagonEpisodeSyncTest {

    private lateinit var db: SgRoomDatabase

    @Before
    fun switchToInMemoryDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SgRoomDatabase.switchToInMemory(context)
        db = SgRoomDatabase.getInstance(context)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun buildAndApplyEpisodeUpdatesFromCloud() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val hexagonTools = HexagonTools(context)
        val showHelper = db.sgShow2Helper()
        val episodeHelper = db.sgEpisode2Helper()
        val episodeSync =
            HexagonEpisodeSync(context, hexagonTools, episodeHelper, showHelper)

        // Insert test show
        val testShow = ShowTestHelper.showToInsert()
        val tmdbId = testShow.tmdbId!!
        val showId = showHelper.insertShow(testShow)
        val testSeason = ShowTestHelper.seasonToInsert(showId, 0)
        val seasonId = db.sgSeason2Helper().insertSeason(testSeason)

        val toRemainUnwatchedEpisode =
            episodeHelper.insertEpisode(testSeason.episodeToInsert(seasonId, 1, 0))
        val toBeWatchedOnceEpisode =
            episodeHelper.insertEpisode(testSeason.episodeToInsert(seasonId, 2, 0))
        val toBeSkippedEpisode =
            episodeHelper.insertEpisode(testSeason.episodeToInsert(seasonId, 3, 0))
        val toBeCollectedEpisode =
            episodeHelper.insertEpisode(testSeason.episodeToInsert(seasonId, 4, 0))
        val toBeWatchedAndCollectedEpisode =
            episodeHelper.insertEpisode(testSeason.episodeToInsert(seasonId, 5, 0))

        val toBeUnwatchedEpisode =
            episodeHelper.insertEpisode(
                testSeason.episodeToInsert(seasonId, 6, 0)
                    .copy(watched = EpisodeFlags.WATCHED, plays = 1)
            )
        val toBeNotCollectedEpisode =
            episodeHelper.insertEpisode(
                testSeason.episodeToInsert(seasonId, 7, 0)
                    .copy(collected = true)
            )
        val toBeNotWatchedOrCollectedEpisode =
            episodeHelper.insertEpisode(
                testSeason.episodeToInsert(seasonId, 8, 0)
                    .copy(watched = EpisodeFlags.WATCHED, plays = 1, collected = true)
            )

        val toBeWatchedTwiceEpisode =
            episodeHelper.insertEpisode(
                testSeason.episodeToInsert(seasonId, 9, 0)
                    .copy(watched = EpisodeFlags.WATCHED, plays = 1)
            )

        // Apply cloud state
        val episodes = listOf(
            // Test legacy Cloud data: no value for plays
            cloudEpisodeOf(tmdbId, 0, 2, EpisodeFlags.WATCHED, null, null),
            cloudEpisodeOf(tmdbId, 0, 3, EpisodeFlags.SKIPPED, 0, null),
            cloudEpisodeOf(tmdbId, 0, 4, null, null, true),
            cloudEpisodeOf(tmdbId, 0, 5, EpisodeFlags.WATCHED, 1, true),

            cloudEpisodeOf(tmdbId, 0, 6, EpisodeFlags.UNWATCHED, 0, null),
            cloudEpisodeOf(tmdbId, 0, 7, null, null, false),
            cloudEpisodeOf(tmdbId, 0, 8, EpisodeFlags.UNWATCHED, 0, false),

            cloudEpisodeOf(tmdbId, 0, 9, EpisodeFlags.WATCHED, 2, null)
        )

        episodeSync.buildAndApplyEpisodeUpdatesFromCloud(episodes, mapOf(tmdbId to showId))

        // Verify state was applied
        episodeHelper.getEpisode(toRemainUnwatchedEpisode)!!.assertEpisode(
            watchedExpected = EpisodeFlags.UNWATCHED,
            playsExpected = 0,
            inCollectionExpected = false
        )
        episodeHelper.getEpisode(toBeWatchedOnceEpisode)!!.assertEpisode(
            watchedExpected = EpisodeFlags.WATCHED,
            playsExpected = 1,
            inCollectionExpected = false
        )
        episodeHelper.getEpisode(toBeSkippedEpisode)!!.assertEpisode(
            watchedExpected = EpisodeFlags.SKIPPED,
            playsExpected = 0,
            inCollectionExpected = false
        )
        episodeHelper.getEpisode(toBeCollectedEpisode)!!.assertEpisode(
            watchedExpected = EpisodeFlags.UNWATCHED,
            playsExpected = 0,
            inCollectionExpected = true
        )
        episodeHelper.getEpisode(toBeWatchedAndCollectedEpisode)!!.assertEpisode(
            watchedExpected = EpisodeFlags.WATCHED,
            playsExpected = 1,
            inCollectionExpected = true
        )

        episodeHelper.getEpisode(toBeUnwatchedEpisode)!!.assertEpisode(
            watchedExpected = EpisodeFlags.UNWATCHED,
            playsExpected = 0,
            inCollectionExpected = false
        )
        episodeHelper.getEpisode(toBeNotCollectedEpisode)!!.assertEpisode(
            watchedExpected = EpisodeFlags.UNWATCHED,
            playsExpected = 0,
            inCollectionExpected = false
        )
        episodeHelper.getEpisode(toBeNotWatchedOrCollectedEpisode)!!.assertEpisode(
            watchedExpected = EpisodeFlags.UNWATCHED,
            playsExpected = 0,
            inCollectionExpected = false
        )

        episodeHelper.getEpisode(toBeWatchedTwiceEpisode)!!.assertEpisode(
            watchedExpected = EpisodeFlags.WATCHED,
            playsExpected = 2,
            inCollectionExpected = false
        )
    }

    @Test
    fun buildAndApplyEpisodeValuesFromCloud() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val hexagonTools = HexagonTools(context)
        val showHelper = db.sgShow2Helper()
        val episodeHelper = db.sgEpisode2Helper()
        val episodeSync =
            HexagonEpisodeSync(context, hexagonTools, episodeHelper, showHelper)

        // Insert test show
        val testShow = ShowTestHelper.showToInsert()
        val tmdbId = testShow.tmdbId!!
        val showId = showHelper.insertShow(testShow)
        val testSeason = ShowTestHelper.seasonToInsert(showId, 0)
        val seasonId = db.sgSeason2Helper().insertSeason(testSeason)
        val toRemainUnwatchedEpisode =
            episodeHelper.insertEpisode(testSeason.episodeToInsert(seasonId, 1, 0))
        val toBeWatchedOnceEpisode =
            episodeHelper.insertEpisode(testSeason.episodeToInsert(seasonId, 2, 0))
        val toBeSkippedEpisode =
            episodeHelper.insertEpisode(testSeason.episodeToInsert(seasonId, 3, 0))
        val toBeCollectedEpisode =
            episodeHelper.insertEpisode(testSeason.episodeToInsert(seasonId, 4, 0))
        val toBeWatchedAndCollectedEpisode =
            episodeHelper.insertEpisode(testSeason.episodeToInsert(seasonId, 5, 0))
        val toBeWatchedTwiceEpisode =
            episodeHelper.insertEpisode(testSeason.episodeToInsert(seasonId, 6, 0))

        // Apply cloud state
        val episodes = listOf(
            // Test legacy Cloud data: no value for plays
            cloudEpisodeOf(tmdbId, 0, 2, EpisodeFlags.WATCHED, null, null),
            cloudEpisodeOf(tmdbId, 0, 3, EpisodeFlags.SKIPPED, 0, null),
            cloudEpisodeOf(tmdbId, 0, 4, null, null, true),
            cloudEpisodeOf(tmdbId, 0, 5, EpisodeFlags.WATCHED, 1, true),
            cloudEpisodeOf(tmdbId, 0, 6, EpisodeFlags.WATCHED, 2, null)
        )

        episodeSync.buildAndApplyEpisodeValuesFromCloud(showId, episodes)

        // Verify state was applied
        episodeHelper.getEpisode(toRemainUnwatchedEpisode)!!.assertEpisode(
            watchedExpected = EpisodeFlags.UNWATCHED,
            playsExpected = 0,
            inCollectionExpected = false
        )
        episodeHelper.getEpisode(toBeWatchedOnceEpisode)!!.assertEpisode(
            watchedExpected = EpisodeFlags.WATCHED,
            playsExpected = 1,
            inCollectionExpected = false
        )
        episodeHelper.getEpisode(toBeSkippedEpisode)!!.assertEpisode(
            watchedExpected = EpisodeFlags.SKIPPED,
            playsExpected = 0,
            inCollectionExpected = false
        )
        episodeHelper.getEpisode(toBeCollectedEpisode)!!.assertEpisode(
            watchedExpected = EpisodeFlags.UNWATCHED,
            playsExpected = 0,
            inCollectionExpected = true
        )
        episodeHelper.getEpisode(toBeWatchedAndCollectedEpisode)!!.assertEpisode(
            watchedExpected = EpisodeFlags.WATCHED,
            playsExpected = 1,
            inCollectionExpected = true
        )
        episodeHelper.getEpisode(toBeWatchedTwiceEpisode)!!.assertEpisode(
            watchedExpected = EpisodeFlags.WATCHED,
            playsExpected = 2,
            inCollectionExpected = false
        )
    }

    private fun SgEpisode2.assertEpisode(
        watchedExpected: Int,
        playsExpected: Int,
        inCollectionExpected: Boolean
    ) {
        assertWithMessage("watched flag").that(watched).isEqualTo(watchedExpected)
        assertWithMessage("plays").that(plays).isEqualTo(playsExpected)
        assertWithMessage("collected").that(collected).isEqualTo(inCollectionExpected)
    }

    @Test
    fun getLatestUpdatedAt() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val hexagonTools = HexagonTools(context)
        val showHelper = db.sgShow2Helper()
        val episodeHelper = db.sgEpisode2Helper()
        val episodeSync =
            HexagonEpisodeSync(context, hexagonTools, episodeHelper, showHelper)

        val latestUpdatedAt = LocalDateTime.of(2025, 7, 11, 12, 1)
        val latestUpdatedAtDateTime = latestUpdatedAt.toDateTime()

        // Ignores not watched episodes
        val isUpdatedAtOfWatched = episodeSync.getLatestUpdatedAt(
            null,
            listOf(
                // Use later updatedAt and ensure it is not picked because episodes are not watched
                cloudEpisodeOf(1, 1, 1, null, null, null)
                    .setUpdatedAt(latestUpdatedAt.plusDays(1).toDateTime()),
                cloudEpisodeOf(1, 1, 2, EpisodeFlags.UNWATCHED, null, null)
                    .setUpdatedAt(latestUpdatedAt.plusDays(1).toDateTime()),
                cloudEpisodeOf(1, 2, 1, EpisodeFlags.WATCHED, null, null)
                    .setUpdatedAt(latestUpdatedAt.toDateTime()),
            )
        )
        assertThat(isUpdatedAtOfWatched).isEqualTo(latestUpdatedAtDateTime.value)

        // Also considers skipped episodes
        val isUpdatedAtOfSkipped = episodeSync.getLatestUpdatedAt(
            null,
            listOf(
                cloudEpisodeOf(1, 2, 1, EpisodeFlags.SKIPPED, null, null)
                    .setUpdatedAt(latestUpdatedAt.toDateTime()),
            )
        )
        assertThat(isUpdatedAtOfSkipped).isEqualTo(latestUpdatedAtDateTime.value)

        // Ignores episodes without updateAt or watched flag
        val isNone = episodeSync.getLatestUpdatedAt(
            null,
            listOf(
                cloudEpisodeOf(1, 1, 1, EpisodeFlags.WATCHED, null, null),
                cloudEpisodeOf(1, 1, 2, null, null, null),
            )
        )
        assertThat(isNone).isNull()

        // Keeps given time if its later
        val isSame = episodeSync.getLatestUpdatedAt(
            latestUpdatedAtDateTime.value,
            listOf(
                cloudEpisodeOf(1, 2, 1, EpisodeFlags.WATCHED, null, null)
                    .setUpdatedAt(latestUpdatedAt.minusDays(1).toDateTime()),
            )
        )
        assertThat(isSame).isEqualTo(latestUpdatedAtDateTime.value)
    }

    private fun LocalDateTime.toDateTime(): DateTime {
        return DateTime(toInstant(ZoneOffset.UTC).toEpochMilli())
    }

    private fun cloudEpisodeOf(
        show: Int, season: Int, number: Int, watchedFlag: Int?, plays: Int?, inCollection: Boolean?
    ): SgCloudEpisode {
        return SgCloudEpisode()
            .setShowTmdbId(show)
            .setEpisodeNumber(number)
            .setSeasonNumber(season)
            .setWatchedFlag(watchedFlag)
            .setPlays(plays)
            .setIsInCollection(inCollection)
    }
}