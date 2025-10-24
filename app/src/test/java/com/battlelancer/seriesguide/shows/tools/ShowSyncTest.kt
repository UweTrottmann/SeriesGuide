// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2025 Uwe Trottmann

package com.battlelancer.seriesguide.shows.tools

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.battlelancer.seriesguide.EmptyTestApplication
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.sync.SyncOptions
import com.battlelancer.seriesguide.sync.SyncProgress
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests using Room are recommended to be run on an actual device due to SQLite differences,
 * but this test does not really rely on the database.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = EmptyTestApplication::class)
class ShowSyncTest {

    @Before
    fun switchToInMemoryDb() {
        // ProviderTestRule does not work with Room
        // so instead blatantly replace the instance with one that uses an in-memory database
        val context = ApplicationProvider.getApplicationContext<Context>()
        SgRoomDatabase.switchToInMemory(context)
    }

    @After
    fun closeDb() {
        SgRoomDatabase.getInstance(ApplicationProvider.getApplicationContext()).close()
    }

    @Test
    fun test_singleNoId() {
        val syncType = SyncOptions.SyncType.SINGLE

        val showSync = ShowSync(syncType, 0)

        assertThat(sync(showSync)).isNull()
        assertThat(showSync.hasUpdatedShows()).isFalse()
    }

    @Test
    fun test_fullNoShows() {
        val syncType = SyncOptions.SyncType.FULL

        val showSync = ShowSync(syncType, 0)

        assertThat(sync(showSync)).isEqualTo(SgSyncAdapter.UpdateResult.SUCCESS)
        assertThat(showSync.hasUpdatedShows()).isFalse()
    }

    @Test
    fun test_deltaNoShows() {
        val syncType = SyncOptions.SyncType.DELTA

        val showSync = ShowSync(syncType, 0)

        assertThat(sync(showSync)).isEqualTo(SgSyncAdapter.UpdateResult.SUCCESS)
        assertThat(showSync.hasUpdatedShows()).isFalse()
    }

    private fun sync(showSync: ShowSync): SgSyncAdapter.UpdateResult? {
        return showSync.sync(
            ApplicationProvider.getApplicationContext(),
            System.currentTimeMillis(),
            SyncProgress()
        )
    }

    @Test
    fun getShowsToDeltaUpdate() {
        val currentTime = System.currentTimeMillis()

        val showSync = ShowSync(SyncOptions.SyncType.DELTA, 0)

        val showHelper = SgRoomDatabase
            .getInstance(ApplicationProvider.getApplicationContext())
            .sgShow2Helper()

        val returningToUpdate = showHelper.insertShow(
            ShowTestHelper.showToInsert().copy(
                status = ShowStatus.RETURNING,
                lastUpdatedMs = currentTime - ShowSync.UPDATE_THRESHOLD_MS - 1
            )
        )
        showHelper.insertShow(
            ShowTestHelper.showToInsert().copy(
                status = ShowStatus.RETURNING,
                lastUpdatedMs = currentTime
            )
        )
        val endedToUpdate = showHelper.insertShow(
            ShowTestHelper.showToInsert().copy(
                status = ShowStatus.ENDED,
                lastUpdatedMs = currentTime - ShowSync.UPDATE_THRESHOLD_ENDED_MS - 1
            )
        )
        // Should exclude ended show that only exceeds regular threshold
        showHelper.insertShow(
            ShowTestHelper.showToInsert().copy(
                status = ShowStatus.ENDED,
                lastUpdatedMs = currentTime - ShowSync.UPDATE_THRESHOLD_MS - 1
            )
        )
        showHelper.insertShow(
            ShowTestHelper.showToInsert().copy(
                status = ShowStatus.ENDED,
                lastUpdatedMs = currentTime
            )
        )

        val showsToUpdate = showSync.getShowsToDeltaUpdate(showHelper, currentTime)

        assertThat(showsToUpdate).containsExactlyElementsIn(
            listOf(
                returningToUpdate,
                endedToUpdate
            )
        )
    }
}

