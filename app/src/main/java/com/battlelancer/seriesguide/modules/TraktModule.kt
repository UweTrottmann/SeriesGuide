package com.battlelancer.seriesguide.modules

import android.content.Context
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.services.Episodes
import com.uwetrottmann.trakt5.services.Movies
import com.uwetrottmann.trakt5.services.Search
import com.uwetrottmann.trakt5.services.Shows
import com.uwetrottmann.trakt5.services.Sync
import com.uwetrottmann.trakt5.services.Users
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
open class TraktModule {
    @Singleton
    @Provides
    open fun provideEpisodes(trakt: TraktV2): Episodes? {
        return trakt.episodes()
    }

    @Singleton
    @Provides
    open fun provideMovies(trakt: TraktV2): Movies? {
        return trakt.movies()
    }

    @Singleton
    @Provides
    open fun provideShows(trakt: TraktV2): Shows? {
        return trakt.shows()
    }

    @Singleton
    @Provides
    open fun provideSearch(trakt: TraktV2): Search? {
        return trakt.search()
    }

    @Singleton
    @Provides
    open fun provideSync(trakt: TraktV2): Sync? {
        return trakt.sync()
    }

    @Singleton
    @Provides
    open fun provideUsers(trakt: TraktV2): Users? {
        return trakt.users()
    }

    @Singleton
    @Provides
    fun provideTrakt(@ApplicationContext context: Context, okHttpClient: OkHttpClient): TraktV2 {
        return SgTrakt(context, okHttpClient)
    }
}