// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.tools;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import com.battlelancer.seriesguide.EmptyTestApplication;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.sync.SyncOptions;
import com.battlelancer.seriesguide.sync.SyncProgress;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Tests using Room are recommended to be run on an actual device due to SQLite differences,
 * but this test does not really rely on the database.
 */
@RunWith(RobolectricTestRunner.class)
@Config(application = EmptyTestApplication.class)
public class ShowSyncTest {

    @Before
    public void switchToInMemoryDb() {
        // ProviderTestRule does not work with Room
        // so instead blatantly replace the instance with one that uses an in-memory database
        Context context = ApplicationProvider.getApplicationContext();
        SgRoomDatabase.switchToInMemory(context);
    }

    @After
    public void closeDb() {
        SgRoomDatabase.getInstance(ApplicationProvider.getApplicationContext()).close();
    }

    @Test
    public void test_singleNoId() throws InterruptedException {
        SyncOptions.SyncType syncType = SyncOptions.SyncType.SINGLE;

        ShowSync showSync = new ShowSync(syncType, 0);

        assertThat(sync(showSync)).isNull();
        assertThat(showSync.hasUpdatedShows()).isFalse();
    }

    @Test
    public void test_fullNoShows() throws InterruptedException {
        SyncOptions.SyncType syncType = SyncOptions.SyncType.FULL;

        ShowSync showSync = new ShowSync(syncType, 0);

        assertThat(sync(showSync)).isEqualTo(SgSyncAdapter.UpdateResult.SUCCESS);
        assertThat(showSync.hasUpdatedShows()).isFalse();
    }

    @Test
    public void test_deltaNoShows() throws InterruptedException {
        SyncOptions.SyncType syncType = SyncOptions.SyncType.DELTA;

        ShowSync showSync = new ShowSync(syncType, 0);

        assertThat(sync(showSync)).isEqualTo(SgSyncAdapter.UpdateResult.SUCCESS);
        assertThat(showSync.hasUpdatedShows()).isFalse();
    }

    @Nullable
    private SgSyncAdapter.UpdateResult sync(ShowSync showSync) throws InterruptedException {
        return showSync.sync(ApplicationProvider.getApplicationContext(),
                System.currentTimeMillis(), new SyncProgress());
    }
}
