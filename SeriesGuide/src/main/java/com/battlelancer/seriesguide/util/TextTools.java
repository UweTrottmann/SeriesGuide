package com.battlelancer.seriesguide.util;

import android.content.Context;

/**
 * Tools to help build text fragments to be used throughout the user interface.
 */
public class TextTools {

    private TextTools() {
        // prevent instantiation
    }

    /**
     * Returns a string like "Show Title 1x01". The number format may change based on user
     * preference.
     */
    public static String getShowWithEpisodeNumber(Context context, String title, int season,
            int episode) {
        String number = Utils.getEpisodeNumber(context, season, episode);
        title += " " + number;
        return title;
    }

}
