package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.enums.EpisodeFlags;

public class EpisodeTools {

    public static boolean isWatched(int episodeFlags) {
        return (episodeFlags & EpisodeFlags.WATCHED) != 0;
    }

}
