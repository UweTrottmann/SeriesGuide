package com.battlelancer.seriesguide.modules;

import android.content.Context;
import com.uwetrottmann.thetvdb.TheTvdb;
import com.uwetrottmann.thetvdb.services.TheTvdbEpisodes;
import com.uwetrottmann.thetvdb.services.TheTvdbSearch;
import com.uwetrottmann.thetvdb.services.TheTvdbSeries;
import okhttp3.OkHttpClient;

public class TestTvdbModule extends TvdbModule {

    @Override
    TheTvdbEpisodes provideEpisodesService(TheTvdb theTvdb) {
        return null;
    }

    @Override
    TheTvdbSearch provideSearch(TheTvdb theTvdb) {
        return null;
    }

    @Override
    TheTvdbSeries provideSeriesService(TheTvdb theTvdb) {
        return null;
    }

    @Override
    TheTvdb provideTheTvdb(@ApplicationContext Context context, OkHttpClient okHttpClient) {
        return null;
    }
}
