package com.battlelancer.seriesguide.util;

import android.content.Context;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.SeasonTags;

public class SeasonTools {

    public static boolean hasSkippedTag(String tags) {
        return SeasonTags.SKIPPED.equals(tags);
    }

    /**
     * Builds a localized string like "Season 5" or if the number is 0 "Special Episodes".
     */
    public static String getSeasonString(Context context, int seasonNumber) {
        if (seasonNumber == 0) {
            return context.getString(R.string.specialseason);
        } else {
            return context.getString(R.string.season_number, seasonNumber);
        }
    }

}
