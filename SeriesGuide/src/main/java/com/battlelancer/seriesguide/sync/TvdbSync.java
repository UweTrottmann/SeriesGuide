package com.battlelancer.seriesguide.sync;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.AppSettings;
import com.battlelancer.seriesguide.thetvdbapi.TvdbException;
import com.battlelancer.seriesguide.thetvdbapi.TvdbTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import dagger.Lazy;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import timber.log.Timber;

public class TvdbSync {

    // Values based on the assumption that sync runs about every 24 hours
    private static final long UPDATE_THRESHOLD_WEEKLYS_MS = 6 * DateUtils.DAY_IN_MILLIS +
            12 * DateUtils.HOUR_IN_MILLIS;
    private static final long UPDATE_THRESHOLD_DAILYS_MS = DateUtils.DAY_IN_MILLIS
            + 12 * DateUtils.HOUR_IN_MILLIS;

    private final SyncType syncType;
    private final int singleShowTvdbId;
    private boolean hasUpdatedShows;

    public TvdbSync(Bundle extras) {
        syncType = SyncType.from(extras.getInt(SgSyncAdapter.EXTRA_SYNC_TYPE, SyncType.DELTA.id));
        singleShowTvdbId = extras.getInt(SgSyncAdapter.EXTRA_SYNC_SHOW_TVDB_ID, 0);
    }

    /**
     * Update shows based on the sync type.
     */
    @Nullable
    public SgSyncAdapter.UpdateResult sync(Context context, Lazy<TvdbTools> tvdbTools,
            long currentTime) {
        hasUpdatedShows = false;

        int[] showsToUpdate = getShowsToUpdate(context, currentTime);
        if (showsToUpdate == null) {
            return null;
        }

        // from here on we need more sophisticated abort handling, so keep track of errors
        SgSyncAdapter.UpdateResult resultCode = SgSyncAdapter.UpdateResult.SUCCESS;

        // loop through shows and download latest data from TVDb
        final ContentResolver resolver = context.getContentResolver();
        int consecutiveTimeouts = 0;
        for (int i = 0; i < showsToUpdate.length; i++) {
            int showTvdbId = showsToUpdate[i];

            // stop sync if connectivity is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                resultCode = SgSyncAdapter.UpdateResult.INCOMPLETE;
                break;
            }

            try {
                tvdbTools.get().updateShow(showTvdbId);
                hasUpdatedShows = true;

                // make sure other loaders (activity, overview, details) are notified
                resolver.notifyChange(SeriesGuideContract.Episodes.CONTENT_URI_WITHSHOW, null);
            } catch (TvdbException e) {
                // failed, continue with other shows
                resultCode = SgSyncAdapter.UpdateResult.INCOMPLETE;
                Timber.e(e, "Updating show failed");
                Throwable cause = e.getCause();
                if (cause != null && cause instanceof SocketTimeoutException) {
                    consecutiveTimeouts++;
                } else if (consecutiveTimeouts > 0) {
                    consecutiveTimeouts--;
                }
                // skip after multiple consecutive timeouts (around 3 * 15/20 seconds)
                if (consecutiveTimeouts == 3) {
                    Timber.e("Connection unstable, give up.");
                    return resultCode;
                }
            }
        }

        return resultCode;
    }

    /**
     * Returns an array of show ids to update.
     */
    @Nullable
    private int[] getShowsToUpdate(Context context, long currentTime) {
        switch (syncType) {
            case SINGLE: {
                int showTvdbId = singleShowTvdbId;
                if (showTvdbId == 0) {
                    Timber.e("Syncing...ABORT_INVALID_SHOW_TVDB_ID");
                    return null;
                }
                return new int[] { showTvdbId };
            }
            case FULL: {
                // get all show IDs for a full update
                final Cursor showsQuery = context.getContentResolver().query(
                        SeriesGuideContract.Shows.CONTENT_URI,
                        new String[] {
                                SeriesGuideContract.Shows._ID
                        }, null, null, null
                );
                if (showsQuery == null) {
                    Timber.e("Syncing...ABORT_SHOW_QUERY_FAILED");
                    return null;
                }

                int[] showIds = new int[showsQuery.getCount()];
                int i = 0;
                while (showsQuery.moveToNext()) {
                    showIds[i] = showsQuery.getInt(0);
                    i++;
                }
                showsQuery.close();
                return showIds;
            }
            case DELTA:
            default:
                return getShowsToDeltaUpdate(context, currentTime);
        }
    }

    /**
     * Return list of show TVDb ids that have not been updated for a certain time.
     */
    @Nullable
    private int[] getShowsToDeltaUpdate(Context context, long currentTime) {
        // get existing show ids
        final Cursor shows = context.getContentResolver()
                .query(SeriesGuideContract.Shows.CONTENT_URI, new String[] {
                        SeriesGuideContract.Shows._ID, SeriesGuideContract.Shows.LASTUPDATED,
                        SeriesGuideContract.Shows.RELEASE_WEEKDAY
                }, null, null, null);
        if (shows == null) {
            Timber.e("Syncing...ABORT_SHOW_QUERY_FAILED");
            return null;
        }

        final List<Integer> updatableShowIds = new ArrayList<>();
        while (shows.moveToNext()) {
            boolean isDailyShow = shows.getInt(2) == TimeTools.RELEASE_WEEKDAY_DAILY;
            long lastUpdatedTime = shows.getLong(1);
            // update daily shows more frequently than weekly shows
            if (currentTime - lastUpdatedTime >
                    (isDailyShow ? UPDATE_THRESHOLD_DAILYS_MS : UPDATE_THRESHOLD_WEEKLYS_MS)) {
                // add shows that are due for updating
                updatableShowIds.add(shows.getInt(0));
            }
        }

        int showCount = shows.getCount();
        if (showCount > 0 && AppSettings.shouldReportStats(context)) {
            Utils.trackCustomEvent(context, "Statistics", "Shows", String.valueOf(showCount));
        }

        shows.close();

        // copy to int array
        int[] showTvdbIds = new int[updatableShowIds.size()];
        for (int i = 0; i < updatableShowIds.size(); i++) {
            showTvdbIds[i] = updatableShowIds.get(i);
        }
        return showTvdbIds;
    }

    public SyncType syncType() {
        return syncType;
    }

    public boolean isSyncMultiple() {
        return syncType != SyncType.SINGLE;
    }

    public boolean hasUpdatedShows() {
        return hasUpdatedShows;
    }

    public enum SyncType {
        DELTA(0),
        SINGLE(1),
        FULL(2);

        public int id;

        SyncType(int id) {
            this.id = id;
        }

        public static SyncType from(int id) {
            return values()[id];
        }
    }
}
