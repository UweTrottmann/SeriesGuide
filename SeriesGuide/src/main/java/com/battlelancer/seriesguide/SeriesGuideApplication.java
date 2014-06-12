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
import com.battlelancer.seriesguide.settings.AppSettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.Utils;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;
import com.uwetrottmann.androidutils.AndroidUtils;
import timber.log.Timber;

/**
 * Initializes settings and services and on pre-ICS implements actions for low memory state.
 *
 * @author Uwe Trottmann
 */
public class SeriesGuideApplication extends Application {

    public static final String FLAVOR_INTERNAL = "internal";

    /**
     * The content authority used to identify the SeriesGuide {@link ContentProvider}
     */
    public static String CONTENT_AUTHORITY;

    @Override
    public void onCreate() {
        super.onCreate();

        // logging setup
        if (BuildConfig.DEBUG) {
            // detailed logcat logging
            Timber.plant(new Timber.DebugTree());
        }
        if (!BuildConfig.DEBUG || FLAVOR_INTERNAL.equals(BuildConfig.FLAVOR)) {
            // crash and error reporting
            Timber.plant(new AnalyticsTree());
            Crashlytics.start(this);
        }

        // Set provider authority
        CONTENT_AUTHORITY = getString(R.string.provider_authority);

        // Initialize settings on first run
        PreferenceManager.setDefaultValues(this, R.xml.settings_basic, false);
        PreferenceManager.setDefaultValues(this, R.xml.settings_advanced, false);

        // Load the current theme into a global variable
        Utils.updateTheme(DisplaySettings.getThemeIndex(this));

        // Ensure GA opt-out
        GoogleAnalytics.getInstance(this).setAppOptOut(AppSettings.isGaAppOptOut(this));
        if (BuildConfig.DEBUG) {
            GoogleAnalytics.getInstance(this).setDryRun(true);
            GoogleAnalytics.getInstance(this).getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
        }
        // Initialize tracker
        Analytics.getTracker(this);

        // Enable StrictMode
        enableStrictMode();
    }

    /**
     * Used to enable {@link StrictMode} during production
     */
    @SuppressWarnings("PointlessBooleanExpression")
    @SuppressLint("NewApi")
    private static void enableStrictMode() {
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
}
