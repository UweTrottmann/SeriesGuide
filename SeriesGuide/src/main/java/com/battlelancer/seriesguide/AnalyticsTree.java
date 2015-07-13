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
import android.util.Log;
import com.battlelancer.seriesguide.thetvdbapi.TvdbException;
import com.battlelancer.seriesguide.util.Utils;
import com.crashlytics.android.Crashlytics;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * A customized {@link timber.log.Timber.DebugTree} that logs to Crashlytics and Google Analytics.
 * Always drops debug and verbose logs.
 */
public class AnalyticsTree extends Timber.DebugTree {
    private final Context context;

    public AnalyticsTree(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        if (priority == Log.ERROR) {
            // special treatment for retrofit and TheTVDB errors
            if (t instanceof RetrofitError) {
                RetrofitError e = (RetrofitError) t;

                // only logging retrofit HTTP and conversion errors for now
                if (e.getKind() == RetrofitError.Kind.HTTP) {
                    Utils.trackCustomEvent(context,
                            "Network Request Error",
                            tag + ": " + message,
                            e.getResponse().getStatus() + " " + e.getUrl());
                } else if (e.getKind() == RetrofitError.Kind.CONVERSION) {
                    Utils.trackCustomEvent(context,
                            "Request Conversion Error",
                            tag + ": " + message,
                            e.getResponse().getStatus() + " " + e.getUrl());
                }
                return;
            } else if (t instanceof TvdbException) {
                TvdbException e = (TvdbException) t;
                Utils.trackCustomEvent(context,
                        "TheTVDB Error",
                        tag + ": " + message,
                        e.getMessage());
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
    }
}
