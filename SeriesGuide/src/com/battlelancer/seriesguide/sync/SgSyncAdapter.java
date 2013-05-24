
package com.battlelancer.seriesguide.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

import com.battlelancer.seriesguide.SeriesGuideApplication;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.util.AdvancedSettings;
import com.battlelancer.seriesguide.util.Lists;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.thetvdbapi.TheTVDB;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.entities.Activity;
import com.jakewharton.trakt.entities.ActivityItem;
import com.jakewharton.trakt.entities.TvShowEpisode;
import com.jakewharton.trakt.enumerations.ActivityAction;
import com.jakewharton.trakt.enumerations.ActivityType;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;
import com.uwetrottmann.tmdb.TmdbException;
import com.uwetrottmann.tmdb.entities.Configuration;

import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link AbstractThreadedSyncAdapter} which updates the show library.
 */
public class SgSyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "SgSyncAdapter";

    public static final int UPDATE_INTERVAL_MINUTES = 30;

    public static final String SHOW_TVDB_ID = "com.battlelancer.seriesguide.update_type";

    private ArrayList<SearchResult> mNewShows;

    /**
     * Helper which eventually calls {@link ContentResolver}
     * {@code .requestSync()}, but only if at least UPDATE_INTERVAL has passed.
     */
    public static void requestSync(Context context) {
        if (AndroidUtils.isNetworkConnected(context)) {
            // only request sync if at least UPDATE_INTERVAL has passed
            long now = System.currentTimeMillis();
            long previousUpdateTime = AdvancedSettings.getLastAutoUpdateTime(context);

            final boolean isTime = (now - previousUpdateTime) >
                    UPDATE_INTERVAL_MINUTES * DateUtils.MINUTE_IN_MILLIS;

            if (isTime) {
                SgSyncAdapter.requestSync(context, 0);
            }
        }
    }

    /**
     * Update just a single shows data, or do a delta update or a full update.
     * 
     * @param showTvdbId Update the show with the given TVDbid, if 0 does a
     *            delta update, if less than 0 does a full update.
     */
    public static void requestSync(Context context, int showTvdbId) {
        Bundle args = new Bundle();
        args.putInt(SHOW_TVDB_ID, showTvdbId);

        final Account account = SgAccountAuthenticator.getSyncAccount(context);
        ContentResolver.requestSync(account,
                SeriesGuideApplication.CONTENT_AUTHORITY, args);
    }

    /**
     * Set whether or not the provider is synced when it receives a network
     * tickle.
     */
    public static void setSyncAutomatically(Context context, boolean sync) {
        final Account account = SgAccountAuthenticator.getSyncAccount(context);
        ContentResolver.setSyncAutomatically(account, SeriesGuideApplication.CONTENT_AUTHORITY,
                sync);
    }

    /**
     * Check if the provider should be synced when a network tickle is received.
     */
    public static boolean isSyncAutomatically(Context context) {
        return ContentResolver.getSyncAutomatically(SgAccountAuthenticator.getSyncAccount(context),
                SeriesGuideApplication.CONTENT_AUTHORITY);
    }

    /**
     * Returns true if there is currently a sync operation for the given account
     * or authority in the pending list, or actively being processed.
     */
    public static boolean isSyncActive(Context context, boolean displayWarning) {
        boolean isSyncActive = ContentResolver.isSyncActive(
                SgAccountAuthenticator.getSyncAccount(context),
                SeriesGuideApplication.CONTENT_AUTHORITY);
        if (isSyncActive) {
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

        String[] mShows = null;
        UpdateType type;
        if (showTvdbId == 0) {
            type = UpdateType.DELTA;
        } else if (showTvdbId < 0) {
            type = UpdateType.FULL;
        } else {
            type = UpdateType.SINGLE;
            mShows = new String[] {
                    String.valueOf(showTvdbId)
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

        // actually update the shows
        for (int i = updateCount.get(); i < mShows.length; i++) {
            String id = mShows[i];

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

                // get latest TMDb configuration
                try {
                    Configuration config = ServiceUtils.getTmdbServiceManager(getContext())
                            .configurationService()
                            .configuration().fire();
                    if (config != null && config.images != null
                            && !TextUtils.isEmpty(config.images.base_url)) {
                        prefs.edit()
                                .putString(SeriesGuidePreferences.KEY_TMDB_BASE_URL,
                                        config.images.base_url).commit();
                    }
                } catch (TmdbException e) {
                    Utils.trackExceptionAndLog(getContext(), TAG, e);
                } catch (ApiException e) {
                    Utils.trackExceptionAndLog(getContext(), TAG, e);
                }
            }

            // get newly watched episodes from trakt
            final UpdateResult traktResult = getTraktActivity(currentTime);

            // do not overwrite earlier failure codes
            if (resultCode == UpdateResult.SUCCESS) {
                resultCode = traktResult;
            }

            // store time of update, set retry counter on failure
            if (resultCode == UpdateResult.SUCCESS) {
                // we were successful, reset failed counter
                prefs.edit().putLong(AdvancedSettings.KEY_LASTUPDATE, currentTime)
                        .putInt(SeriesGuidePreferences.KEY_FAILED_COUNTER, 0).commit();
            } else {
                int failed = prefs.getInt(SeriesGuidePreferences.KEY_FAILED_COUNTER, 0);

                /*
                 * Back off by 2**(failure + 2) * minutes. Purposely set a fake
                 * last update time, because the next update will be triggered
                 * UPDATE_INTERVAL minutes after the last update time. This way
                 * we can trigger it earlier (4min up to 32min).
                 */
                long fakeLastUpdateTime;
                if (failed < 4) {
                    fakeLastUpdateTime = currentTime
                            - ((UPDATE_INTERVAL_MINUTES - (int) Math.pow(2, failed + 2)) * DateUtils.MINUTE_IN_MILLIS);
                } else {
                    fakeLastUpdateTime = currentTime;
                }

                failed += 1;
                prefs.edit()
                        .putLong(AdvancedSettings.KEY_LASTUPDATE, fakeLastUpdateTime)
                        .putInt(SeriesGuidePreferences.KEY_FAILED_COUNTER, failed).commit();
            }
        }

        // add newly discovered shows to database
        if (mNewShows != null && mNewShows.size() > 0) {
            TaskManager.getInstance(getContext()).performAddTask(mNewShows);
        }

        // There could have been new episodes added after an update
        Utils.runNotificationService(getContext());

        Log.d(TAG, "Finished syncing shows (" + showTvdbId + "): " + resultCode.toString());
    }

    /**
     * Returns an array of show ids to update.
     */
    private String[] getShowsToUpdate(UpdateType type, long currentTime) {
        switch (type) {
            case FULL:
                // get all show IDs for a full update
                final Cursor shows = getContext().getContentResolver().query(Shows.CONTENT_URI,
                        new String[] {
                            Shows._ID
                        }, null, null, null);

                String[] showIds = new String[shows.getCount()];
                int i = 0;
                while (shows.moveToNext()) {
                    showIds[i] = shows.getString(0);
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

    private UpdateResult getTraktActivity(long currentTime) {
        if (!ServiceUtils.isTraktCredentialsValid(getContext())) {
            // trakt is not connected, we are done here
            return UpdateResult.SUCCESS;
        }

        // return if connectivity is lost
        if (!AndroidUtils.isNetworkConnected(getContext())) {
            return UpdateResult.INCOMPLETE;
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        final ContentResolver resolver = getContext().getContentResolver();

        // get last trakt update timestamp
        final long startTimeTrakt = prefs.getLong(SeriesGuidePreferences.KEY_LASTTRAKTUPDATE,
                currentTime) / 1000;

        ServiceManager manager = ServiceUtils
                .getTraktServiceManagerWithAuth(getContext(), false);
        if (manager == null) {
            return UpdateResult.INCOMPLETE;
        }

        // get episode activity from trakt
        Activity activity;
        try {
            activity = manager
                    .activityService()
                    .user(ServiceUtils.getTraktUsername(getContext()))
                    .types(ActivityType.Episode)
                    .actions(ActivityAction.Checkin, ActivityAction.Seen,
                            ActivityAction.Scrobble, ActivityAction.Collection)
                    .timestamp(startTimeTrakt).fire();
        } catch (TraktException e) {
            Utils.trackExceptionAndLog(getContext(), TAG, e);
            return UpdateResult.INCOMPLETE;
        } catch (ApiException e) {
            Utils.trackExceptionAndLog(getContext(), TAG, e);
            return UpdateResult.INCOMPLETE;
        }

        if (activity == null || activity.activity == null) {
            return UpdateResult.INCOMPLETE;
        }

        // get a list of existing shows
        boolean isAutoAddingShows = prefs.getBoolean(
                SeriesGuidePreferences.KEY_AUTO_ADD_TRAKT_SHOWS, true);
        final HashSet<String> existingShows = new HashSet<String>();
        if (isAutoAddingShows) {
            final Cursor shows = resolver.query(Shows.CONTENT_URI,
                    new String[] {
                        Shows._ID
                    }, null, null, null);
            if (shows != null) {
                while (shows.moveToNext()) {
                    existingShows.add(shows.getString(0));
                }
                shows.close();
            }
        }

        // build an update batch
        mNewShows = Lists.newArrayList();
        final HashSet<String> newShowIds = new HashSet<String>();
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
        for (ActivityItem item : activity.activity) {
            // check for null (potential fix for reported crash)
            if (item.action != null && item.show != null) {
                if (isAutoAddingShows && !existingShows.contains(item.show.tvdbId)
                        && !newShowIds.contains(item.show.tvdbId)) {
                    SearchResult show = new SearchResult();
                    show.title = item.show.title;
                    show.tvdbid = item.show.tvdbId;
                    mNewShows.add(show);
                    newShowIds.add(item.show.tvdbId); // prevent duplicates
                } else {
                    // show added, get watched episodes
                    switch (item.action) {
                        case Seen: {
                            // seen uses an array of episodes
                            List<TvShowEpisode> episodes = item.episodes;
                            int season = -1;
                            int number = -1;
                            for (TvShowEpisode episode : episodes) {
                                if (episode.season > season || episode.number > number) {
                                    season = episode.season;
                                    number = episode.number;
                                }
                                addEpisodeSeenUpdateOp(batch, episode, item.show.tvdbId);
                            }
                            // set highest season + number combo as last
                            // watched
                            if (season != -1 && number != -1) {
                                addLastWatchedUpdateOp(resolver, batch, season, number,
                                        item.show.tvdbId);
                            }
                            break;
                        }
                        case Checkin:
                        case Scrobble: {
                            // checkin and scrobble use a single episode
                            TvShowEpisode episode = item.episode;
                            addEpisodeSeenUpdateOp(batch, episode, item.show.tvdbId);
                            addLastWatchedUpdateOp(resolver, batch, episode.season,
                                    episode.number, item.show.tvdbId);
                            break;
                        }
                        case Collection: {
                            // collection uses an array of episodes
                            List<TvShowEpisode> episodes = item.episodes;
                            for (TvShowEpisode episode : episodes) {
                                addEpisodeCollectedUpdateOp(batch, episode, item.show.tvdbId);
                            }
                            break;
                        }
                        default:
                            break;
                    }
                }
            }
        }

        // execute the batch
        try {
            getContext().getContentResolver()
                    .applyBatch(SeriesGuideApplication.CONTENT_AUTHORITY, batch);
        } catch (RemoteException e) {
            // Failed binder transactions aren't recoverable
            Utils.trackExceptionAndLog(getContext(), TAG, e);
            throw new RuntimeException("Problem applying batch operation", e);
        } catch (OperationApplicationException e) {
            // Failures like constraint violation aren't
            // recoverable
            Utils.trackExceptionAndLog(getContext(), TAG, e);
            throw new RuntimeException("Problem applying batch operation", e);
        }

        // store time of this update as seen by the trakt server
        prefs.edit()
                .putLong(SeriesGuidePreferences.KEY_LASTTRAKTUPDATE,
                        activity.timestamps.current.getTime()).commit();

        return UpdateResult.SUCCESS;
    }

    /**
     * Helper method to build update to flag an episode watched.
     */
    private static void addEpisodeSeenUpdateOp(final ArrayList<ContentProviderOperation> batch,
            TvShowEpisode episode, String showTvdbId) {
        batch.add(ContentProviderOperation.newUpdate(Episodes.buildEpisodesOfShowUri(showTvdbId))
                .withSelection(Episodes.NUMBER + "=? AND " + Episodes.SEASON + "=?", new String[] {
                        String.valueOf(episode.number), String.valueOf(episode.season)
                }).withValue(Episodes.WATCHED, true).build());
    }

    /**
     * Helper method to build update to flag an episode collected.
     */
    private static void addEpisodeCollectedUpdateOp(ArrayList<ContentProviderOperation> batch,
            TvShowEpisode episode, String showTvdbId) {
        batch.add(ContentProviderOperation.newUpdate(Episodes.buildEpisodesOfShowUri(showTvdbId))
                .withSelection(Episodes.NUMBER + "=? AND " + Episodes.SEASON + "=?", new String[] {
                        String.valueOf(episode.number), String.valueOf(episode.season)
                }).withValue(Episodes.COLLECTED, true).build());
    }

    /**
     * Queries for an episode id and adds a content provider op to set it as
     * last watched for the given show.
     */
    private void addLastWatchedUpdateOp(ContentResolver resolver,
            ArrayList<ContentProviderOperation> batch, int season, int number, String showTvdbId) {
        // query for the episode id
        final Cursor episode = resolver.query(
                Episodes.buildEpisodesOfShowUri(showTvdbId),
                new String[] {
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
