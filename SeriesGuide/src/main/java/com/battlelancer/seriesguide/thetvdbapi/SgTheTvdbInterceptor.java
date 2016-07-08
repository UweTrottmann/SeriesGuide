package com.battlelancer.seriesguide.thetvdbapi;

import android.content.Context;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.thetvdb.TheTvdbInterceptor;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * A custom {@link TheTvdbInterceptor} which does not require a {@link
 * com.uwetrottmann.thetvdb.TheTvdb} instance until intercepting.
 */
public class SgTheTvdbInterceptor implements Interceptor {

    private Context context;

    public SgTheTvdbInterceptor(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        return TheTvdbInterceptor.handleIntercept(chain,
                ServiceUtils.getTheTvdb(context).jsonWebToken());
    }
}
