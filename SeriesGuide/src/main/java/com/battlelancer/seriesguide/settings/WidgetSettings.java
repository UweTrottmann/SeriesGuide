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

    public static final String KEY_PREFIX_WIDGET_BACKGROUND_OPACITY = "background_color_";

    public static final String KEY_PREFIX_WIDGET_THEME = "theme_";

    public static final String KEY_PREFIX_WIDGET_LISTTYPE = "type_";

    public static final String KEY_PREFIX_WIDGET_HIDE_WATCHED = "unwatched_";

    public static final String KEY_PREFIX_WIDGET_ONLY_FAVORITES = "only_favorites_";

    private static final int DEFAULT_WIDGET_BACKGROUND_OPACITY = 50;

    /**
     * Returns the type of episodes that the widget should display.
     *
     * @return One of {@link com.battlelancer.seriesguide.settings.WidgetSettings.Type}.
     */
    public static int getWidgetListType(Context context, int appWidgetId) {
        int type = Type.UPCOMING;
        try {
            type = Integer.parseInt(context.getSharedPreferences(SETTINGS_FILE, 0)
                    .getString(KEY_PREFIX_WIDGET_LISTTYPE + appWidgetId, "0"));
        } catch (NumberFormatException ignored) {
        }

        return type;
    }

    /**
     * Returns if this widget should not show watched episodes.
     */
    public static boolean isHidingWatchedEpisodes(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(SETTINGS_FILE, 0);
        return prefs.getBoolean(KEY_PREFIX_WIDGET_HIDE_WATCHED + appWidgetId, false);
    }

    /**
     * Returns if this widget should only show episodes of favorited shows.
     */
    public static boolean isOnlyFavoriteShows(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(SETTINGS_FILE, 0);
        return prefs.getBoolean(KEY_PREFIX_WIDGET_ONLY_FAVORITES + appWidgetId, false);
    }

    /**
     * Returns if this widget should use a light theme instead of the default one.
     */
    public static boolean isLightTheme(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(SETTINGS_FILE, 0);

        boolean isLightTheme = false;
        try {
            isLightTheme =
                    Integer.parseInt(prefs.getString(KEY_PREFIX_WIDGET_THEME + appWidgetId, "0"))
                            == 1;
        } catch (NumberFormatException ignored) {
        }
        return isLightTheme;
    }

    /**
     * Calculates the background color for this widget based on user preference.
     *
     * @param lightBackground If true, will return white with alpha. Otherwise black with alpha. See
     * {@link #isLightTheme}.
     */
    public static int getWidgetBackgroundColor(Context context, int appWidgetId,
            boolean lightBackground) {
        // taken from https://code.google.com/p/dashclock
        int opacity = DEFAULT_WIDGET_BACKGROUND_OPACITY;
        try {
            opacity = Integer.parseInt(context.getSharedPreferences(SETTINGS_FILE, 0)
                    .getString(KEY_PREFIX_WIDGET_BACKGROUND_OPACITY + appWidgetId, "50"));
        } catch (NumberFormatException ignored) {
        }

        if (opacity == 100) {
            // avoid overflow by handling 100
            return lightBackground ? 0xffffffff : 0xff000000;
        } else {
            int color = (opacity * 256 / 100) << 24;
            if (lightBackground) {
                color += 0x00ffffff;
            }
            return color;
        }
    }
}
