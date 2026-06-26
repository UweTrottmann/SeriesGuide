// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.shows.tools

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.battlelancer.seriesguide.EmptyTestApplication
import com.battlelancer.seriesguide.billing.BillingTools
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.tmdbapi.TmdbTools3.TmdbError
import com.battlelancer.seriesguide.tmdbapi.TmdbTools3.TmdbStop
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.google.common.truth.Truth.assertThat
import com.uwetrottmann.tmdb2.entities.TvSeason
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = EmptyTestApplication::class)
class AddUpdateShowToolsTest {

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

    // Note: could also test for failing to insert show or season, but would have to mock database
    // helpers to simulate failure. Which then won't allow testing if the database changes are
    // actually not applied.
    @Test
    fun addShow_getEpisodesOfSeasonFails_rollsBackChanges() {
        val testShow = ShowTestHelper.showToInsert()
        val testTmdbId = testShow.tmdbId!!
        val testLanguage = "en"

        // So getShowDetails returns an OK result and the show and season are inserted
        val showTools = ShowTools2(context, mock())
        val getShowTools: GetShowTools = mock()
        whenever(
            getShowTools.getShowDetails(
                testTmdbId,
                testLanguage
            )
        ).thenReturn(
            Ok(
                GetShowTools.ShowDetails(
                    show = testShow,
                    seasons = listOf(TEST_TMDB_SEASON)
                )
            )
        )

        // So HexagonSettings.isEnabled(context) returns false and fetching from Cloud is skipped
        BillingTools.updateUnlockState(context)

        // Fail getting episodes for season call
        val getEpisodesOfSeasonTools: GetEpisodesOfSeasonTools = mock()
        whenever(
            getEpisodesOfSeasonTools.getEpisodesOfSeason(
                any(),
                eq(testTmdbId),
                any(),
                any(),
                any(),
                any(),
                isNull(),
                isNull()
            )
        ).thenReturn(Err<TmdbError>(TmdbStop))

        val addUpdateShowTools = AddUpdateShowTools(
            context,
            getShowTools,
            mock(),
            mock(),
            { showTools },
            getEpisodesOfSeasonTools
        )

        val result = addUpdateShowTools.addShow(
            testTmdbId,
            testLanguage,
            null,
            null,
            mock()
        )

        assertThat(result).isEqualTo(AddUpdateShowTools.ShowResult.TMDB_ERROR)
        // Inserted show and season should not be persisted
        assertThat(testDb.sgShow2Helper().getShowsForExport()).isEmpty()
        assertThat(testDb.sgSeason2Helper().countSeasons()).isEqualTo(0);
    }

    companion object {
        private val TEST_TMDB_SEASON = TvSeason().apply {
            id = 1234
            season_number = 1
            name = "Test Season 1"
        }
    }

}