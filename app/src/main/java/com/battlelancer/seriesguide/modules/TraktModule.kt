// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2016 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.modules

import android.content.Context
import com.battlelancer.seriesguide.traktapi.SgTrakt
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
class TraktModule {

    @Singleton
    @Provides
    fun provideTrakt(@ApplicationContext context: Context, okHttpClient: OkHttpClient): SgTrakt {
        return SgTrakt(context, okHttpClient)
    }
}