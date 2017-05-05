package com.battlelancer.seriesguide.modules;

import android.app.Application;
import com.battlelancer.seriesguide.thetvdbapi.SgTheTvdb;
import com.uwetrottmann.thetvdb.TheTvdb;
import com.uwetrottmann.thetvdb.services.TheTvdbEpisodes;
import com.uwetrottmann.thetvdb.services.TheTvdbSearch;
import com.uwetrottmann.thetvdb.services.TheTvdbSeries;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;

@Module
public class TvdbModule {

    @Singleton
    @Provides
    TheTvdbEpisodes provideEpisodesService(TheTvdb theTvdb) {
        return theTvdb.episodes();
    }

    @Singleton
    @Provides
    TheTvdbSearch provideSearch(TheTvdb theTvdb) {
        return theTvdb.search();
    }

    @Singleton
    @Provides
    TheTvdbSeries provideSeriesService(TheTvdb theTvdb) {
        return theTvdb.series();
    }

    @Singleton
    @Provides
    TheTvdb provideTheTvdb(Application application, OkHttpClient okHttpClient) {
        return new SgTheTvdb(application, okHttpClient);
    }
}
