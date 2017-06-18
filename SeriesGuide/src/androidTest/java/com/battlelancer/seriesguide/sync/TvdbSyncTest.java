package com.battlelancer.seriesguide.sync;

import android.os.Bundle;
import android.support.test.runner.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class TvdbSyncTest {

    @Test
    public void test_singleNoId() {
        Bundle args = new Bundle();
        args.putInt(SgSyncAdapter.EXTRA_SYNC_TYPE, TvdbSync.SyncType.SINGLE.id);

        TvdbSync tvdbSync = new TvdbSync(args);

        assertThat(tvdbSync.syncType(), is(TvdbSync.SyncType.SINGLE));
        assertThat(tvdbSync.sync(null, null, 0), equalTo(null));
        assertThat(tvdbSync.hasUpdatedShows(), is(false));
    }
}
