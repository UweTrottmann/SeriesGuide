package com.battlelancer.seriesguide.sync;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import com.battlelancer.seriesguide.modules.AppModule;
import com.battlelancer.seriesguide.modules.DaggerTestServicesComponent;
import com.battlelancer.seriesguide.modules.TestHttpClientModule;
import com.battlelancer.seriesguide.modules.TestServicesComponent;
import com.battlelancer.seriesguide.modules.TestTmdbModule;
import com.battlelancer.seriesguide.modules.TestTraktModule;
import com.battlelancer.seriesguide.modules.TestTvdbModule;
import com.battlelancer.seriesguide.thetvdbapi.TvdbTools;
import dagger.Lazy;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class TvdbSyncTest {

    @Inject Lazy<TvdbTools> tvdbToolsLazy;

    @Before
    public void setUp() {
        TestServicesComponent component = DaggerTestServicesComponent.builder()
                .appModule(new AppModule(InstrumentationRegistry.getContext()))
                .httpClientModule(new TestHttpClientModule())
                .traktModule(new TestTraktModule())
                .tmdbModule(new TestTmdbModule())
                .tvdbModule(new TestTvdbModule())
                .build();
        component.inject(this);
    }

    @Test
    public void test_singleNoId() {
        TvdbSync.SyncType syncType = TvdbSync.SyncType.SINGLE;

        TvdbSync tvdbSync = new TvdbSync(syncArgs(syncType.id));
        assertThat(tvdbSync.syncType(), is(syncType));

        assertThat(sync(tvdbSync), equalTo(null));
        assertThat(tvdbSync.hasUpdatedShows(), is(false));
    }

    @Test
    public void test_fullNoShows() {
        TvdbSync.SyncType syncType = TvdbSync.SyncType.FULL;

        TvdbSync tvdbSync = new TvdbSync(syncArgs(syncType.id));

        assertThat(tvdbSync.syncType(), is(syncType));
        assertThat(sync(tvdbSync), equalTo(SgSyncAdapter.UpdateResult.SUCCESS));
        assertThat(tvdbSync.hasUpdatedShows(), is(false));
    }

    @Test
    public void test_deltaNoShows() {
        TvdbSync.SyncType syncType = TvdbSync.SyncType.DELTA;

        TvdbSync tvdbSync = new TvdbSync(syncArgs(syncType.id));

        assertThat(tvdbSync.syncType(), is(syncType));
        assertThat(sync(tvdbSync), equalTo(SgSyncAdapter.UpdateResult.SUCCESS));
        assertThat(tvdbSync.hasUpdatedShows(), is(false));
    }

    @Nullable
    private SgSyncAdapter.UpdateResult sync(TvdbSync tvdbSync) {
        return tvdbSync.sync(InstrumentationRegistry.getContext(), tvdbToolsLazy,
                System.currentTimeMillis());
    }

    @NonNull
    private Bundle syncArgs(int id) {
        Bundle args = new Bundle();
        args.putInt(SgSyncAdapter.EXTRA_SYNC_TYPE, id);
        return args;
    }
}
