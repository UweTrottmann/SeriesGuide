// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2021 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.modules

import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.tmdbapi.SgTmdb
import com.uwetrottmann.tmdb2.Tmdb
import com.uwetrottmann.tmdb2.services.MoviesService
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
open class TmdbModule {

    @Singleton
    @Provides
    fun provideMovieService(tmdb: Tmdb): MoviesService {
        return tmdb.moviesService()
    }

    @Singleton
    @Provides
    fun provideSgTmdb(okHttpClient: OkHttpClient): Tmdb {
        return SgTmdb(okHttpClient, BuildConfig.TMDB_API_KEY)
    }
}