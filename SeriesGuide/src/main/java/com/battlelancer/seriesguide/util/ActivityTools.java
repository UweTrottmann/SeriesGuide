package com.battlelancer.seriesguide.util;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Activity;
import java.util.ArrayList;
import java.util.HashSet;
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
                .delete(Activity.CONTENT_URI,
                        Activity.TIMESTAMP_MS + "<" + timeMonthAgo, null);
        Timber.d("addActivity: removed %d outdated activities", deleted);

        // add new entry
        ContentValues values = new ContentValues();
        values.put(Activity.EPISODE_TVDB_ID, episodeTvdbId);
        values.put(Activity.SHOW_TVDB_ID, showTvdbId);
        long currentTime = System.currentTimeMillis();
        values.put(Activity.TIMESTAMP_MS, currentTime);

        context.getContentResolver().insert(Activity.CONTENT_URI, values);
        Timber.d("addActivity: episode: %d timestamp: %d", episodeTvdbId, currentTime);
    }

    /**
     * Tries to remove any activity with the given episode TheTVDB id.
     */
    public static void removeActivity(Context context, int episodeTvdbId) {
        int deleted = context.getContentResolver().delete(Activity.CONTENT_URI,
                Activity.EPISODE_TVDB_ID + "=" + episodeTvdbId, null);
        Timber.d("removeActivity: deleted %d activity entries", deleted);
    }

    /**
     * Get latest activity for each show and update last watched time if newer.
     */
    public static void populateShowsLastWatchedTime(Context context) {
        Cursor query = context.getContentResolver()
                .query(Activity.CONTENT_URI,
                        new String[] { Activity.TIMESTAMP_MS, Activity.SHOW_TVDB_ID },
                        null, null,
                        Activity.SORT_LATEST);
        if (query == null) {
            Timber.e("populateShowsLastWatchedTime: query is null.");
            return;
        }

        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        HashSet<Integer> handledShows = new HashSet<>();
        while (query.moveToNext()) {
            int showTvdbId = query.getInt(1);
            if (!handledShows.contains(showTvdbId)) {
                handledShows.add(showTvdbId);
                long lastWatchedMs = query.getLong(0);
                ShowTools.addLastWatchedUpdateOpIfNewer(context, batch, showTvdbId, lastWatchedMs);
            }
        }
        query.close();
    }
}
