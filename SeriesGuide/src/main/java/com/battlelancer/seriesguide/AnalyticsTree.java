/*
 * Copyright 2014 Uwe Trottmann
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

package com.battlelancer.seriesguide;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.text.TextUtils;
import android.util.Log;
import com.battlelancer.seriesguide.thetvdbapi.TvdbException;
import com.battlelancer.seriesguide.util.Utils;
import com.crashlytics.android.Crashlytics;
import com.google.api.client.http.HttpResponseException;
import java.util.regex.Pattern;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * A customized {@link timber.log.Timber.DebugTree} that logs to Crashlytics and Google Analytics.
 * Always drops debug and verbose logs.
 */
public class AnalyticsTree extends Timber.DebugTree {

    private static final Pattern TMDB_V3_API_KEY = Pattern.compile("api_key=([^&]*)");

    private final Context context;

    public AnalyticsTree(Context context) {
        this.context = context.getApplicationContext();
    }

    private static String sanitizeTmdbUrls(String s) {
        if (s == null) {
            return null;
        }
        return TMDB_V3_API_KEY.matcher(s).replaceAll("api_key=<key-removed>");
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        if (priority == Log.ERROR) {
            // remove any stack trace attached by Timber
            if (message != null) {
                int newLine = message.indexOf('\n');
                if (newLine > 0) {
                    message = message.substring(0, newLine);
                }
            }

            // special treatment for retrofit and TheTVDB errors
            if (t instanceof RetrofitError) {
                RetrofitError e = (RetrofitError) t;

                // only logging retrofit HTTP and conversion errors for now
                if (e.getKind() == RetrofitError.Kind.HTTP) {
                    Utils.trackCustomEvent(context,
                            "Network Request Error",
                            tag + ": " + message,
                            e.getResponse().getStatus() + " " + sanitizeTmdbUrls(e.getUrl()));
                } else if (e.getKind() == RetrofitError.Kind.CONVERSION) {
                    Utils.trackCustomEvent(context,
                            "Request Conversion Error",
                            tag + ": " + message,
                            e.getResponse().getStatus() + " " + sanitizeTmdbUrls(e.getUrl()));
                }
                return;
            } else if (t instanceof TvdbException) {
                TvdbException e = (TvdbException) t;
                Utils.trackCustomEvent(context,
                        "TheTVDB Error",
                        tag + ": " + message,
                        e.getMessage());
                return;
            } else if (t instanceof OAuthProblemException) {
                // log trakt OAuth failures
                OAuthProblemException e = (OAuthProblemException) t;
                StringBuilder exceptionMessage = new StringBuilder();
                if (!TextUtils.isEmpty(e.getError())) {
                    exceptionMessage.append(e.getError());
                }
                if (!TextUtils.isEmpty(e.getDescription())) {
                    exceptionMessage.append(", ").append(e.getDescription());
                }
                if (!TextUtils.isEmpty(e.getUri())) {
                    exceptionMessage.append(", ").append(e.getUri());
                }
                Utils.trackCustomEvent(context,
                        "OAuth Error",
                        tag + ": " + message,
                        exceptionMessage.toString());
                return;
            } else if (t instanceof OAuthSystemException) {
                // log trakt OAuth failures
                OAuthSystemException e = (OAuthSystemException) t;
                Utils.trackCustomEvent(context,
                        "OAuth Error",
                        tag + ": " + message,
                        e.getMessage());
                return;
            } else if (t instanceof HttpResponseException) {
                // log Cloud errors
                HttpResponseException e = (HttpResponseException) t;
                Utils.trackCustomEvent(context,
                        "Hexagon Error",
                        tag + ": " + message,
                        e.getStatusCode() + ": " + e.getStatusMessage());
                return;
            }
        }

        // drop empty messages
        if (message == null) {
            return;
        }
        // drop debug and verbose logs
        if (priority == Log.DEBUG || priority == Log.VERBOSE) {
            return;
        }

        // transform priority into string
        String level = null;
        switch (priority) {
            case Log.INFO:
                level = "INFO";
                break;
            case Log.WARN:
                level = "WARN";
                break;
            case Log.ERROR:
                level = "ERROR";
                break;
        }

        // finally log to crashlytics
        Crashlytics.log(level + "/" + tag + ": " + message);

        // track some non-fatal exceptions with crashlytics
        if (priority == Log.ERROR) {
            if (t instanceof SQLiteException) {
                Crashlytics.logException(t);
            }
        }
    }
}
