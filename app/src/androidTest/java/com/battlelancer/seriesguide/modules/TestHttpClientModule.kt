// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: GPL-3.0-or-later

package com.battlelancer.seriesguide.modules

import android.content.Context
import okhttp3.Cache

class TestHttpClientModule : HttpClientModule() {

    override fun provideOkHttpCache(@ApplicationContext context: Context): Cache {
        val cacheDir = createCacheDir(context, "$API_CACHE-test")
        return Cache(cacheDir, calculateApiDiskCacheSize(cacheDir))
    }

}