package com.battlelancer.seriesguide.sync;

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

/**
 * WARNING: uses app context so uses actual app database. Ensure it is empty or tests might fail.
 */
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
        SyncOptions.SyncType syncType = SyncOptions.SyncType.SINGLE;

        TvdbSync tvdbSync = new TvdbSync(syncType, 0);

        assertThat(sync(tvdbSync), equalTo(null));
        assertThat(tvdbSync.hasUpdatedShows(), is(false));
    }

    @Test
    public void test_fullNoShows() {
        SyncOptions.SyncType syncType = SyncOptions.SyncType.FULL;

        TvdbSync tvdbSync = new TvdbSync(syncType, 0);

        assertThat(sync(tvdbSync), equalTo(SgSyncAdapter.UpdateResult.SUCCESS));
        assertThat(tvdbSync.hasUpdatedShows(), is(false));
    }

    @Test
    public void test_deltaNoShows() {
        SyncOptions.SyncType syncType = SyncOptions.SyncType.DELTA;

        TvdbSync tvdbSync = new TvdbSync(syncType, 0);

        assertThat(sync(tvdbSync), equalTo(SgSyncAdapter.UpdateResult.SUCCESS));
        assertThat(tvdbSync.hasUpdatedShows(), is(false));
    }

    @Nullable
    private SgSyncAdapter.UpdateResult sync(TvdbSync tvdbSync) {
        return tvdbSync.sync(InstrumentationRegistry.getContext(), tvdbToolsLazy,
                System.currentTimeMillis());
    }
}
