
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

import com.battlelancer.seriesguide.SeriesGuideApplication;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.TmdbSettings;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.settings.UpdateSettings;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.MovieTools;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.TraktSync;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.thetvdbapi.TheTVDB;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.Activity;
import com.jakewharton.trakt.entities.ActivityItem;
import com.jakewharton.trakt.entities.TvShow;
import com.jakewharton.trakt.entities.TvShowEpisode;
import com.jakewharton.trakt.enumerations.ActivityAction;
import com.jakewharton.trakt.enumerations.ActivityType;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;
import com.uwetrottmann.tmdb.entities.Configuration;

import org.xml.sax.SAXException;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit.RetrofitError;

/**
 * {@link AbstractThreadedSyncAdapter} which updates the show library.
 */
public class SgSyncAdapter extends AbstractThreadedSyncAdapter {

    public static final String TAG = "SgSyncAdapter";

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
     * Schedules a sync. Will only queue a sync request if there is a network connection and
     * auto-sync is enabled.
     *
     * @param syncType   Any of {@link SyncType}.
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
     * @param syncType        Any of {@link SyncType}.
     * @param showTvdbId      If using {@link SyncType#SINGLE}, the TVDb id of a show.
     * @param isUserRequested If set, shows a status toast and aborts if offline.
     */
    public static void requestSyncImmediate(Context context, SyncType syncType, int showTvdbId,
            boolean isUserRequested) {
        if (isUserRequested) {
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
        final Account account = AccountUtils.getAccount(context);
        ContentResolver.requestSync(account,
                SeriesGuideApplication.CONTENT_AUTHORITY, args);
    }

    /**
     * Set whether or not the provider is synced when it receives a network tickle.
     */
    public static void setSyncAutomatically(Context context, boolean sync) {
        final Account account = AccountUtils.getAccount(context);
        ContentResolver.setSyncAutomatically(account, SeriesGuideApplication.CONTENT_AUTHORITY,
                sync);
    }

    /**
     * Check if the provider should be synced when a network tickle is received.
     */
    public static boolean isSyncAutomatically(Context context) {
        return ContentResolver.getSyncAutomatically(AccountUtils.getAccount(context),
                SeriesGuideApplication.CONTENT_AUTHORITY);
    }

    /**
     * Returns true if there is currently a sync operation for the given account or authority in the
     * pending list, or actively being processed.
     */
    public static boolean isSyncActive(Context context, boolean isDisplayWarning) {
        boolean isSyncActive = ContentResolver.isSyncActive(
                AccountUtils.getAccount(context),
                SeriesGuideApplication.CONTENT_AUTHORITY);
        if (isSyncActive && isDisplayWarning) {
            Toast.makeText(context, R.string.update_inprogress, Toast.LENGTH_LONG).show();
        }
        return isSyncActive;
    }

    public SgSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        Log.d(TAG, "Creating sync adapter");
    }

    public enum UpdateResult {
        SUCCESS, INCOMPLETE
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        // determine type of sync
        final boolean syncImmediately = extras.getBoolean(SyncInitBundle.SYNC_IMMEDIATE, false);
        final SyncType syncType = SyncType.from(
                extras.getInt(SyncInitBundle.SYNC_TYPE, SyncType.DELTA.id));
        Log.d(TAG, "Syncing..." + syncType + (syncImmediately ? "_IMMEDIATE" : "_REGULAR"));

        // should we sync?
        final long currentTime = System.currentTimeMillis();
        if (!syncImmediately && syncType != SyncType.SINGLE) {
            if (!isTimeForSync(getContext(), currentTime)) {
                Log.d(TAG, "Syncing...ABORT_DID_JUST_SYNC");
                return;
            }
        }

        // build a list of shows to update
        int[] showsToUpdate;
        if (syncType == SyncType.SINGLE) {
            int showTvdbId = extras.getInt(SyncInitBundle.SYNC_SHOW_TVDB_ID, 0);
            if (showTvdbId == 0) {
                Log.d(TAG, "Syncing...ERROR_INVALID_SHOW_TVDB_ID");
                return;
            }
            showsToUpdate = new int[]{
                    showTvdbId
            };
        } else {
            showsToUpdate = getShowsToUpdate(syncType, currentTime);
        }

        // from here on we need more sophisticated abort handling, so keep track of errors
        UpdateResult resultCode = UpdateResult.SUCCESS;

        // loop through shows and download latest data from TVDb
        Log.d(TAG, "Syncing...TVDb");
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
                TheTVDB.updateShow(id, getContext());

                // make sure overview and details loaders are notified
                resolver.notifyChange(Episodes.CONTENT_URI_WITHSHOW, null);
            } catch (SAXException e) {
                // failed, continue with other shows
                resultCode = UpdateResult.INCOMPLETE;
                Utils.trackExceptionAndLog(getContext(), TAG, e);
            }

            updateCount.incrementAndGet();
        }

        // do some more things if this is not a quick update
        if (syncType != SyncType.SINGLE) {
            final SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(getContext());

            // get latest TMDb configuration
            Log.d(TAG, "Syncing...TMDb config");
            getTmdbConfiguration(getContext(), prefs);

            // prepare for finding shows not yet added to local database
            final HashSet<Integer> showsExisting = ShowTools.getShowTvdbIdsAsSet(getContext());
            final HashMap<Integer, SearchResult> showsNew = new HashMap<>();
            if (showsExisting == null) {
                resultCode = UpdateResult.INCOMPLETE;
            } else {
                // sync with hexagon
                if (ShowTools.get(getContext()).isSignedIn()) {
                    UpdateResult resultHexagon = ShowTools.Download
                            .syncRemoteShows(getContext(), showsExisting, showsNew);
                    // don't overwrite earlier failure
                    if (resultCode == UpdateResult.SUCCESS) {
                        resultCode = resultHexagon;
                    }
                }

                // sync movies with trakt
                Log.d(TAG, "Sync movies with trakt...");
                UpdateResult resultMovies = MovieTools.Download.syncMoviesFromTrakt(getContext());
                Log.d(TAG, "Sync movies with trakt..." + resultMovies.toString());
                // don't overwrite earlier failure
                if (resultCode == UpdateResult.SUCCESS) {
                    resultCode = resultMovies;
                }

                // sync with trakt
                UpdateResult resultTrakt = syncWithTrakt(getContext(), showsExisting, showsNew,
                        syncImmediately, currentTime);
                // don't overwrite earlier failure with success
                if (resultCode == UpdateResult.SUCCESS) {
                    resultCode = resultTrakt;
                }

                // add newly discovered shows to database
                if (showsNew.size() > 0) {
                    List<SearchResult> showsNewList = new LinkedList<>(showsNew.values());
                    TaskManager.getInstance(getContext()).performAddTask(showsNewList, true);
                }
            }

            // renew search table if shows were updated and it will not be renewed by add task
            if (updateCount.get() > 0 && showsToUpdate.length > 0 && showsNew.size() == 0) {
                TheTVDB.onRenewFTSTable(getContext());
            }

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

        Log.d(TAG, "Syncing..." + resultCode.toString());
    }

    /**
     * Returns an array of show ids to update.
     */
    private int[] getShowsToUpdate(SyncType syncType, long currentTime) {
        switch (syncType) {
            case FULL:
                // get all show IDs for a full update
                final Cursor shows = getContext().getContentResolver().query(Shows.CONTENT_URI,
                        new String[]{
                                Shows._ID
                        }, null, null, null);

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
            Configuration config = ServiceUtils.getTmdbServiceManager(context)
                    .configurationService().configuration();
            if (config != null && config.images != null
                    && !TextUtils.isEmpty(config.images.base_url)) {
                prefs.edit()
                        .putString(TmdbSettings.KEY_TMDB_BASE_URL, config.images.base_url).commit();
            }
        } catch (RetrofitError e) {
            Utils.trackExceptionAndLog(context, TAG, e);
        }
    }

    private static UpdateResult syncWithTrakt(Context context, HashSet<Integer> showsExisting,
            HashMap<Integer, SearchResult> showsNew, boolean syncImmediately, long currentTime) {
        // validate credentials with trakt servers
        Log.d(TAG, "Syncing...trakt auth check");
        TraktCredentials.get(context).validateCredentials();

        // are we still authenticated?
        Trakt trakt = ServiceUtils.getTraktWithAuth(context);
        if (trakt == null) {
            // no, we are done here
            return UpdateResult.SUCCESS;
        }

        if (!AndroidUtils.isNetworkConnected(context)) {
            return UpdateResult.INCOMPLETE;
        }

        UpdateResult result = UpdateResult.SUCCESS;

        // sync all watched and collected flags?
        if (syncImmediately || TraktSettings.isTimeForFullSync(context, currentTime)) {
            Log.d(TAG, "Syncing...trakt full sync...");
            int resultCode = TraktSync.syncToSeriesGuide(context, trakt, showsExisting, true);

            if (resultCode < 0) {
                result = UpdateResult.INCOMPLETE;
                Log.d(TAG, "Syncing...trakt full sync...INCOMPLETE");
            } else {
                // set last full sync time
                PreferenceManager.getDefaultSharedPreferences(context).edit()
                        .putLong(TraktSettings.KEY_LAST_FULL_SYNC, currentTime)
                        .commit();
                Log.d(TAG, "Syncing...trakt full sync...SUCCESS");
            }

            if (!AndroidUtils.isNetworkConnected(context)) {
                return UpdateResult.INCOMPLETE;
            }
        }

        // get trakt activity
        Log.d(TAG, "Syncing...trakt activity...");
        UpdateResult activityResult = getTraktActivity(context, trakt, showsExisting, showsNew);
        Log.d(TAG, "Syncing...trakt activity..." + activityResult.toString());

        // don't overwrite failure
        if (result == UpdateResult.SUCCESS) {
            result = activityResult;
        }

        return result;
    }

    /**
     * Downloads new trakt episode activity (checkins, seen, scrobbles, collected) and applies
     * resulting episode flag changes (watched, collected) to the database for the given {@code
     * showsExisting}.<br/>If {@link TraktSettings#isAutoAddingShows(android.content.Context)} is
     * enabled, detects new shows based on {@code showsExisting} and adds them to {@code showsNew}.
     */
    private static UpdateResult getTraktActivity(Context context, Trakt trakt,
            HashSet<Integer> showsExisting, HashMap<Integer, SearchResult> showsNew) {
        // get cut-off for activity stream
        final long startTimeTrakt = TraktSettings.getLastUpdateTime(context) / 1000;

        // download activity stream
        Activity activities;
        try {
            activities = trakt
                    .activityService()
                    .user(TraktCredentials.get(context).getUsername(),
                            ActivityType.Episode.toString(),
                            ActivityAction.Checkin + "," + ActivityAction.Seen + "," +
                                    ActivityAction.Scrobble + "," + ActivityAction.Collection,
                            startTimeTrakt, 1, 0);
        } catch (RetrofitError e) {
            Utils.trackExceptionAndLog(context, TAG, e);
            return UpdateResult.INCOMPLETE;
        }

        if (activities == null || activities.activity == null) {
            return UpdateResult.INCOMPLETE;
        }

        // build an update batch for episode flag changes of existing shows, detect new shows
        final ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        final boolean isAutoAddingShows = TraktSettings.isAutoAddingShows(context);

        for (ActivityItem activity : activities.activity) {
            if (activity == null || activity.show == null
                    || activity.show.tvdb_id == null) {
                // invalid activity, skip
                continue;
            }

            if (showsExisting.contains(activity.show.tvdb_id)) {
                // show exists locally, get episode flag changes
                buildActionBatch(context, batch, activity);
            } else if (isAutoAddingShows && !showsNew.containsKey(activity.show.tvdb_id)) {
                // new show, remember to add to local database later
                SearchResult show = new SearchResult();
                show.tvdbid = activity.show.tvdb_id;
                show.title = activity.show.title;
                showsNew.put(activity.show.tvdb_id, show);
            }
        }

        // apply all episode updates from downloaded trakt activity
        DBUtils.applyInSmallBatches(context, batch);

        // store time of this update as seen by the trakt server
        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(context).edit();
        editor.putLong(TraktSettings.KEY_LAST_UPDATE,
                activities.timestamps.current.getTime()).commit();

        return UpdateResult.SUCCESS;
    }

    /**
     * Adds episode update ops based on the action of the given activity item.
     */
    private static void buildActionBatch(Context context, ArrayList<ContentProviderOperation> batch,
            ActivityItem item) {
        if (item.action == null) {
            return;
        }
        switch (item.action) {
            case Seen: {
                // recently watched episode
                if (item.episodes == null) {
                    break;
                }

                // seen uses an array of episodes
                List<TvShowEpisode> episodes = item.episodes;
                int season = -1;
                int number = -1;
                TvShowEpisode highestEpisode = null;
                for (TvShowEpisode episode : episodes) {
                    if (episode == null) {
                        continue;
                    }
                    if (episode.season > season || episode.number > number) {
                        season = episode.season;
                        number = episode.number;
                        highestEpisode = episode;
                    }
                    addEpisodeSeenUpdateOp(batch, episode);
                }

                // set highest season + number combo as last watched
                if (highestEpisode != null) {
                    addLastWatchedUpdateOp(batch, item.show, highestEpisode);
                }

                break;
            }
            case Checkin:
            case Scrobble: {
                if (item.episode == null) {
                    break;
                }
                // checkin and scrobble use a single episode
                TvShowEpisode episode = item.episode;
                addEpisodeSeenUpdateOp(batch, episode);
                addLastWatchedUpdateOp(batch, item.show, episode);
                break;
            }
            case Collection: {
                if (item.episodes == null) {
                    break;
                }

                // collection uses an array of episodes
                List<TvShowEpisode> episodes = item.episodes;
                for (TvShowEpisode episode : episodes) {
                    if (episode == null) {
                        continue;
                    }
                    addEpisodeCollectedUpdateOp(batch, episode);
                }
                break;
            }
            default:
                break;
        }
    }

    /**
     * Helper method to build update to flag an episode watched.
     */
    private static void addEpisodeSeenUpdateOp(final ArrayList<ContentProviderOperation> batch,
            TvShowEpisode episode) {
        batch.add(ContentProviderOperation.newUpdate(Episodes.buildEpisodeUri(episode.tvdb_id))
                .withValue(Episodes.WATCHED, EpisodeFlags.WATCHED).build());
    }

    /**
     * Helper method to build update to flag an episode collected.
     */
    private static void addEpisodeCollectedUpdateOp(ArrayList<ContentProviderOperation> batch,
            TvShowEpisode episode) {
        batch.add(ContentProviderOperation.newUpdate(Episodes.buildEpisodeUri(episode.tvdb_id))
                .withValue(Episodes.COLLECTED, true).build());
    }

    /**
     * Adds a content provider op to set the episode TVDb id as last watched for the given show.
     */
    private static void addLastWatchedUpdateOp(ArrayList<ContentProviderOperation> batch,
            TvShow show, TvShowEpisode episode) {
        // store the episode id as last watched for the given show
        batch.add(ContentProviderOperation.newUpdate(Shows.buildShowUri(show.tvdb_id))
                .withValue(Shows.LASTWATCHEDID, episode.tvdb_id).build());
    }

    private static boolean isTimeForSync(Context context, long currentTime) {
        long previousUpdateTime = UpdateSettings.getLastAutoUpdateTime(context);
        return (currentTime - previousUpdateTime) >
                DEFAULT_SYNC_INTERVAL_MINUTES * DateUtils.MINUTE_IN_MILLIS;
    }

}
