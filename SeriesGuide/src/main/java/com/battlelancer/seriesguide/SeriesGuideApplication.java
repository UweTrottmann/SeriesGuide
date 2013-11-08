/*
 * Copyright 2011 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide;

import com.google.analytics.tracking.android.GoogleAnalytics;

import com.battlelancer.seriesguide.settings.AppSettings;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.BuildConfig;
import com.uwetrottmann.seriesguide.R;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ContentProvider;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.StrictMode.VmPolicy;
import android.preference.PreferenceManager;

/**
 * Initializes settings and services and on pre-ICS implements actions for low
 * memory state.
 * 
 * @author Uwe Trottmann
 */
public class SeriesGuideApplication extends Application {

    /**
     * The content authority used to identify the SeriesGuide
     * {@link ContentProvider}
     */
    public static String CONTENT_AUTHORITY;

    @Override
    public void onCreate() {
        super.onCreate();

        // Set provider authority
        CONTENT_AUTHORITY = getPackageName() + ".provider";

        // Initialize settings on first run
        PreferenceManager.setDefaultValues(this, R.xml.settings_basic, false);
        PreferenceManager.setDefaultValues(this, R.xml.settings_advanced, false);

        // Load the current theme into a global variable
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String theme = prefs.getString(SeriesGuidePreferences.KEY_THEME, "0");
        Utils.updateTheme(theme);

        // Ensure GA opt-out
        GoogleAnalytics.getInstance(this).setAppOptOut(AppSettings.isGaAppOptOut(this));

        // Enable StrictMode
        enableStrictMode();
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

}
