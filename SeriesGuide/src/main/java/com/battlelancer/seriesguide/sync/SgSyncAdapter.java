
/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.sync;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SeriesGuideApplication;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.TmdbSettings;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.settings.UpdateSettings;
import com.battlelancer.seriesguide.thetvdbapi.TheTVDB;
import com.battlelancer.seriesguide.thetvdbapi.TvdbException;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.MovieTools;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.TraktTools;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.tmdb.entities.Configuration;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.LastActivities;
import com.uwetrottmann.trakt.v2.entities.LastActivityMore;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * {@link AbstractThreadedSyncAdapter} which updates the show library.
 */
public class SgSyncAdapter extends AbstractThreadedSyncAdapter {

    public enum SyncType {

        DELTA(0),
        SINGLE(1),
        FULL(2);

        public int id;

        private SyncType(int id) {
            this.id = id;
        }

        public static SyncType from(int id) {
            return values()[id];
        }
    }

    public interface SyncInitBundle {

        /**
         * One of {@link com.battlelancer.seriesguide.sync.SgSyncAdapter.SyncType}.
         */
        public String SYNC_TYPE = "com.battlelancer.seriesguide.sync_type";

        /**
         * If {@link #SYNC_TYPE} is {@link SyncType#SINGLE}, the TVDb id of the show to sync.
         */
        public String SYNC_SHOW_TVDB_ID = "com.battlelancer.seriesguide.sync_show";

        /**
         * Whether the sync should occur despite time or backoff limits.
         */
        public String SYNC_IMMEDIATE = "com.battlelancer.seriesguide.sync_immediate";
    }

    private static final int DEFAULT_SYNC_INTERVAL_MINUTES = 20;

    /**
     * Calls {@link ContentResolver} {@code .requestSyncIfConnected()} if there is no pending sync
     * already.
     */
    public static void requestSyncIfTime(Context context) {
        // guard against scheduling too many sync requests
        Account account = AccountUtils.getAccount(context);
        if (account == null ||
                ContentResolver.isSyncPending(account, SeriesGuideApplication.CONTENT_AUTHORITY)) {
            return;
        }

        if (!isTimeForSync(context, System.currentTimeMillis())) {
            return;
        }

        SgSyncAdapter.requestSyncIfConnected(context, SyncType.DELTA, 0);
    }

    /**
     * Schedules a sync for a single show if {@link com.battlelancer.seriesguide.thetvdbapi.TheTVDB#isUpdateShow(android.content.Context,
     * int)} returns true.
     *
     * <p> <em>Note: Runs a content provider op, so you should do this on a background thread.</em>
     */
    public static void requestSyncIfTime(Context context, int showTvdbId) {
        if (TheTVDB.isUpdateShow(context, showTvdbId)) {
            SgSyncAdapter.requestSyncIfConnected(context, SyncType.SINGLE, showTvdbId);
        }
    }

    /**
     * Schedules a sync. Will only queue a sync request if there is a network connection and
     * auto-sync is enabled.
     *
     * @param syncType Any of {@link SyncType}.
     * @param showTvdbId If using {@link SyncType#SINGLE}, the TVDb id of a show.
     */
    public static void requestSyncIfConnected(Context context, SyncType syncType, int showTvdbId) {
        if (!AndroidUtils.isNetworkConnected(context) || !isSyncAutomatically(context)) {
            // offline or auto-sync disabled: abort
            return;
        }

        Bundle args = new Bundle();
        args.putInt(SyncInitBundle.SYNC_TYPE, syncType.id);
        args.putInt(SyncInitBundle.SYNC_SHOW_TVDB_ID, showTvdbId);

        requestSync(context, args);
    }

    /**
     * Schedules an immediate sync even if auto-sync is disabled, it runs as soon as there is a
     * connection.
     *
     * @param syncType Any of {@link SyncType}.
     * @param showTvdbId If using {@link SyncType#SINGLE}, the TVDb id of a show.
     * @param showStatusToast If set, shows a status toast and aborts if offline.
     */
    public static void requestSyncImmediate(Context context, SyncType syncType, int showTvdbId,
            boolean showStatusToast) {
        if (showStatusToast) {
            if (!AndroidUtils.isNetworkConnected(context)) {
                // offline: notify and abort
                Toast.makeText(context, R.string.update_no_connection, Toast.LENGTH_LONG).show();
                return;
            }
            // notify about upcoming sync
            Toast.makeText(context, R.string.update_scheduled, Toast.LENGTH_SHORT).show();
        }

        Bundle args = new Bundle();
        args.putBoolean(SyncInitBundle.SYNC_IMMEDIATE, true);
        args.putInt(SyncInitBundle.SYNC_TYPE, syncType.id);
        args.putInt(SyncInitBundle.SYNC_SHOW_TVDB_ID, showTvdbId);

        // ignore sync settings and backoff
        args.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        // push to front of sync queue
        args.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

        requestSync(context, args);
    }

    /**
     * Schedules a sync with the given arguments.
     */
    private static void requestSync(Context context, Bundle args) {
        Account account = AccountUtils.getAccount(context);
        if (account == null) {
            return;
        }
        ContentResolver.requestSync(account,
                SeriesGuideApplication.CONTENT_AUTHORITY, args);
    }

    /**
     * Set whether or not the provider is synced when it receives a network tickle.
     */
    public static void setSyncAutomatically(Context context, boolean sync) {
        Account account = AccountUtils.getAccount(context);
        if (account == null) {
            return;
        }
        ContentResolver.setSyncAutomatically(account, SeriesGuideApplication.CONTENT_AUTHORITY,
                sync);
    }

    /**
     * Check if the provider should be synced when a network tickle is received.
     */
    public static boolean isSyncAutomatically(Context context) {
        Account account = AccountUtils.getAccount(context);
        if (account == null) {
            return false;
        }
        return ContentResolver.getSyncAutomatically(account,
                SeriesGuideApplication.CONTENT_AUTHORITY);
    }

    /**
     * Returns true if there is currently a sync operation for the given account or authority in the
     * pending list, or actively being processed.
     */
    public static boolean isSyncActive(Context context, boolean isDisplayWarning) {
        Account account = AccountUtils.getAccount(context);
        if (account == null) {
            return false;
        }
        boolean isSyncActive = ContentResolver.isSyncActive(account,
                SeriesGuideApplication.CONTENT_AUTHORITY);
        if (isSyncActive && isDisplayWarning) {
            Toast.makeText(context, R.string.update_inprogress, Toast.LENGTH_LONG).show();
        }
        return isSyncActive;
    }

    public SgSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        Timber.d("Creating sync adapter");
    }

    public enum UpdateResult {
        SUCCESS, INCOMPLETE
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        // determine type of sync
        final boolean syncImmediately = extras.getBoolean(SyncInitBundle.SYNC_IMMEDIATE, false);
        final SyncType syncType = SyncType.from(
                extras.getInt(SyncInitBundle.SYNC_TYPE, SyncType.DELTA.id));
        Timber.i("Syncing..." + syncType + (syncImmediately ? "_IMMEDIATE" : "_REGULAR"));

        // should we sync?
        final long currentTime = System.currentTimeMillis();
        if (!syncImmediately && syncType != SyncType.SINGLE) {
            if (!isTimeForSync(getContext(), currentTime)) {
                Timber.d("Syncing...ABORT_DID_JUST_SYNC");
                return;
            }
        }

        // build a list of shows to update
        int[] showsToUpdate;
        if (syncType == SyncType.SINGLE) {
            int showTvdbId = extras.getInt(SyncInitBundle.SYNC_SHOW_TVDB_ID, 0);
            if (showTvdbId == 0) {
                Timber.e("Syncing...ABORT_INVALID_SHOW_TVDB_ID");
                return;
            }
            showsToUpdate = new int[] {
                    showTvdbId
            };
        } else {
            showsToUpdate = getShowsToUpdate(syncType, currentTime);
        }

        // from here on we need more sophisticated abort handling, so keep track of errors
        UpdateResult resultCode = UpdateResult.SUCCESS;

        // loop through shows and download latest data from TVDb
        Timber.d("Syncing...TVDb");
        final AtomicInteger updateCount = new AtomicInteger();
        final ContentResolver resolver = getContext().getContentResolver();
        for (int i = updateCount.get(); i < showsToUpdate.length; i++) {
            int id = showsToUpdate[i];

            // stop sync if connectivity is lost
            if (!AndroidUtils.isNetworkConnected(getContext())) {
                resultCode = UpdateResult.INCOMPLETE;
                break;
            }

            try {
                TheTVDB.updateShow(getContext(), id);

                // make sure other loaders (activity, overview, details) are notified
                resolver.notifyChange(Episodes.CONTENT_URI_WITHSHOW, null);
            } catch (TvdbException e) {
                // failed, continue with other shows
                resultCode = UpdateResult.INCOMPLETE;
                Timber.e(e, "Updating show failed");
            }

            updateCount.incrementAndGet();
        }

        // do some more things if this is not a quick update
        if (syncType != SyncType.SINGLE) {
            final SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(getContext());

            // get latest TMDb configuration
            Timber.d("Syncing...TMDb config");
            getTmdbConfiguration(getContext(), prefs);

            // sync with Hexagon or trakt
            final HashSet<Integer> showsExisting = ShowTools.getShowTvdbIdsAsSet(getContext());
            final HashMap<Integer, SearchResult> showsNew = new HashMap<>();
            if (showsExisting == null) {
                resultCode = UpdateResult.INCOMPLETE;
            } else {
                if (HexagonTools.isSignedIn(getContext())) {
                    // sync with hexagon...
                    boolean success = HexagonTools.syncWithHexagon(getContext(), showsExisting,
                            showsNew);
                    // don't overwrite failure
                    if (resultCode == UpdateResult.SUCCESS) {
                        resultCode = success ? UpdateResult.SUCCESS : UpdateResult.INCOMPLETE;
                    }
                } else {
                    // ...OR sync with trakt
                    UpdateResult resultTrakt = performTraktSync(getContext(), showsExisting,
                            currentTime);
                    // don't overwrite failure
                    if (resultCode == UpdateResult.SUCCESS) {
                        resultCode = resultTrakt;
                    }

                    // add shows newly discovered on trakt
                    if (showsNew.size() > 0) {
                        List<SearchResult> showsNewList = new LinkedList<>(showsNew.values());
                        TaskManager.getInstance(getContext())
                                .performAddTask(showsNewList, true, false);
                    }
                }

                // make sure other loaders (activity, overview, details) are notified of changes
                resolver.notifyChange(Episodes.CONTENT_URI_WITHSHOW, null);
            }

            // renew search table if shows were updated and it will not be renewed by add task
            if (updateCount.get() > 0 && showsToUpdate.length > 0 && showsNew.size() == 0) {
                DBUtils.rebuildFtsTable(getContext());
            }

            // update next episodes for all shows
            TaskManager.getInstance(getContext()).tryNextEpisodeUpdateTask();

            // store time of update, set retry counter on failure
            if (resultCode == UpdateResult.SUCCESS) {
                // we were successful, reset failed counter
                prefs.edit().putLong(UpdateSettings.KEY_LASTUPDATE, currentTime)
                        .putInt(UpdateSettings.KEY_FAILED_COUNTER, 0).commit();
            } else {
                int failed = UpdateSettings.getFailedNumberOfUpdates(getContext());

                /*
                 * Back off by 2**(failure + 2) * minutes. Purposely set a fake
                 * last update time, because the next update will be triggered
                 * UPDATE_INTERVAL minutes after the last update time. This way
                 * we can trigger it earlier (4min up to 32min).
                 */
                long fakeLastUpdateTime;
                if (failed < 4) {
                    fakeLastUpdateTime = currentTime
                            - ((DEFAULT_SYNC_INTERVAL_MINUTES - (int) Math.pow(2, failed + 2))
                            * DateUtils.MINUTE_IN_MILLIS);
                } else {
                    fakeLastUpdateTime = currentTime;
                }

                failed += 1;
                prefs.edit()
                        .putLong(UpdateSettings.KEY_LASTUPDATE, fakeLastUpdateTime)
                        .putInt(UpdateSettings.KEY_FAILED_COUNTER, failed).commit();
            }
        }

        // There could have been new episodes added after an update
        Utils.runNotificationService(getContext());

        Timber.i("Syncing..." + resultCode.toString());
    }

    /**
     * Returns an array of show ids to update.
     */
    private int[] getShowsToUpdate(SyncType syncType, long currentTime) {
        switch (syncType) {
            case FULL:
                // get all show IDs for a full update
                final Cursor shows = getContext().getContentResolver().query(Shows.CONTENT_URI,
                        new String[] {
                                Shows._ID
                        }, null, null, null
                );

                int[] showIds = new int[shows.getCount()];
                int i = 0;
                while (shows.moveToNext()) {
                    showIds[i] = shows.getInt(0);
                    i++;
                }

                shows.close();

                return showIds;
            case DELTA:
            default:
                // Get shows which have not been updated for a certain time.
                return TheTVDB.deltaUpdateShows(currentTime, getContext());
        }
    }

    /**
     * Downloads and stores the latest image url configuration from themoviedb.org.
     */
    private static void getTmdbConfiguration(Context context, SharedPreferences prefs) {
        try {
            Configuration config = ServiceUtils.getTmdb(context)
                    .configurationService().configuration();
            if (config != null && config.images != null
                    && !TextUtils.isEmpty(config.images.secure_base_url)) {
                prefs.edit()
                        .putString(TmdbSettings.KEY_TMDB_BASE_URL, config.images.secure_base_url)
                        .apply();
            }
        } catch (RetrofitError e) {
            Timber.e(e, "Downloading TMDb config failed");
        }
    }

    private static UpdateResult performTraktSync(Context context, HashSet<Integer> localShows,
            long currentTime) {
        if (!TraktCredentials.get(context).hasCredentials()) {
            Timber.d("performTraktSync: no auth, skip");
            return UpdateResult.SUCCESS;
        }

        if (!AndroidUtils.isNetworkConnected(context)) {
            return UpdateResult.INCOMPLETE;
        }

        // get last activity timestamps
        LastActivities lastActivity = getTraktLastActivity(context);
        if (lastActivity == null) {
            // trakt is likely offline or busy, try later
            Timber.e("performTraktSync: last activity download failed");
            return UpdateResult.INCOMPLETE;
        }

        if (!AndroidUtils.isNetworkConnected(context)) {
            return UpdateResult.INCOMPLETE;
        }

        if (localShows.size() == 0) {
            Timber.d("performTraktSync: no local shows, skip shows");
        } else {
            // download and upload episode watched and collected flags
            if (performTraktEpisodeSync(context, localShows, lastActivity.episodes, currentTime)
                    != UpdateResult.SUCCESS) {
                return UpdateResult.INCOMPLETE;
            }

            if (!AndroidUtils.isNetworkConnected(context)) {
                return UpdateResult.INCOMPLETE;
            }

            // download show ratings
            if (TraktTools.downloadShowRatings(context, lastActivity.shows)
                    != UpdateResult.SUCCESS) {
                return UpdateResult.INCOMPLETE;
            }

            if (!AndroidUtils.isNetworkConnected(context)) {
                return UpdateResult.INCOMPLETE;
            }

            // download episode ratings
            if (TraktTools.downloadEpisodeRatings(context, lastActivity.episodes)
                    != UpdateResult.SUCCESS) {
                return UpdateResult.INCOMPLETE;
            }

            if (!AndroidUtils.isNetworkConnected(context)) {
                return UpdateResult.INCOMPLETE;
            }
        }

        // sync watchlist and collection with trakt
        if (MovieTools.Download.syncMovieListsWithTrakt(context, lastActivity.movies)
                != UpdateResult.SUCCESS) {
            return UpdateResult.INCOMPLETE;
        }

        if (!AndroidUtils.isNetworkConnected(context)) {
            return UpdateResult.INCOMPLETE;
        }

        // download watched movies
        if (TraktTools.downloadWatchedMovies(context, lastActivity.movies.watched_at)
                != UpdateResult.SUCCESS) {
            return UpdateResult.INCOMPLETE;
        }

        // clean up any useless movies (not watched or not in any list)
        MovieTools.deleteUnusedMovies(context);

        if (!AndroidUtils.isNetworkConnected(context)) {
            return UpdateResult.INCOMPLETE;
        }

        // download movie ratings
        return TraktTools.downloadMovieRatings(context, lastActivity.movies.rated_at);
    }

    private static LastActivities getTraktLastActivity(Context context) {
        Timber.d("performTraktSync: get last activity");

        TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(context);
        if (trakt == null) {
            return null;
        }

        try {
            return trakt.sync().lastActivities();
        } catch (RetrofitError e) {
            Timber.e(e, "Failed to get trakt last activity");
        } catch (OAuthUnauthorizedException e) {
            TraktCredentials.get(context).setCredentialsInvalid();
        }

        return null;
    }

    /**
     * Downloads and uploads episode watched and collected flags.
     *
     * <p> Do <b>NOT</b> call if there are no local shows to avoid unnecessary work.
     */
    @SuppressLint("CommitPrefEdits")
    private static UpdateResult performTraktEpisodeSync(Context context,
            HashSet<Integer> localShows, LastActivityMore lastActivity, long currentTime) {
        // do we need to merge data instead of overwriting with data from trakt?
        boolean isInitialSync = !TraktSettings.hasMergedEpisodes(context);

        // download watched and collected flags
        // if initial sync, upload any flags missing on trakt
        // otherwise clear all local flags not on trakt
        int resultCode = TraktTools.syncEpisodeFlags(context, localShows, lastActivity,
                isInitialSync);

        if (resultCode < 0) {
            return UpdateResult.INCOMPLETE;
        }

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context)
                .edit();
        if (isInitialSync) {
            // success, set initial sync as complete
            editor.putBoolean(TraktSettings.KEY_HAS_MERGED_EPISODES, true);
        }

        // success, set last sync time to now
        editor.putLong(TraktSettings.KEY_LAST_FULL_EPISODE_SYNC, currentTime);
        Timber.d("performTraktEpisodeSync: success, last sync at " + currentTime);

        editor.commit();

        return UpdateResult.SUCCESS;
    }

    private static boolean isTimeForSync(Context context, long currentTime) {
        long previousUpdateTime = UpdateSettings.getLastAutoUpdateTime(context);
        return (currentTime - previousUpdateTime) >
                DEFAULT_SYNC_INTERVAL_MINUTES * DateUtils.MINUTE_IN_MILLIS;
    }
}
