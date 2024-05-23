// SPDX-License-Identifier: Apache-2.0
// Copyright 2016-2024 Uwe Trottmann

package com.battlelancer.seriesguide.modules

import android.content.Context
import com.battlelancer.seriesguide.traktapi.SgTrakt
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
    open fun provideEpisodes(trakt: SgTrakt): Episodes? {
        return trakt.episodes()
    }

    @Singleton
    @Provides
    open fun provideMovies(trakt: SgTrakt): Movies? {
        return trakt.movies()
    }

    @Singleton
    @Provides
    open fun provideShows(trakt: SgTrakt): Shows? {
        return trakt.shows()
    }

    @Singleton
    @Provides
    open fun provideSearch(trakt: SgTrakt): Search? {
        return trakt.search()
    }

    @Singleton
    @Provides
    open fun provideSync(trakt: SgTrakt): Sync? {
        return trakt.sync()
    }

    @Singleton
    @Provides
    open fun provideUsers(trakt: SgTrakt): Users? {
        return trakt.users()
    }

    @Singleton
    @Provides
    fun provideTrakt(@ApplicationContext context: Context, okHttpClient: OkHttpClient): SgTrakt {
        return SgTrakt(context, okHttpClient)
    }
}