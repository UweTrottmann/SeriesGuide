package com.battlelancer.seriesguide.sync;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import android.content.ContentResolver;
import android.content.Context;
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
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.thetvdbapi.TvdbTools;
import dagger.Lazy;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TvdbSyncTest {

    @Inject Lazy<TvdbTools> tvdbToolsLazy;

    private ContentResolver resolver;

    @Before
    public void switchToInMemoryDb() {
        // ProviderTestRule does not work with Room
        // so instead blatantly replace the instance with one that uses an in-memory database
        // and use the real ContentResolver
        Context context = InstrumentationRegistry.getTargetContext();
        SgRoomDatabase.switchToInMemory(context);
        resolver = context.getContentResolver();

        TestServicesComponent component = DaggerTestServicesComponent.builder()
                .appModule(new AppModule(InstrumentationRegistry.getContext()))
                .httpClientModule(new TestHttpClientModule())
                .traktModule(new TestTraktModule())
                .tmdbModule(new TestTmdbModule())
                .tvdbModule(new TestTvdbModule())
                .build();
        component.inject(this);
    }

    @After
    public void closeDb() {
        SgRoomDatabase.getInstance(InstrumentationRegistry.getTargetContext()).close();
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
        return tvdbSync.sync(InstrumentationRegistry.getContext(), resolver,
                tvdbToolsLazy, System.currentTimeMillis());
    }
}
