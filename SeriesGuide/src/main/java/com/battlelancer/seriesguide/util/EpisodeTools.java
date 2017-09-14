package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.jobs.episodes.EpisodeCollectedJob;
import com.battlelancer.seriesguide.jobs.episodes.EpisodeFlagJob;
import com.battlelancer.seriesguide.jobs.episodes.EpisodeJobAsyncTask;
import com.battlelancer.seriesguide.jobs.episodes.EpisodeWatchedJob;
import com.battlelancer.seriesguide.jobs.episodes.EpisodeWatchedPreviousJob;
import com.battlelancer.seriesguide.jobs.episodes.SeasonCollectedJob;
import com.battlelancer.seriesguide.jobs.episodes.SeasonWatchedJob;
import com.battlelancer.seriesguide.jobs.episodes.ShowCollectedJob;
import com.battlelancer.seriesguide.jobs.episodes.ShowWatchedJob;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;

public class EpisodeTools {

    /**
     * Checks the database whether there is an entry for this episode.
     */
    public static boolean isEpisodeExists(Context context, int episodeTvdbId) {
        Cursor query = context.getContentResolver().query(
                SeriesGuideContract.Episodes.buildEpisodeUri(episodeTvdbId), new String[] {
                        SeriesGuideContract.Episodes._ID }, null, null, null
        );
        if (query == null) {
            return false;
        }

        boolean isExists = query.getCount() > 0;
        query.close();

        return isExists;
    }

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

    public static void validateFlags(int episodeFlags) {
        if (isUnwatched(episodeFlags)) {
            return;
        }
        if (isSkipped(episodeFlags)) {
            return;
        }
        if (isWatched(episodeFlags)) {
            return;
        }

        throw new IllegalArgumentException(
                "Did not pass a valid episode flag. See EpisodeFlags class for details.");
    }

    public static void episodeWatched(Context context, int showTvdbId, int episodeTvdbId,
            int season, int episode, int episodeFlags) {
        validateFlags(episodeFlags);
        execute(context, new EpisodeWatchedJob(showTvdbId, episodeTvdbId, season, episode,
                episodeFlags));
    }

    public static void episodeCollected(Context context, int showTvdbId, int episodeTvdbId,
            int season, int episode, boolean isCollected) {
        execute(context, new EpisodeCollectedJob(showTvdbId, episodeTvdbId, season, episode,
                isCollected));
    }

    /**
     * Flags all episodes released previous to this one as watched (excluding episodes with no
     * release date).
     */
    public static void episodeWatchedPrevious(Context context, int showTvdbId,
            long episodeFirstAired) {
        execute(context, new EpisodeWatchedPreviousJob(showTvdbId, episodeFirstAired));
    }

    public static void seasonWatched(Context context, int showTvdbId, int seasonTvdbId, int season,
            int episodeFlags) {
        validateFlags(episodeFlags);
        execute(context, new SeasonWatchedJob(showTvdbId, seasonTvdbId, season,
                episodeFlags, TimeTools.getCurrentTime(context)));
    }

    public static void seasonCollected(Context context, int showTvdbId, int seasonTvdbId,
            int season, boolean isCollected) {
        execute(context,
                new SeasonCollectedJob(showTvdbId, seasonTvdbId, season, isCollected));
    }

    public static void showWatched(Context context, int showTvdbId, boolean isFlag) {
        execute(context,
                new ShowWatchedJob(showTvdbId, isFlag ? 1 : 0, TimeTools.getCurrentTime(context)));
    }

    public static void showCollected(Context context, int showTvdbId, boolean isCollected) {
        execute(context, new ShowCollectedJob(showTvdbId, isCollected));
    }

    /**
     * Run the task on the thread pool.
     */
    private static void execute(Context context, @NonNull EpisodeFlagJob job) {
        new EpisodeJobAsyncTask(context, job).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
