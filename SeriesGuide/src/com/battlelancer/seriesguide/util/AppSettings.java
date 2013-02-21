/*
 * Copyright (C) 2013 Uwe Trottmann 
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

package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Retrieve settings values.
 */
public class AppSettings {

    public static final String KEY_WIDGET_BACKGROUND_COLOR = "widget_background_color";

    public static int getWidgetBackgroundColor(Context context) {
        // taken from https://code.google.com/p/dashclock
        int opacity = 50;
        try {
            opacity = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(KEY_WIDGET_BACKGROUND_COLOR, "50"));
        } catch (NumberFormatException ignored) {
        }

        if (opacity == 100) {
            return 0xff000000;
        } else {
            return (opacity * 256 / 100) << 24;
        }
    }
}
