package com.battlelancer.seriesguide.sync;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.format.DateUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.provider.SgShow2Helper;
import com.battlelancer.seriesguide.provider.SgShow2UpdateInfo;
import com.battlelancer.seriesguide.sync.SyncOptions.SyncType;
import com.battlelancer.seriesguide.ui.shows.ShowTools;
import com.battlelancer.seriesguide.ui.shows.ShowTools2.ShowResult;
import com.battlelancer.seriesguide.util.TimeTools;
import com.uwetrottmann.androidutils.AndroidUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import timber.log.Timber;

/**
 * Updates show data from the show data source.
 * If updating a single show, should supply its row ID.
 */
public class ShowSync {

    // Values based on the assumption that sync runs about every 24 hours
    private static final long UPDATE_THRESHOLD_WEEKLYS_MS = 6 * DateUtils.DAY_IN_MILLIS +
            12 * DateUtils.HOUR_IN_MILLIS;
    private static final long UPDATE_THRESHOLD_DAILYS_MS = DateUtils.DAY_IN_MILLIS
            + 12 * DateUtils.HOUR_IN_MILLIS;

    private final SyncType syncType;
    private final long singleShowId;
    private boolean hasUpdatedShows;

    public ShowSync(SyncType syncType, long singleShowId) {
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
            ShowTools showTools,
            long currentTime,
            SyncProgress progress
    ) {
        hasUpdatedShows = false;

        List<Long> showsToUpdate = getShowsToUpdate(context, currentTime);
        if (showsToUpdate == null) {
            return null;
        }
        Timber.d("Updating %d show(s)...", showsToUpdate.size());

        // from here on we need more sophisticated abort handling, so keep track of errors
        SgSyncAdapter.UpdateResult resultCode = SgSyncAdapter.UpdateResult.SUCCESS;

        // loop through shows and download latest data from TVDb
        int consecutiveTimeouts = 0;
        for (Long showId : showsToUpdate) {
            // stop sync if connectivity is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                resultCode = SgSyncAdapter.UpdateResult.INCOMPLETE;
                break;
            }

            ShowResult result = showTools.updateShow(showId);
            if (result == ShowResult.SUCCESS) {
                hasUpdatedShows = true;
            } else {
                // failed, continue with other shows
                resultCode = SgSyncAdapter.UpdateResult.INCOMPLETE;

                SgShow2Helper helper = SgRoomDatabase.getInstance(context).sgShow2Helper();
                String showTitle = helper.getShowTitle(showId);
                Integer showTmdbId = helper.getShowTmdbId(showId);
                String message = String
                        .format("Failed to update show ('%s', TMDB id %s).", showTitle, showTmdbId);
                if (result == ShowResult.DOES_NOT_EXIST) {
                    message += " It no longer exists.";
                }
                progress.setImportantErrorIfNone(message);
                Timber.e(message);

                // Stop updating after multiple consecutive timeouts (around 3 * 15/20 seconds)
                if (result == ShowResult.TIMEOUT_ERROR) {
                    consecutiveTimeouts++;
                } else if (consecutiveTimeouts > 0) {
                    consecutiveTimeouts--;
                }
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
    private List<Long> getShowsToUpdate(Context context, long currentTime) {
        switch (syncType) {
            case SINGLE: {
                long showId = singleShowId;
                if (showId == 0) {
                    Timber.e("Syncing...ABORT_INVALID_SHOW_TVDB_ID");
                    return null;
                }
                return Collections.singletonList(showId);
            }
            case FULL: {
                // get all show IDs for a full update
                return SgRoomDatabase.getInstance(context).sgShow2Helper().getShowIdsLong();
            }
            case DELTA:
                return getShowsToDeltaUpdate(context, currentTime);
            default:
                throw new IllegalArgumentException("Sync type " + syncType + " is not supported.");
        }
    }

    /**
     * Return list of show IDs that have not been updated for a certain time.
     */
    @NonNull
    private List<Long> getShowsToDeltaUpdate(Context context, long currentTime) {
        // get existing show ids
        List<SgShow2UpdateInfo> shows = SgRoomDatabase.getInstance(context)
                .sgShow2Helper().getShowsUpdateInfo();

        final List<Long> updatableShowIds = new ArrayList<>();
        for (SgShow2UpdateInfo show : shows) {
            boolean isDailyShow = show.getReleaseWeekDay() == TimeTools.RELEASE_WEEKDAY_DAILY;
            long lastUpdatedTime = show.getLastUpdatedMs();
            // update daily shows more frequently than weekly shows
            if (currentTime - lastUpdatedTime >
                    (isDailyShow ? UPDATE_THRESHOLD_DAILYS_MS : UPDATE_THRESHOLD_WEEKLYS_MS)) {
                // add shows that are due for updating
                updatableShowIds.add(show.getId());
            }
        }

        return updatableShowIds;
    }

    public boolean isSyncMultiple() {
        return syncType == SyncType.DELTA || syncType == SyncType.FULL;
    }

    public boolean hasUpdatedShows() {
        return hasUpdatedShows;
    }
}
