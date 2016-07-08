package com.battlelancer.seriesguide.util;

import android.content.ContentValues;
import android.content.Context;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import timber.log.Timber;

/**
 * Helper methods for dealing with the {@link com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables#ACTIVITY}
 * table.
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

        // delete all entries older than 30 days
        int deleted = context.getContentResolver()
                .delete(SeriesGuideContract.Activity.CONTENT_URI,
                        SeriesGuideContract.Activity.TIMESTAMP + "<" + timeMonthAgo, null);
        Timber.d("addActivity: removed " + deleted + " outdated activities");

        // add new entry
        ContentValues values = new ContentValues();
        values.put(SeriesGuideContract.Activity.EPISODE_TVDB_ID, episodeTvdbId);
        values.put(SeriesGuideContract.Activity.SHOW_TVDB_ID, showTvdbId);
        long currentTime = System.currentTimeMillis();
        values.put(SeriesGuideContract.Activity.TIMESTAMP, currentTime);

        context.getContentResolver().insert(SeriesGuideContract.Activity.CONTENT_URI, values);
        Timber.d("addActivity: episode: " + episodeTvdbId + " timestamp: " + currentTime);
    }

    /**
     * Tries to remove any activity with the given episode TheTVDB id.
     */
    public static void removeActivity(Context context, int episodeTvdbId) {
        int deleted = context.getContentResolver().delete(SeriesGuideContract.Activity.CONTENT_URI,
                SeriesGuideContract.Activity.EPISODE_TVDB_ID + "=" + episodeTvdbId, null);
        Timber.d("removeActivity: deleted " + deleted + " activity entries");
    }
}
