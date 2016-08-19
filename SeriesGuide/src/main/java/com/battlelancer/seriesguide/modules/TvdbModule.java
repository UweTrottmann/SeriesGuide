package com.battlelancer.seriesguide.modules;

import android.app.Application;
import com.battlelancer.seriesguide.thetvdbapi.SgTheTvdb;
import com.uwetrottmann.thetvdb.TheTvdb;
import com.uwetrottmann.thetvdb.services.Search;
import com.uwetrottmann.thetvdb.services.SeriesService;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;

@Module
public class TvdbModule {

    @Singleton
    @Provides
    Search provideSearch(TheTvdb theTvdb) {
        return theTvdb.search();
    }

    @Singleton
    @Provides
    SeriesService provideSeriesServie(TheTvdb theTvdb) {
        return theTvdb.series();
    }

    @Singleton
    @Provides
    TheTvdb provideTheTvdb(Application application, OkHttpClient okHttpClient) {
        return new SgTheTvdb(application, okHttpClient);
    }
}
