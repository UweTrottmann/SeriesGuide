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

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

/**
 * Initializes settings and services and on pre-ICS implements actions for low
 * memory state.
 * 
 * @author Uwe Trottmann
 */
public class SeriesGuideApplication extends Application {

    public static String CONTENT_AUTHORITY;

    @Override
    public void onCreate() {
        super.onCreate();

        // set provider authority
        CONTENT_AUTHORITY = getPackageName() + ".provider";

        // initialize settings on first run
        PreferenceManager.setDefaultValues(this, R.xml.settings_basic, false);
        PreferenceManager.setDefaultValues(this, R.xml.settings_advanced, false);

        // load the current theme into a global variable
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String theme = prefs.getString(
                SeriesGuidePreferences.KEY_THEME, "0");
        Utils.updateTheme(theme);

        // set a context for Google Analytics
        EasyTracker.getInstance().setContext(getApplicationContext());
    }

    @Override
    public void onLowMemory() {
        if (!AndroidUtils.isICSOrHigher()) {
            // clear the whole cache as Honeycomb and below don't support
            // onTrimMemory (used directly in our ImageProvider)
            ImageProvider.getInstance(this).clearCache();
        }
    }

}
