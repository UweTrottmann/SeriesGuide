/*
 * Copyright 2016 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.traktapi;

import android.content.Context;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.BaseOAuthActivity;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.trakt5.TraktV2;
import okhttp3.OkHttpClient;
import retrofit2.Response;

/**
 * Extends {@link TraktV2} to use our own caching OkHttp client and {@link
 * com.battlelancer.seriesguide.settings.TraktCredentials} to store user credentials.
 */
public class SgTrakt extends TraktV2 {

    private static String TAG_TRAKT_ERROR = "trakt Error";

    private final Context context;

    public SgTrakt(Context context) {
        super(BuildConfig.TRAKT_CLIENT_ID, BuildConfig.TRAKT_CLIENT_SECRET,
                BaseOAuthActivity.OAUTH_CALLBACK_URL_CUSTOM);
        this.context = context.getApplicationContext();
    }

    @Override
    public String accessToken() {
        return TraktCredentials.get(context).getAccessToken();
    }

    @Override
    protected synchronized OkHttpClient okHttpClient() {
        return ServiceUtils.getCachingOkHttpClient(context);
    }

    /**
     * Check if the request was unauthorized.
     *
     * @see #isUnauthorized(Context, Response)
     */
    public static boolean isUnauthorized(retrofit2.Response response) {
        return response.code() == 401;
    }

    /**
     * Returns if the request was not authorized. If it was, also calls {@link
     * TraktCredentials#setCredentialsInvalid()} to notify the user.
     */
    public static boolean isUnauthorized(Context context, retrofit2.Response response) {
        if (response.code() == 401) {
            // current access token is invalid, remove it and notify user to re-connect
            TraktCredentials.get(context).setCredentialsInvalid();
            return true;
        } else {
            return false;
        }
    }

    public static void trackFailedRequest(Context context, String action,
            retrofit2.Response response) {
        Utils.trackFailedRequest(context, TAG_TRAKT_ERROR, action, response);
    }

    public static void trackFailedRequest(Context context, String action, Throwable throwable) {
        Utils.trackFailedRequest(context, TAG_TRAKT_ERROR, action, throwable);
    }
}
