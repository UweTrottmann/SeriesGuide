package com.battlelancer.seriesguide.util;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.AttrRes;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
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
        int theme = Integer.parseInt(themeIndex);
        // The Light theme is a DayNight theme that can be toggled between light and dark.
        SeriesGuidePreferences.THEME = R.style.Theme_SeriesGuide_Light;
        switch (theme) {
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case 2:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
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

    public static int getColorFromAttribute(Context context, @AttrRes int attribute) {
        return ContextCompat.getColor(
                context,
                Utils.resolveAttributeToResourceId(context.getTheme(), attribute)
        );
    }
}
