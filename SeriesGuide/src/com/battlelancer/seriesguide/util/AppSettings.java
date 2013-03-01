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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.battlelancer.seriesguide.enums.WidgetListType;

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

    public static final String SETTINGS_LIST_WIDGETS = "ListWidgetPreferences";

    public static final String KEY_PREFIX_LISTTYPE = "listtype_";

    public static final String KEY_PREFIX_HIDE_WATCHED = "unwatched_";

    public static int getWidgetListType(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(SETTINGS_LIST_WIDGETS, 0);
        return prefs.getInt(KEY_PREFIX_LISTTYPE + appWidgetId, WidgetListType.UPCOMING.index);
    }

    public static boolean getWidgetHidesWatched(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(SETTINGS_LIST_WIDGETS, 0);
        return prefs.getBoolean(KEY_PREFIX_HIDE_WATCHED + appWidgetId, false);
    }

    public static boolean saveWidgetConfiguration(Context context, int appWidgetId, int listType,
            boolean displaysWatched) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(SETTINGS_LIST_WIDGETS, 0)
                .edit();

        prefs.putInt(KEY_PREFIX_LISTTYPE + appWidgetId, listType);
        prefs.putBoolean(KEY_PREFIX_HIDE_WATCHED + appWidgetId, displaysWatched);

        return prefs.commit();
    }

}
