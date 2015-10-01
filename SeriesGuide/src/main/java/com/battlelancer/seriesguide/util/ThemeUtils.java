/*
 * Copyright 2015 Uwe Trottmann
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

import android.app.Activity;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;

/**
 * Helper methods to support SeriesGuide's different themes.
 */
public class ThemeUtils {

    /**
     * Sets the global app theme variable. Applied by all activities once they are created.
     */
    public static synchronized void updateTheme(String themeIndex) {
        int theme = Integer.valueOf(themeIndex);
        switch (theme) {
            case 1:
                SeriesGuidePreferences.THEME = R.style.Theme_SeriesGuide_DarkBlue;
                break;
            case 2:
                SeriesGuidePreferences.THEME = R.style.Theme_SeriesGuide_Light;
                break;
            default:
                SeriesGuidePreferences.THEME = R.style.Theme_SeriesGuide;
                break;
        }
    }

    /**
     * Applies an immersive theme (translucent status bar) to the given activity.
     */
    public static void setImmersiveTheme(Activity activity) {
        if (SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_Light) {
            activity.setTheme(R.style.Theme_SeriesGuide_Light_Immersive);
        } else if (SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_DarkBlue) {
            activity.setTheme(R.style.Theme_SeriesGuide_DarkBlue_Immersive);
        } else {
            activity.setTheme(R.style.Theme_SeriesGuide_Immersive);
        }
    }
}
