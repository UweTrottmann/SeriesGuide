package com.battlelancer.seriesguide.modules

import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.tmdbapi.SgTmdb
import com.uwetrottmann.tmdb2.Tmdb
import com.uwetrottmann.tmdb2.services.ConfigurationService
import com.uwetrottmann.tmdb2.services.MoviesService
import com.uwetrottmann.tmdb2.services.PeopleService
import com.uwetrottmann.tmdb2.services.SearchService
import com.uwetrottmann.tmdb2.services.TvService
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
open class TmdbModule {
    @Singleton
    @Provides
    fun provideConfigurationService(tmdb: Tmdb): ConfigurationService {
        return tmdb.configurationService()
    }

    @Singleton
    @Provides
    fun provideMovieService(tmdb: Tmdb): MoviesService {
        return tmdb.moviesService()
    }

    @Singleton
    @Provides
    open fun providePeopleService(tmdb: Tmdb): PeopleService? {
        return tmdb.personService()
    }

    @Singleton
    @Provides
    open fun provideSearchService(tmdb: Tmdb): SearchService? {
        return tmdb.searchService()
    }

    @Singleton
    @Provides
    open fun provideTvService(tmdb: Tmdb): TvService? {
        return tmdb.tvService()
    }

    @Singleton
    @Provides
    fun provideSgTmdb(okHttpClient: OkHttpClient): Tmdb {
        return SgTmdb(okHttpClient, BuildConfig.TMDB_API_KEY)
    }
}