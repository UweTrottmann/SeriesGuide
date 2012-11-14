/*
 * Copyright 2011 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.battlelancer.seriesguide.SeriesGuideApplication;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.thetvdbapi.TheTVDB;
import com.google.analytics.tracking.android.EasyTracker;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.entities.Activity;
import com.jakewharton.trakt.entities.ActivityItem;
import com.jakewharton.trakt.entities.TvShowEpisode;
import com.jakewharton.trakt.enumerations.ActivityAction;
import com.jakewharton.trakt.enumerations.ActivityType;
import com.uwetrottmann.seriesguide.R;

import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

enum UpdateResult {
    SUCCESS, SILENT_SUCCESS, ERROR, OFFLINE, CANCELLED;
}

enum UpdateType {
    SINGLE, DELTA, FULL
}

public class UpdateTask extends AsyncTask<Void, Integer, UpdateResult> {

    private static final String TAG = "UpdateTask";

    private Context mAppContext;

    private String[] mShows = null;

    private final AtomicInteger mUpdateCount = new AtomicInteger();

    private String mFailedShows = "";

    private UpdateType mUpdateType;

    private String mCurrentShowName;

    private NotificationManager mNotificationManager;

    private Notification mNotification;

    private static final int UPDATE_NOTIFICATION_ID = 1;

    public UpdateTask(boolean isFullUpdate, Context context) {
        mAppContext = context.getApplicationContext();
        if (isFullUpdate) {
            mUpdateType = UpdateType.FULL;
        } else {
            mUpdateType = UpdateType.DELTA;
        }
    }

    public UpdateTask(String showId, Context context) {
        mAppContext = context.getApplicationContext();
        mShows = new String[] {
                showId
        };
        mUpdateType = UpdateType.SINGLE;
    }

    public UpdateTask(String[] showIds, int index, String failedShows, Context context) {
        mAppContext = context.getApplicationContext();
        mShows = showIds;
        mUpdateCount.set(index);
        mFailedShows = failedShows;
        mUpdateType = UpdateType.DELTA;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onPreExecute() {
        // create a notification (holy crap is that a lot of code)
        String ns = Context.NOTIFICATION_SERVICE;
        mNotificationManager = (NotificationManager) mAppContext.getSystemService(ns);

        CharSequence tickerText = mAppContext.getString(R.string.update_notification);
        long when = System.currentTimeMillis();
        final int icon = R.drawable.stat_sys_download;

        mNotification = new Notification(icon, tickerText, when);
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR
                | Notification.FLAG_ONLY_ALERT_ONCE;

        // content view
        RemoteViews contentView = new RemoteViews(mAppContext.getPackageName(),
                R.layout.update_notification);
        contentView.setTextViewText(R.id.text, mAppContext.getString(R.string.update_notification));
        contentView.setProgressBar(R.id.progressbar, 0, 0, true);
        mNotification.contentView = contentView;

        // content intent
        Intent notificationIntent = new Intent(mAppContext, ShowsActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(mAppContext, 0, notificationIntent,
                0);
        mNotification.contentIntent = contentIntent;

        mNotificationManager.notify(UPDATE_NOTIFICATION_ID, mNotification);
    }

    @Override
    protected UpdateResult doInBackground(Void... params) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mAppContext);
        final ContentResolver resolver = mAppContext.getContentResolver();
        final AtomicInteger updateCount = mUpdateCount;
        long currentTime = System.currentTimeMillis();

        // build a list of shows to update
        if (mShows == null) {
            switch (mUpdateType) {
                case FULL:
                    // get all show IDs for a full update
                    final Cursor shows = resolver.query(Shows.CONTENT_URI, new String[] {
                            Shows._ID
                    }, null, null, null);

                    mShows = new String[shows.getCount()];
                    int i = 0;
                    while (shows.moveToNext()) {
                        mShows[i] = shows.getString(0);
                        i++;
                    }

                    shows.close();
                    break;
                case DELTA:
                default:
                    // get only shows which have not been updated for a certain
                    // time
                    mShows = TheTVDB.deltaUpdateShows(currentTime, prefs, mAppContext);
                    break;
            }
        }

        final int maxProgress = mShows.length + 2;
        UpdateResult resultCode = UpdateResult.SUCCESS;
        String id;

        // actually update the shows
        for (int i = updateCount.get(); i < mShows.length; i++) {
            // skip ahead if we get cancelled or connectivity is
            // lost/forbidden
            if (isCancelled()) {
                resultCode = UpdateResult.CANCELLED;
                break;
            }
            if (!Utils.isAllowedConnection(mAppContext)) {
                resultCode = UpdateResult.OFFLINE;
                break;
            }

            id = mShows[i];
            setCurrentShowName(resolver, id);

            publishProgress(i, maxProgress);

            for (int itry = 0; itry < 2; itry++) {
                // skip ahead if we get cancelled or connectivity is
                // lost/forbidden
                if (isCancelled()) {
                    resultCode = UpdateResult.CANCELLED;
                    break;
                }
                if (!Utils.isAllowedConnection(mAppContext)) {
                    resultCode = UpdateResult.OFFLINE;
                    break;
                }

                try {
                    TheTVDB.updateShow(id, mAppContext);
                    break;
                } catch (SAXException e) {
                    if (itry == 1) {
                        // failed twice, give up
                        resultCode = UpdateResult.ERROR;
                        addFailedShow(mCurrentShowName);
                        Utils.trackExceptionAndLog(mAppContext, TAG, e);
                    }
                }
            }

            updateCount.incrementAndGet();
        }

        // do not refresh search table and load trakt activity on each single
        // auto update will run anyhow
        if (mUpdateType != UpdateType.SINGLE) {
            // try to avoid renewing the search table as it is time consuming
            if (updateCount.get() > 0 && mShows.length > 0) {
                publishProgress(mShows.length, maxProgress);
                TheTVDB.onRenewFTSTable(mAppContext);
            }

            // mark episodes based on trakt activity
            final UpdateResult traktResult = getTraktActivity(prefs, maxProgress, currentTime);
            // do not overwrite earlier failure codes
            if (resultCode == UpdateResult.SUCCESS) {
                resultCode = traktResult;
            }

            publishProgress(maxProgress, maxProgress);

            // update the latest episodes
            Utils.updateLatestEpisodes(mAppContext);

            // store time for triggering next update 15min after
            if (resultCode == UpdateResult.SUCCESS) {
                // now, if we were successful, reset failed counter
                prefs.edit().putLong(SeriesGuidePreferences.KEY_LASTUPDATE, currentTime)
                        .putInt(SeriesGuidePreferences.KEY_FAILED_COUNTER, 0).commit();
            } else {
                int failed = prefs.getInt(SeriesGuidePreferences.KEY_FAILED_COUNTER, 0);

                // back off by the power of 2 times minutes
                long time;
                if (failed < 4) {
                    time = currentTime
                            - ((15 - (int) Math.pow(2, failed)) * DateUtils.MINUTE_IN_MILLIS);
                } else {
                    time = currentTime;
                }

                failed += 1;
                prefs.edit()
                        .putLong(SeriesGuidePreferences.KEY_LASTUPDATE, time)
                        .putInt(SeriesGuidePreferences.KEY_FAILED_COUNTER, failed).commit();
            }
        } else {
            publishProgress(maxProgress, maxProgress);
        }

        // do not display a disturbing info toast for specific updates
        if (mUpdateType == UpdateType.SINGLE && resultCode == UpdateResult.SUCCESS) {
            resultCode = UpdateResult.SILENT_SUCCESS;
        }

        return resultCode;
    }

    private UpdateResult getTraktActivity(SharedPreferences prefs, int maxProgress, long currentTime) {
        if (Utils.isTraktCredentialsValid(mAppContext)) {
            // return if we get cancelled or connectivity is lost/forbidden
            if (isCancelled()) {
                return UpdateResult.CANCELLED;
            }
            if (!Utils.isAllowedConnection(mAppContext)) {
                return UpdateResult.OFFLINE;
            }

            publishProgress(maxProgress - 1, maxProgress);

            // get last trakt update timestamp
            final long startTimeTrakt = prefs.getLong(SeriesGuidePreferences.KEY_LASTTRAKTUPDATE,
                    currentTime) / 1000;

            ServiceManager manager = Utils.getServiceManagerWithAuth(mAppContext, false);
            if (manager == null) {
                return UpdateResult.ERROR;
            }

            // get watched episodes from trakt
            Activity activity;
            try {
                activity = manager
                        .activityService()
                        .user(Utils.getTraktUsername(mAppContext))
                        .types(ActivityType.Episode)
                        .actions(ActivityAction.Checkin, ActivityAction.Seen,
                                ActivityAction.Scrobble, ActivityAction.Collection)
                        .timestamp(startTimeTrakt).fire();
            } catch (TraktException e) {
                Utils.trackExceptionAndLog(mAppContext, TAG, e);
                return UpdateResult.ERROR;
            } catch (ApiException e) {
                Utils.trackExceptionAndLog(mAppContext, TAG, e);
                return UpdateResult.ERROR;
            }

            if (activity == null) {
                return UpdateResult.ERROR;
            }

            // build an update batch
            final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
            for (ActivityItem item : activity.activity) {
                // check for null (potential fix for reported crash)
                if (item.action != null && item.show != null) {
                    switch (item.action) {
                        case Seen: {
                            // seen uses an array of episodes
                            List<TvShowEpisode> episodes = item.episodes;
                            for (TvShowEpisode episode : episodes) {
                                addEpisodeSeenOp(batch, episode, item.show.tvdbId);
                            }
                            break;
                        }
                        case Checkin:
                        case Scrobble: {
                            // checkin and scrobble use a single episode
                            TvShowEpisode episode = item.episode;
                            addEpisodeSeenOp(batch, episode, item.show.tvdbId);
                            break;
                        }
                        case Collection: {
                            // collection uses an array of episodes
                            List<TvShowEpisode> episodes = item.episodes;
                            for (TvShowEpisode episode : episodes) {
                                addEpisodeCollectedOp(batch, episode, item.show.tvdbId);
                            }
                            break;
                        }
                        default:
                            break;
                    }
                }
            }

            // execute the batch
            try {
                mAppContext.getContentResolver()
                        .applyBatch(SeriesGuideApplication.CONTENT_AUTHORITY, batch);
            } catch (RemoteException e) {
                // Failed binder transactions aren't recoverable
                Utils.trackExceptionAndLog(mAppContext, TAG, e);
                throw new RuntimeException("Problem applying batch operation", e);
            } catch (OperationApplicationException e) {
                // Failures like constraint violation aren't
                // recoverable
                Utils.trackExceptionAndLog(mAppContext, TAG, e);
                throw new RuntimeException("Problem applying batch operation", e);
            }

            // store time of this update as seen by the trakt server
            prefs.edit()
                    .putLong(SeriesGuidePreferences.KEY_LASTTRAKTUPDATE,
                            activity.timestamps.current.getTime()).commit();

        }

        return UpdateResult.SUCCESS;
    }

    private void setCurrentShowName(final ContentResolver resolver, String id) {
        Cursor show = resolver.query(Shows.buildShowUri(id), new String[] {
                Shows.TITLE
        }, null, null, null);
        if (show.moveToFirst()) {
            mCurrentShowName = show.getString(0);
        }
        show.close();
    }

    @Override
    protected void onPostExecute(UpdateResult result) {
        String message = null;
        int length = 0;
        switch (result) {
            case SUCCESS:
                message = mAppContext.getString(R.string.update_success);
                length = Toast.LENGTH_SHORT;
                // fall through one case here
            case SILENT_SUCCESS:
                fireTrackerEvent("Success");
                break;
            case ERROR:
                message = mAppContext.getString(R.string.update_saxerror);
                length = Toast.LENGTH_LONG;

                fireTrackerEvent("Error");
                break;
            case OFFLINE:
                message = mAppContext.getString(R.string.update_offline);
                length = Toast.LENGTH_LONG;

                fireTrackerEvent("Offline");
                break;
            default:
                break;
        }

        if (message != null) {
            // add a list of failed shows
            if (mFailedShows.length() != 0) {
                message += "(" + mFailedShows + ")";
            }
            Toast.makeText(mAppContext, message, length).show();
        }
        mNotificationManager.cancel(UPDATE_NOTIFICATION_ID);
        TaskManager.getInstance(mAppContext).onTaskCompleted();
    }

    @Override
    protected void onCancelled() {
        mNotificationManager.cancel(UPDATE_NOTIFICATION_ID);
        TaskManager.getInstance(mAppContext).onTaskCompleted();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        String text;
        if (values[0] == values[1]) {
            // clear the text field if we are finishing up
            text = "";
        } else if (values[0] + 1 == values[1]) {
            // if we're one before completion, we're looking on trakt for user
            // activity
            text = mAppContext.getString(R.string.update_traktactivity);
        } else if (values[0] + 2 == values[1]) {
            // if we're two before completion, we're rebuilding the search index
            text = mAppContext.getString(R.string.update_rebuildsearch);
        } else {
            text = mCurrentShowName + "...";
        }

        mNotification.contentView.setTextViewText(R.id.text, text);
        mNotification.contentView.setProgressBar(R.id.progressbar, values[1], values[0], false);
        mNotificationManager.notify(UPDATE_NOTIFICATION_ID, mNotification);
    }

    private void addFailedShow(String seriesName) {
        if (mFailedShows.length() != 0) {
            mFailedShows += ", ";
        }
        mFailedShows += seriesName;
    }

    private static void addEpisodeSeenOp(final ArrayList<ContentProviderOperation> batch,
            TvShowEpisode episode, String showTvdbId) {
        batch.add(ContentProviderOperation.newUpdate(Episodes.buildEpisodesOfShowUri(showTvdbId))
                .withSelection(Episodes.NUMBER + "=? AND " + Episodes.SEASON + "=?", new String[] {
                        String.valueOf(episode.number), String.valueOf(episode.season)
                }).withValue(Episodes.WATCHED, true).build());
    }

    private static void addEpisodeCollectedOp(ArrayList<ContentProviderOperation> batch,
            TvShowEpisode episode, String showTvdbId) {
        batch.add(ContentProviderOperation.newUpdate(Episodes.buildEpisodesOfShowUri(showTvdbId))
                .withSelection(Episodes.NUMBER + "=? AND " + Episodes.SEASON + "=?", new String[] {
                        String.valueOf(episode.number), String.valueOf(episode.season)
                }).withValue(Episodes.COLLECTED, true).build());
    }

    private void fireTrackerEvent(String message) {
        EasyTracker.getTracker().trackEvent(TAG, "Update result", message, (long) 0);
    }

}
