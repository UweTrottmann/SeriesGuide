package com.battlelancer.seriesguide.modules;

import android.content.Context;
import java.io.File;
import okhttp3.Cache;

public class TestHttpClientModule extends HttpClientModule {

    @Override
    Cache provideOkHttpCache(@ApplicationContext Context context) {
        File cacheDir = createApiCacheDir(context, API_CACHE + "-test");
        return new Cache(cacheDir, calculateApiDiskCacheSize(cacheDir));
    }
}
