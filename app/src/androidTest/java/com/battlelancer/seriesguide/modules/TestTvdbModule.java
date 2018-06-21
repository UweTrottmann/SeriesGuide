package com.battlelancer.seriesguide.modules;

import com.uwetrottmann.thetvdb.TheTvdb;
import com.uwetrottmann.thetvdb.services.TheTvdbEpisodes;
import com.uwetrottmann.thetvdb.services.TheTvdbSearch;
import com.uwetrottmann.thetvdb.services.TheTvdbSeries;

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
}
