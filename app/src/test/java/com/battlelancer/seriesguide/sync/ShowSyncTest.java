package com.battlelancer.seriesguide.sync;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import com.battlelancer.seriesguide.EmptyTestApplication;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.ui.shows.ShowTools;
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

    private ShowTools showTools;

    @Before
    public void switchToInMemoryDb() {
        // ProviderTestRule does not work with Room
        // so instead blatantly replace the instance with one that uses an in-memory database
        Context context = ApplicationProvider.getApplicationContext();
        SgRoomDatabase.switchToInMemory(context);

        showTools = new ShowTools(context);
    }

    @After
    public void closeDb() {
        SgRoomDatabase.getInstance(ApplicationProvider.getApplicationContext()).close();
    }

    @Test
    public void test_singleNoId() {
        SyncOptions.SyncType syncType = SyncOptions.SyncType.SINGLE;

        ShowSync showSync = new ShowSync(syncType, 0);

        assertThat(sync(showSync)).isNull();
        assertThat(showSync.hasUpdatedShows()).isFalse();
    }

    @Test
    public void test_fullNoShows() {
        SyncOptions.SyncType syncType = SyncOptions.SyncType.FULL;

        ShowSync showSync = new ShowSync(syncType, 0);

        assertThat(sync(showSync)).isEqualTo(SgSyncAdapter.UpdateResult.SUCCESS);
        assertThat(showSync.hasUpdatedShows()).isFalse();
    }

    @Test
    public void test_deltaNoShows() {
        SyncOptions.SyncType syncType = SyncOptions.SyncType.DELTA;

        ShowSync showSync = new ShowSync(syncType, 0);

        assertThat(sync(showSync)).isEqualTo(SgSyncAdapter.UpdateResult.SUCCESS);
        assertThat(showSync.hasUpdatedShows()).isFalse();
    }

    @Nullable
    private SgSyncAdapter.UpdateResult sync(ShowSync showSync) {
        return showSync.sync(ApplicationProvider.getApplicationContext(),
                showTools, System.currentTimeMillis(), new SyncProgress());
    }
}
