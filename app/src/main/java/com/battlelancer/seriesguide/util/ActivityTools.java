package com.battlelancer.seriesguide.util;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.model.SgActivity;
import com.battlelancer.seriesguide.provider.SgActivityHelper;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.ui.shows.ShowTools;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    public static void addActivity(Context context, int episodeTvdbId, int showTvdbId) {
        long timeMonthAgo = System.currentTimeMillis() - HISTORY_THRESHOLD;
        SgActivityHelper helper = SgRoomDatabase.getInstance(context).sgActivityHelper();

        // delete all entries older than 30 days
        int deleted = helper.deleteOldActivity(timeMonthAgo);
        Timber.d("addActivity: removed %d outdated activities", deleted);

        // add new entry
        long currentTime = System.currentTimeMillis();
        SgActivity activity = new SgActivity(null,
                String.valueOf(episodeTvdbId),
                String.valueOf(showTvdbId),
                currentTime);
        helper.insertActivity(activity);
        Timber.d("addActivity: episode: %d timestamp: %d", episodeTvdbId, currentTime);
    }

    /**
     * Tries to remove any activity with the given episode TheTVDB id.
     */
    public static void removeActivity(Context context, int episodeTvdbId) {
        int deleted = SgRoomDatabase.getInstance(context).sgActivityHelper()
                .deleteActivity(String.valueOf(episodeTvdbId));
        Timber.d("removeActivity: deleted %d activity entries", deleted);
    }

    /**
     * Get latest activity for each show and update last watched time if newer.
     */
    public static void populateShowsLastWatchedTime(Context context) {
        List<SgActivity> activities = SgRoomDatabase.getInstance(context).sgActivityHelper()
                .getActivityByLatest();

        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        HashSet<Integer> handledShows = new HashSet<>();
        for (SgActivity activity : activities) {
            int showTvdbId = Integer.parseInt(activity.getShowTvdbOrTmdbId());
            if (!handledShows.contains(showTvdbId)) {
                handledShows.add(showTvdbId);
                ShowTools.addLastWatchedUpdateOpIfNewer(context, batch, showTvdbId,
                        activity.getTimestampMs());
            }
        }
    }
}
