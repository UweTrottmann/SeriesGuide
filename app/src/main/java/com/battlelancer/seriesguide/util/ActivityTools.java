package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.model.SgActivity;
import com.battlelancer.seriesguide.provider.SgActivityHelper;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import timber.log.Timber;

/**
 * Helper methods for adding or removing local episode watch activity.
 */
public class ActivityTools {

    private static final long HISTORY_THRESHOLD = 30 * DateUtils.DAY_IN_MILLIS;

    /**
     * Adds an activity entry for the given episode with the current time as timestamp. If an entry
     * already exists it is replaced.
     *
     * <p>Also cleans up old entries.
     */
    public static void addActivity(Context context, long episodeId, long showId) {
        // Need to use global IDs (in case a show is removed and added again).
        SgRoomDatabase database = SgRoomDatabase.getInstance(context);
        int showTvdbIdOrZero = database.sgShow2Helper().getShowTvdbId(showId);
        if (showTvdbIdOrZero == 0) return;
        int episodeTvdbIdOrZero = database.sgEpisode2Helper().getEpisodeTvdbId(episodeId);
        if (episodeTvdbIdOrZero == 0) return;

        long timeMonthAgo = System.currentTimeMillis() - HISTORY_THRESHOLD;
        SgActivityHelper helper = database.sgActivityHelper();

        // delete all entries older than 30 days
        int deleted = helper.deleteOldActivity(timeMonthAgo);
        Timber.d("addActivity: removed %d outdated activities", deleted);

        // add new entry
        long currentTime = System.currentTimeMillis();
        SgActivity activity = new SgActivity(null,
                String.valueOf(episodeTvdbIdOrZero),
                String.valueOf(showTvdbIdOrZero),
                currentTime);
        helper.insertActivity(activity);
        Timber.d("addActivity: episode: %d timestamp: %d", episodeId, currentTime);
    }

    /**
     * Tries to remove any activity with the given episode id.
     */
    public static void removeActivity(Context context, long episodeId) {
        // Need to use global IDs (in case a show is removed and added again).
        SgRoomDatabase database = SgRoomDatabase.getInstance(context);
        int episodeTvdbIdOrZero = database.sgEpisode2Helper().getEpisodeTvdbId(episodeId);
        if (episodeTvdbIdOrZero == 0) return;
        int deleted = SgRoomDatabase.getInstance(context).sgActivityHelper()
                .deleteActivity(String.valueOf(episodeTvdbIdOrZero));
        Timber.d("removeActivity: deleted %d activity entries", deleted);
    }

}
