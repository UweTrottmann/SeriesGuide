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
