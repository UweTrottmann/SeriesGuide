package com.battlelancer.seriesguide.modules;

import com.uwetrottmann.tmdb2.Tmdb;
import com.uwetrottmann.tmdb2.services.FindService;
import com.uwetrottmann.tmdb2.services.PeopleService;
import com.uwetrottmann.tmdb2.services.SearchService;
import com.uwetrottmann.tmdb2.services.TvService;

public class TestTmdbModule extends TmdbModule {

    @Override
    FindService provideFindService(Tmdb tmdb) {
        return null;
    }

    @Override
    PeopleService providePeopleService(Tmdb tmdb) {
        return null;
    }

    @Override
    SearchService provideSearchService(Tmdb tmdb) {
        return null;
    }

    @Override
    TvService provideTvService(Tmdb tmdb) {
        return null;
    }
}
