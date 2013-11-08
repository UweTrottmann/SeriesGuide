package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.enums.SeasonTags;

public class SeasonTools {

    public static boolean hasSkippedTag(String tags) {
        return SeasonTags.SKIPPED.equals(tags);
    }

}
