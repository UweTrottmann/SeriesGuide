package com.battlelancer.seriesguide.modules

import android.content.Context
import okhttp3.Cache

class TestHttpClientModule : HttpClientModule() {

    override fun provideOkHttpCache(@ApplicationContext context: Context): Cache {
        val cacheDir = createApiCacheDir(context, "$API_CACHE-test")
        return Cache(cacheDir, calculateApiDiskCacheSize(cacheDir))
    }

}