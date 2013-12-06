
package com.battlelancer.seriesguide.sync;

import com.battlelancer.seriesguide.SeriesGuideApplication;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.settings.UpdateSettings;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.thetvdbapi.TheTVDB;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.Activity;
import com.jakewharton.trakt.entities.ActivityItem;
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
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit.RetrofitError;

/**
 * {@link AbstractThreadedSyncAdapter} which updates the show library.
 */
public class SgSyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "SgSyncAdapter";

    public static final int UPDATE_INTERVAL_MINUTES = 20;

    public static final String SHOW_TVDB_ID = "com.battlelancer.seriesguide.update_type";

    private ArrayList<SearchResult> mNewShows;

    /**
     * Helper which eventually calls {@link ContentResolver} {@code .requestSync()}, but only if at
     * least UPDATE_INTERVAL has passed.
     */
    public static void requestSync(Context context) {
        // only request sync if at least UPDATE_INTERVAL has passed
        long now = System.currentTimeMillis();
        long previousUpdateTime = UpdateSettings.getLastAutoUpdateTime(context);

        final boolean isTime = (now - previousUpdateTime) >
                UPDATE_INTERVAL_MINUTES * DateUtils.MINUTE_IN_MILLIS;

        if (isTime) {
            SgSyncAdapter.requestSync(context, 0, false);
        }
    }

    /**
     * Same as {@link #requestSync(Context, int, boolean)} with {@code isUserRequested} set to
     * true.
     */
    public static void requestSync(Context context, int showTvdbId) {
        requestSync(context, showTvdbId, true);
    }

    /**
     * Update just a single shows data, or do a delta update or a full update. Will only queue a
     * sync request if there is a network connection.
     *
     * @param showTvdbId      Update the show with the given TVDbid, if 0 does a delta update, if
     *                        less than 0 does a full update.
     * @param isUserRequested If true, will show feedback toasts and force syncing even if (global)
     *                        sync is off.
     */
    public static void requestSync(Context context, int showTvdbId, boolean isUserRequested) {
        if (!Utils.isAllowedConnection(context)) {
            // abort if no connection available
            if (isUserRequested) {
                final boolean isWifiOnly = UpdateSettings.isOnlyUpdateOverWifi(context);
                Toast.makeText(context,
                        isWifiOnly ? R.string.update_no_wifi : R.string.update_no_connection,
                        Toast.LENGTH_LONG).show();
            }
            return;
        } else if (!isUserRequested && !isSyncAutomatically(context)) {
            // abort if sync is disabled
            return;
        }

        Bundle args = new Bundle();
        args.putInt(SHOW_TVDB_ID, showTvdbId);

        if (isUserRequested) {
            // notify
            Toast.makeText(context, R.string.update_scheduled, Toast.LENGTH_SHORT).show();
            // force the sync
            args.putBoolean(ContentResolver.SYNC_EXTRAS_IGNORE_SETTINGS, true);
        }

        final Account account = SyncUtils.getSyncAccount(context);
        ContentResolver.requestSync(account,
                SeriesGuideApplication.CONTENT_AUTHORITY, args);

    }

    /**
     * Set whether or not the provider is synced when it receives a network tickle.
     */
    public static void setSyncAutomatically(Context context, boolean sync) {
        final Account account = SyncUtils.getSyncAccount(context);
        ContentResolver.setSyncAutomatically(account, SeriesGuideApplication.CONTENT_AUTHORITY,
                sync);
    }

    /**
     * Check if the provider should be synced when a network tickle is received.
     */
    public static boolean isSyncAutomatically(Context context) {
        return ContentResolver.getSyncAutomatically(SyncUtils.getSyncAccount(context),
                SeriesGuideApplication.CONTENT_AUTHORITY);
    }

    /**
     * Returns true if there is currently a sync operation for the given account or authority in the
     * pending list, or actively being processed.
     */
    public static boolean isSyncActive(Context context, boolean isDisplayWarning) {
        boolean isSyncActive = ContentResolver.isSyncActive(
                SyncUtils.getSyncAccount(context),
                SeriesGuideApplication.CONTENT_AUTHORITY);
        if (isSyncActive && isDisplayWarning) {
            Toast.makeText(context, R.string.update_inprogress, Toast.LENGTH_LONG).show();
        }
        return isSyncActive;
    }

    public SgSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        Log.d(TAG, "Creating SyncAdapter");
    }

    enum UpdateResult {
        SUCCESS, INCOMPLETE;
    }

    public enum UpdateType {
        SINGLE, DELTA, FULL
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        Log.d(TAG, "Starting to sync shows");

        // determine type of sync
        int showTvdbId = extras.getInt(SgSyncAdapter.SHOW_TVDB_ID);

        int[] mShows = null;
        UpdateType type;
        if (showTvdbId == 0) {
            type = UpdateType.DELTA;
        } else if (showTvdbId < 0) {
            type = UpdateType.FULL;
        } else {
            type = UpdateType.SINGLE;
            mShows = new int[]{
                    showTvdbId
            };
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        final ContentResolver resolver = getContext().getContentResolver();
        final long currentTime = System.currentTimeMillis();
        UpdateResult resultCode = UpdateResult.SUCCESS;
        final AtomicInteger updateCount = new AtomicInteger();

        // build a list of shows to update
        if (mShows == null) {
            mShows = getShowsToUpdate(type, currentTime);
        }

        Log.d(TAG, "Perform TVDb update");

        // actually update the shows
        for (int i = updateCount.get(); i < mShows.length; i++) {
            int id = mShows[i];

            // try to contact TVDb two times
            for (int itry = 0; itry < 2; itry++) {
                // stop sync if connectivity is lost
                if (!AndroidUtils.isNetworkConnected(getContext())) {
                    resultCode = UpdateResult.INCOMPLETE;
                    break;
                }

                try {
                    TheTVDB.updateShow(id, getContext());

                    // make sure overview and details loaders are notified
                    resolver.notifyChange(Episodes.CONTENT_URI_WITHSHOW, null);

                    break;
                } catch (SAXException e) {
                    if (itry == 1) {
                        // failed twice, report error
                        resultCode = UpdateResult.INCOMPLETE;
                        Utils.trackExceptionAndLog(getContext(), TAG, e);
                    }
                }
            }

            // stop sync if connectivity is lost
            if (!AndroidUtils.isNetworkConnected(getContext())) {
                resultCode = UpdateResult.INCOMPLETE;
                break;
            }
            updateCount.incrementAndGet();
        }

        /*
         * Renew search table, get trakt activity and the latest tmdb config if
         * we did update multiple shows.
         */
        if (type != UpdateType.SINGLE) {

            if (updateCount.get() > 0 && mShows.length > 0) {
                // renew search table
                TheTVDB.onRenewFTSTable(getContext());
            }

            // get latest TMDb configuration
            try {
                Configuration config = ServiceUtils.getTmdbServiceManager(getContext())
                        .configurationService().configuration();
                if (config != null && config.images != null
                        && !TextUtils.isEmpty(config.images.base_url)) {
                    prefs.edit()
                            .putString(SeriesGuidePreferences.KEY_TMDB_BASE_URL,
                                    config.images.base_url).commit();
                }
            } catch (RetrofitError e) {
                Utils.trackExceptionAndLog(getContext(), TAG, e);
            }

            // validate trakt credentials
            ServiceUtils.checkTraktCredentials(getContext());

            // get newly watched episodes from trakt
            final UpdateResult traktResult = getTraktActivity();

            // do not overwrite earlier failure codes
            if (resultCode == UpdateResult.SUCCESS) {
                resultCode = traktResult;
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
                            - ((UPDATE_INTERVAL_MINUTES - (int) Math.pow(2, failed + 2))
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

        // add newly discovered shows to database
        if (mNewShows != null && mNewShows.size() > 0) {
            TaskManager.getInstance(getContext()).performAddTask(mNewShows, true);
        }

        // There could have been new episodes added after an update
        Utils.runNotificationService(getContext());

        Log.d(TAG, "Finished syncing shows (" + showTvdbId + "): " + resultCode.toString());
    }

    /**
     * Returns an array of show ids to update.
     */
    private int[] getShowsToUpdate(UpdateType type, long currentTime) {
        switch (type) {
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

    private UpdateResult getTraktActivity() {
        Log.d(TAG, "Getting trakt activity...");
        if (!TraktSettings.hasTraktCredentials(getContext())) {
            // trakt is not connected, we are done here
            return UpdateResult.SUCCESS;
        }

        // return if connectivity is lost
        if (!AndroidUtils.isNetworkConnected(getContext())) {
            return UpdateResult.INCOMPLETE;
        }

        Trakt manager = ServiceUtils.getTraktServiceManagerWithAuth(getContext(), false);
        if (manager == null) {
            return UpdateResult.INCOMPLETE;
        }

        // get last trakt update timestamp
        final long startTimeTrakt = TraktSettings.getLastUpdateTime(getContext()) / 1000;

        // get activity from trakt
        Activity activities;
        try {
            activities = manager
                    .activityService()
                    .user(TraktSettings.getUsername(getContext()),
                            ActivityType.Episode.toString(),
                            ActivityAction.Checkin + "," + ActivityAction.Seen + "," +
                                    ActivityAction.Scrobble + "," + ActivityAction.Collection,
                            startTimeTrakt, 1, 0);
        } catch (RetrofitError e) {
            Utils.trackExceptionAndLog(getContext(), TAG, e);
            return UpdateResult.INCOMPLETE;
        }

        if (activities == null || activities.activity == null) {
            return UpdateResult.INCOMPLETE;
        }

        // get a list of existing shows
        final HashSet<Integer> existingShowTvdbIds = new HashSet<>();
        final Cursor shows = getContext().getContentResolver().query(Shows.CONTENT_URI,
                new String[]{Shows._ID}, null, null, null);
        if (shows == null) {
            return UpdateResult.INCOMPLETE;
        }
        while (shows.moveToNext()) {
            existingShowTvdbIds.add(shows.getInt(0));
        }
        shows.close();

        // build an update batch for episode flag changes of existing shows, detect new shows
        mNewShows = new ArrayList<>();
        boolean isAutoAddingShows = TraktSettings.isAutoAddingShows(getContext());
        final HashSet<Integer> newShowTvdbIds = new HashSet<>();
        final ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        for (ActivityItem activity : activities.activity) {
            if (activity == null || activity.show == null
                    || activity.show.tvdb_id == null) {
                // invalid activity, skip
                continue;
            }
            if (isAutoAddingShows && !existingShowTvdbIds.contains(activity.show.tvdb_id)
                    && !newShowTvdbIds.contains(activity.show.tvdb_id)) {
                // new show detected to add locally later
                SearchResult show = new SearchResult();
                show.title = activity.show.title;
                show.tvdbid = activity.show.tvdb_id;
                mNewShows.add(show);
                newShowTvdbIds.add(activity.show.tvdb_id); // prevent duplicates
            } else if (existingShowTvdbIds.contains(activity.show.tvdb_id)) {
                // show exists locally, get episode flag changes
                buildActionBatch(getContext(), batch, activity);
            }
        }

        // apply all episode updates from downloaded trakt activity
        DBUtils.applyInSmallBatches(getContext(), batch);

        // store time of this update as seen by the trakt server
        final SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(getContext()).edit();
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
                for (TvShowEpisode episode : episodes) {
                    if (episode == null) {
                        continue;
                    }
                    if (episode.season > season || episode.number > number) {
                        season = episode.season;
                        number = episode.number;
                    }
                    addEpisodeSeenUpdateOp(batch, episode, item.show.tvdb_id);
                }

                // set highest season + number combo as last watched
                if (season != -1 && number != -1) {
                    addLastWatchedUpdateOp(context, batch, season, number, item.show.tvdb_id);
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
                addEpisodeSeenUpdateOp(batch, episode, item.show.tvdb_id);
                addLastWatchedUpdateOp(context, batch, episode.season, episode.number,
                        item.show.tvdb_id);
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
                    addEpisodeCollectedUpdateOp(batch, episode, item.show.tvdb_id);
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
            TvShowEpisode episode, int showTvdbId) {
        batch.add(ContentProviderOperation.newUpdate(Episodes.buildEpisodesOfShowUri(showTvdbId))
                .withSelection(Episodes.NUMBER + "=? AND " + Episodes.SEASON + "=?", new String[]{
                        String.valueOf(episode.number), String.valueOf(episode.season)
                }).withValue(Episodes.WATCHED, EpisodeFlags.WATCHED).build());
    }

    /**
     * Helper method to build update to flag an episode collected.
     */
    private static void addEpisodeCollectedUpdateOp(ArrayList<ContentProviderOperation> batch,
            TvShowEpisode episode, int showTvdbId) {
        batch.add(ContentProviderOperation.newUpdate(Episodes.buildEpisodesOfShowUri(showTvdbId))
                .withSelection(Episodes.NUMBER + "=? AND " + Episodes.SEASON + "=?", new String[]{
                        String.valueOf(episode.number), String.valueOf(episode.season)
                }).withValue(Episodes.COLLECTED, true).build());
    }

    /**
     * Queries for an episode id and adds a content provider op to set it as last watched for the
     * given show.
     */
    private static void addLastWatchedUpdateOp(Context context,
            ArrayList<ContentProviderOperation> batch, int season, int number, int showTvdbId) {
        // query for the episode id
        final Cursor episode = context.getContentResolver().query(
                Episodes.buildEpisodesOfShowUri(showTvdbId),
                new String[]{
                        Episodes._ID
                }, Episodes.SEASON + "=" + season + " AND "
                + Episodes.NUMBER + "=" + number, null, null);

        // store the episode id as last watched for the given show
        if (episode != null) {
            if (episode.moveToFirst()) {
                batch.add(ContentProviderOperation.newUpdate(Shows.buildShowUri(showTvdbId))
                        .withValue(Shows.LASTWATCHEDID, episode.getInt(0)).build());
            }

            episode.close();
        }
    }

}
