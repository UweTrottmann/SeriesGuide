package com.battlelancer.seriesguide.modules;

import android.content.Context;
import com.battlelancer.seriesguide.thetvdbapi.SgTheTvdbInterceptor;
import com.battlelancer.seriesguide.traktapi.SgTraktInterceptor;
import com.battlelancer.seriesguide.util.AllApisAuthenticator;
import okhttp3.Cache;
import okhttp3.OkHttpClient;

public class TestHttpClientModule extends HttpClientModule {

    @Override
    OkHttpClient provideOkHttpClient(Cache cache, SgTheTvdbInterceptor tvdbInterceptor,
            SgTraktInterceptor traktInterceptor, AllApisAuthenticator authenticator) {
        return null;
    }

    @Override
    Cache provideOkHttpCache(@ApplicationContext Context context) {
        return null;
    }
}
