package com.battlelancer.seriesguide.ui.episodes;

import android.content.Context;
import com.battlelancer.seriesguide.jobs.FlagJobExecutor;
import com.battlelancer.seriesguide.jobs.episodes.EpisodeCollectedJob;
import com.battlelancer.seriesguide.jobs.episodes.EpisodeWatchedJob;
import com.battlelancer.seriesguide.jobs.episodes.EpisodeWatchedUpToJob;
import com.battlelancer.seriesguide.jobs.episodes.SeasonCollectedJob;
import com.battlelancer.seriesguide.jobs.episodes.SeasonWatchedJob;
import com.battlelancer.seriesguide.jobs.episodes.ShowCollectedJob;
import com.battlelancer.seriesguide.jobs.episodes.ShowWatchedJob;
import com.battlelancer.seriesguide.util.TimeTools;

public class EpisodeTools {

    public static boolean isCollected(int collectedFlag) {
        return collectedFlag == 1;
    }

    public static boolean isSkipped(int episodeFlags) {
        return episodeFlags == EpisodeFlags.SKIPPED;
    }

    public static boolean isUnwatched(int episodeFlags) {
        return episodeFlags == EpisodeFlags.UNWATCHED;
    }

    public static boolean isWatched(int episodeFlags) {
        return episodeFlags == EpisodeFlags.WATCHED;
    }

    public static boolean isValidEpisodeFlag(int episodeFlags) {
        return isUnwatched(episodeFlags) || isSkipped(episodeFlags) || isWatched(episodeFlags);
    }

    public static void validateFlags(int episodeFlags) {
        if (!isValidEpisodeFlag(episodeFlags)) {
            throw new IllegalArgumentException(
                    "Did not pass a valid episode flag. See EpisodeFlags class for details.");
        }
    }

    public static void episodeWatched(Context context, long episodeId, int episodeFlags) {
        validateFlags(episodeFlags);
        FlagJobExecutor.execute(context, new EpisodeWatchedJob(episodeId, episodeFlags));
    }

    public static void episodeWatchedIfNotZero(Context context, long episodeIdOrZero) {
        if (episodeIdOrZero > 0) {
            episodeWatched(context, episodeIdOrZero, EpisodeFlags.WATCHED);
        }
    }

    public static void episodeCollected(Context context, long episodeId, boolean isCollected) {
        FlagJobExecutor.execute(context, new EpisodeCollectedJob(episodeId, isCollected));
    }

    /**
     * See {@link EpisodeWatchedUpToJob}.
     */
    public static void episodeWatchedUpTo(Context context, long showId,
            long episodeFirstAired, int episodeNumber) {
        FlagJobExecutor.execute(context,
                new EpisodeWatchedUpToJob(showId, episodeFirstAired, episodeNumber));
    }

    public static void seasonWatched(Context context, long seasonId, int episodeFlags) {
        validateFlags(episodeFlags);
        FlagJobExecutor.execute(context,
                new SeasonWatchedJob(seasonId, episodeFlags, TimeTools.getCurrentTime(context)));
    }

    public static void seasonCollected(Context context, long seasonId, boolean isCollected) {
        FlagJobExecutor.execute(context, new SeasonCollectedJob(seasonId, isCollected));
    }

    public static void showWatched(Context context, long showId, boolean isFlag) {
        FlagJobExecutor.execute(context,
                new ShowWatchedJob(showId, isFlag ? 1 : 0, TimeTools.getCurrentTime(context)));
    }

    public static void showCollected(Context context, long showId, boolean isCollected) {
        FlagJobExecutor.execute(context, new ShowCollectedJob(showId, isCollected));
    }
}
