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

package com.battlelancer.seriesguide.settings;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Access some widget related settings values.
 */
public class WidgetSettings {

    public interface Type {
        int UPCOMING = 0;
        int RECENT = 1;
        int FAVORITES = 2;
    }

    public static final String SETTINGS_FILE = "ListWidgetPreferences";

    public static final String KEY_PREFIX_WIDGET_BACKGROUND_COLOR = "background_color_";

    public static final String KEY_PREFIX_WIDGET_LISTTYPE = "type_";

    public static final String KEY_PREFIX_WIDGET_HIDE_WATCHED = "unwatched_";

    private static final int DEFAULT_WIDGET_BACKGROUND_OPACITY = 50;

    public static int getWidgetListType(Context context, int appWidgetId) {
        int type = Type.UPCOMING;
        try {
            type = Integer.parseInt(context.getSharedPreferences(SETTINGS_FILE, 0)
                    .getString(KEY_PREFIX_WIDGET_LISTTYPE + appWidgetId, "0"));
        } catch (NumberFormatException ignored) {
        }

        return type;
    }

    public static boolean getWidgetHidesWatched(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(SETTINGS_FILE, 0);
        return prefs.getBoolean(KEY_PREFIX_WIDGET_HIDE_WATCHED + appWidgetId, false);
    }

    public static int getWidgetBackgroundColor(Context context, int appWidgetId) {
        // taken from https://code.google.com/p/dashclock
        int opacity = DEFAULT_WIDGET_BACKGROUND_OPACITY;
        try {
            opacity = Integer.parseInt(context.getSharedPreferences(SETTINGS_FILE, 0)
                    .getString(KEY_PREFIX_WIDGET_BACKGROUND_COLOR + appWidgetId, "50"));
        } catch (NumberFormatException ignored) {
        }

        if (opacity == 100) {
            return 0xff000000;
        } else {
            return (opacity * 256 / 100) << 24;
        }
    }

}
