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

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ContentProvider;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.StrictMode.VmPolicy;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import com.battlelancer.seriesguide.settings.AppSettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.Utils;
import com.crashlytics.android.Crashlytics;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import java.net.URL;

/**
 * Initializes settings and services and on pre-ICS implements actions for low memory state.
 *
 * @author Uwe Trottmann
 */
public class SeriesGuideApplication extends Application {

    /**
     * The content authority used to identify the SeriesGuide {@link ContentProvider}
     */
    public static String CONTENT_AUTHORITY;

    @Override
    public void onCreate() {
        super.onCreate();

        if (!BuildConfig.DEBUG || BuildConfig.FLAVOR == "beta") {
            Crashlytics.start(this);
        }

        // Set provider authority
        CONTENT_AUTHORITY = getPackageName() + ".provider";

        // Initialize settings on first run
        PreferenceManager.setDefaultValues(this, R.xml.settings_basic, false);
        PreferenceManager.setDefaultValues(this, R.xml.settings_advanced, false);

        // Load the current theme into a global variable
        Utils.updateTheme(DisplaySettings.getThemeIndex(this));

        // OkHttp changes the global SSL context, breaks other HTTP clients like used by e.g. Google
        // Analytics.
        // https://github.com/square/okhttp/issues/184
        // So set OkHttp to handle all connections
        URL.setURLStreamHandlerFactory(AndroidUtils.createOkHttpClient());

        // Ensure GA opt-out
        GoogleAnalytics.getInstance(this).setAppOptOut(AppSettings.isGaAppOptOut(this));

        // Enable StrictMode
        enableStrictMode();

        upgrade();
    }

    @Override
    public void onLowMemory() {
        if (!AndroidUtils.isICSOrHigher()) {
            // Clear the whole cache as Honeycomb and below don't support
            // onTrimMemory (used directly in our ImageProvider)
            ImageProvider.getInstance(this).clearCache();
        }
    }

    /**
     * Used to enable {@link StrictMode} during production
     */
    @SuppressWarnings("PointlessBooleanExpression")
    @SuppressLint("NewApi")
    public static void enableStrictMode() {
        if (!BuildConfig.DEBUG || !AndroidUtils.isGingerbreadOrHigher()) {
            return;
        }
        // Enable StrictMode
        final ThreadPolicy.Builder threadPolicyBuilder = new ThreadPolicy.Builder();
        threadPolicyBuilder.detectAll();
        threadPolicyBuilder.penaltyLog();
        StrictMode.setThreadPolicy(threadPolicyBuilder.build());

        // Policy applied to all threads in the virtual machine's process
        final VmPolicy.Builder vmPolicyBuilder = new VmPolicy.Builder();
        vmPolicyBuilder.detectAll();
        vmPolicyBuilder.penaltyLog();
        if (AndroidUtils.isJellyBeanOrHigher()) {
            vmPolicyBuilder.detectLeakedRegistrationObjects();
        }
        StrictMode.setVmPolicy(vmPolicyBuilder.build());
    }

    private void upgrade() {
        /**
         * These upgrade procedures will run on each app launch until the last version gets updated
         * by launching the main activity.
         */
        final int lastVersion = AppSettings.getLastVersionCode(this);

        boolean isBeta = "beta".equals(BuildConfig.FLAVOR);

        // store trakt password in sync account
        if (!isBeta && lastVersion < 204 || isBeta && lastVersion < 216) {
            if (!TraktCredentials.get(this).hasCredentials()) {
                String password = TraktSettings.getPasswordSha1(this);
                if (!TextUtils.isEmpty(password)) {
                    String username = TraktCredentials.get(this).getUsername();
                    TraktCredentials.get(this).setCredentials(username, password);
                }
            }
        }
    }
}
