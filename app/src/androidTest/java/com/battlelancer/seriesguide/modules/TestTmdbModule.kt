package com.battlelancer.seriesguide.modules

import com.uwetrottmann.tmdb2.Tmdb
import com.uwetrottmann.tmdb2.services.PeopleService
import com.uwetrottmann.tmdb2.services.SearchService
import com.uwetrottmann.tmdb2.services.TvService

class TestTmdbModule : TmdbModule() {
    override fun providePeopleService(tmdb: Tmdb): PeopleService? {
        return null
    }

    override fun provideSearchService(tmdb: Tmdb): SearchService? {
        return null
    }

    override fun provideTvService(tmdb: Tmdb): TvService? {
        return null
    }
}