package com.battlelancer.seriesguide.sync;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.model.SgShowTitleAndTvdbId;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.sync.SyncOptions.SyncType;
import com.battlelancer.seriesguide.thetvdbapi.TvdbException;
import com.battlelancer.seriesguide.thetvdbapi.TvdbTools;
import com.battlelancer.seriesguide.util.TimeTools;
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
    private final long singleShowId;
    private boolean hasUpdatedShows;

    public TvdbSync(SyncType syncType, long singleShowId) {
        this.syncType = syncType;
        this.singleShowId = singleShowId;
    }

    /**
     * Update shows based on the sync type.
     */
    @SuppressLint("TimberExceptionLogging")
    @Nullable
    public SgSyncAdapter.UpdateResult sync(
            Context context,
            ContentResolver resolver,
            Lazy<TvdbTools> tvdbTools,
            long currentTime,
            SyncProgress progress
    ) {
        hasUpdatedShows = false;

        long[] showIdsToUpdate = getShowsToUpdate(resolver, currentTime);
        if (showIdsToUpdate == null) {
            return null;
        }
        Timber.d("Updating %d show(s)...", showIdsToUpdate.length);

        // from here on we need more sophisticated abort handling, so keep track of errors
        SgSyncAdapter.UpdateResult resultCode = SgSyncAdapter.UpdateResult.SUCCESS;

        // loop through shows and download latest data from TVDb
        int consecutiveTimeouts = 0;
        for (int i = 0; i < showIdsToUpdate.length; i++) {
            long showId = showIdsToUpdate[i];

            // stop sync if connectivity is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                resultCode = SgSyncAdapter.UpdateResult.INCOMPLETE;
                break;
            }

            try {
                tvdbTools.get().updateShow(showId);
                hasUpdatedShows = true;

                // make sure other loaders (activity, overview, details) are notified
                resolver.notifyChange(SeriesGuideContract.Episodes.CONTENT_URI_WITHSHOW, null);
            } catch (TvdbException e) {
                // failed, continue with other shows
                resultCode = SgSyncAdapter.UpdateResult.INCOMPLETE;

                SgShowTitleAndTvdbId titleAndTvdbId = SgRoomDatabase.getInstance(context)
                        .showHelper()
                        .getShowTitleAndTvdbId(showId);
                String showTitle = titleAndTvdbId != null ? titleAndTvdbId.getTitle() : "?";
                int showTvdbId = titleAndTvdbId != null ? titleAndTvdbId.getTvdbId() : -1;
                String message = String
                        .format("Failed to update show ('%s', TVDB id %s).", showTitle, showTvdbId);
                if (e.itemDoesNotExist()) {
                    message += " It no longer exists.";
                }
                progress.setImportantErrorIfNone(message);
                Timber.e(e, message);

                Throwable cause = e.getCause();
                if (cause instanceof SocketTimeoutException) {
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
     * Returns an array of show row ids to update.
     */
    @Nullable
    private long[] getShowsToUpdate(ContentResolver resolver, long currentTime) {
        switch (syncType) {
            case SINGLE: {
                long showId = singleShowId;
                if (showId == 0) {
                    Timber.e("Syncing...ABORT_INVALID_SHOW_TVDB_ID");
                    return null;
                }
                return new long[]{showId};
            }
            case FULL: {
                // get all show IDs for a full update
                final Cursor showsQuery = resolver.query(
                        Shows.CONTENT_URI,
                        new String[]{Shows._ID},
                        null, null, null
                );
                if (showsQuery == null) {
                    Timber.e("Syncing...ABORT_SHOW_QUERY_FAILED");
                    return null;
                }

                long[] showIds = new long[showsQuery.getCount()];
                int i = 0;
                while (showsQuery.moveToNext()) {
                    showIds[i] = showsQuery.getLong(0);
                    i++;
                }
                showsQuery.close();
                return showIds;
            }
            case DELTA:
                return getShowsToDeltaUpdate(resolver, currentTime);
            default:
                throw new IllegalArgumentException("Sync type " + syncType + " is not supported.");
        }
    }

    /**
     * Return list of show TVDb ids that have not been updated for a certain time.
     */
    @Nullable
    private long[] getShowsToDeltaUpdate(ContentResolver resolver, long currentTime) {
        // get existing show ids
        final Cursor shows = resolver
                .query(Shows.CONTENT_URI,
                        new String[]{Shows._ID, Shows.LASTUPDATED, Shows.RELEASE_WEEKDAY},
                        null, null, null);
        if (shows == null) {
            Timber.e("Syncing...ABORT_SHOW_QUERY_FAILED");
            return null;
        }

        final List<Long> updatableShowIds = new ArrayList<>();
        while (shows.moveToNext()) {
            boolean isDailyShow = shows.getInt(2) == TimeTools.RELEASE_WEEKDAY_DAILY;
            long lastUpdatedTime = shows.getLong(1);
            // update daily shows more frequently than weekly shows
            if (currentTime - lastUpdatedTime >
                    (isDailyShow ? UPDATE_THRESHOLD_DAILYS_MS : UPDATE_THRESHOLD_WEEKLYS_MS)) {
                // add shows that are due for updating
                updatableShowIds.add(shows.getLong(0));
            }
        }

        shows.close();

        // copy to int array
        long[] showIds = new long[updatableShowIds.size()];
        for (int i = 0; i < updatableShowIds.size(); i++) {
            showIds[i] = updatableShowIds.get(i);
        }
        return showIds;
    }

    public boolean isSyncMultiple() {
        return syncType == SyncType.DELTA || syncType == SyncType.FULL;
    }

    public boolean hasUpdatedShows() {
        return hasUpdatedShows;
    }
}
