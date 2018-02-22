package com.battlelancer.seriesguide.modules;

import com.uwetrottmann.tmdb2.Tmdb;
import com.uwetrottmann.tmdb2.services.ConfigurationService;
import com.uwetrottmann.tmdb2.services.FindService;
import com.uwetrottmann.tmdb2.services.MoviesService;
import com.uwetrottmann.tmdb2.services.PeopleService;
import com.uwetrottmann.tmdb2.services.SearchService;
import com.uwetrottmann.tmdb2.services.TvService;
import okhttp3.OkHttpClient;

public class TestTmdbModule extends TmdbModule {

    @Override
    ConfigurationService provideConfigurationService(Tmdb tmdb) {
        return null;
    }

    @Override
    FindService provideFindService(Tmdb tmdb) {
        return null;
    }

    @Override
    MoviesService provideMovieService(Tmdb tmdb) {
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

    @Override
    Tmdb provideSgTmdb(OkHttpClient okHttpClient) {
        return null;
    }
}
