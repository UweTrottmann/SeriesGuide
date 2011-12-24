
package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesContract;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.thetvdbapi.TheTVDB;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.entities.Activity;
import com.jakewharton.trakt.entities.ActivityItem;
import com.jakewharton.trakt.entities.TvShowEpisode;
import com.jakewharton.trakt.enumerations.ActivityAction;
import com.jakewharton.trakt.enumerations.ActivityType;

import org.xml.sax.SAXException;

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
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class UpdateTask extends AsyncTask<Void, Integer, Integer> {

    private static final int UPDATE_SUCCESS = 100;

    private static final int UPDATE_ERROR = 102;

    private static final int UPDATE_OFFLINE = 103;

    private static final int UPDATE_INCOMPLETE = 104;

    public String[] mShows = null;

    public String mFailedShows = "";

    private Context mAppContext;

    public final AtomicInteger mUpdateCount = new AtomicInteger();

    private boolean mIsFullUpdate = false;

    private String mCurrentShowName;

    private NotificationManager mNotificationManager;

    private Notification mNotification;

    private static final int UPDATE_NOTIFICATION_ID = 1;

    public UpdateTask(boolean isFullUpdate, Context context) {
        mAppContext = context.getApplicationContext();
        mIsFullUpdate = isFullUpdate;
    }

    public UpdateTask(String[] shows, int index, String failedShows, Context context) {
        mAppContext = context.getApplicationContext();
        mShows = shows;
        mUpdateCount.set(index);
        mFailedShows = failedShows;
    }

    @Override
    protected void onPreExecute() {
        // create a notification (holy crap is that a lot of code)
        String ns = Context.NOTIFICATION_SERVICE;
        mNotificationManager = (NotificationManager) mAppContext.getSystemService(ns);

        int icon = R.drawable.icon;
        CharSequence tickerText = mAppContext.getString(R.string.update_notification);
        long when = System.currentTimeMillis();

        mNotification = new Notification(icon, tickerText, when);
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR
                | Notification.FLAG_ONLY_ALERT_ONCE;

        RemoteViews contentView = new RemoteViews(mAppContext.getPackageName(),
                R.layout.update_notification);
        contentView.setImageViewResource(R.id.image, icon);
        contentView.setTextViewText(R.id.text, mAppContext.getString(R.string.update_notification));
        contentView.setProgressBar(R.id.progressbar, 0, 0, true);
        mNotification.contentView = contentView;

        Intent notificationIntent = new Intent(mAppContext, ShowsActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(mAppContext, 0, notificationIntent,
                0);
        mNotification.contentIntent = contentIntent;

        mNotificationManager.notify(UPDATE_NOTIFICATION_ID, mNotification);
    }

    @Override
    protected Integer doInBackground(Void... params) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mAppContext);
        final ContentResolver resolver = mAppContext.getContentResolver();
        final AtomicInteger updateCount = mUpdateCount;
        final boolean isAutoUpdateWlanOnly = prefs.getBoolean(
                SeriesGuidePreferences.KEY_AUTOUPDATEWLANONLY, true);
        long currentTime = 0;

        if (mShows == null) {
            currentTime = System.currentTimeMillis();
            final int updateAtLeastEvery = prefs.getInt(
                    SeriesGuidePreferences.KEY_UPDATEATLEASTEVERY, 7);

            // new update task
            if (mIsFullUpdate) {
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
            } else {
                try {
                    mShows = TheTVDB.deltaUpdateShows(currentTime, updateAtLeastEvery, mAppContext);
                } catch (SAXException e) {
                    return UPDATE_ERROR;
                }
            }
        }

        final int maxProgress = mShows.length + 2;
        int resultCode = UPDATE_SUCCESS;
        String id;

        for (int i = updateCount.get(); i < mShows.length; i++) {
            // fail early if cancelled or network connection is lost or wifi is
            // disconnected
            if (isCancelled()) {
                resultCode = UPDATE_INCOMPLETE;
                break;
            }
            if (!Utils.isNetworkConnected(mAppContext)
                    || (isAutoUpdateWlanOnly && !Utils.isWifiConnected(mAppContext))) {
                resultCode = UPDATE_OFFLINE;
                break;
            }

            id = mShows[i];

            Cursor show = resolver.query(Shows.buildShowUri(id), new String[] {
                Shows.TITLE
            }, null, null, null);
            if (show.moveToFirst()) {
                mCurrentShowName = show.getString(0);
            }
            show.close();

            publishProgress(i, maxProgress);

            for (int itry = 0; itry < 2; itry++) {
                try {
                    TheTVDB.updateShow(id, mAppContext);
                    break;
                } catch (SAXException saxe) {
                    // failed twice
                    if (itry == 1) {
                        resultCode = UPDATE_ERROR;
                        addFailedShow(mCurrentShowName);
                    }
                }
            }
            updateCount.incrementAndGet();
        }

        // renew FTS3 table (only if we updated shows)
        if (updateCount.get() != 0 && mShows.length != 0) {
            publishProgress(mShows.length, maxProgress);
            TheTVDB.onRenewFTSTable(mAppContext);
        }

        // look on trakt for recent activity
        if (prefs.getBoolean(SeriesGuidePreferences.KEY_INTEGRATETRAKT, true)
                && ShareUtils.isTraktCredentialsValid(mAppContext)) {
            publishProgress(maxProgress - 1, maxProgress);

            ServiceManager manager;
            try {
                manager = Utils.getServiceManagerWithAuth(mAppContext, false);
            } catch (Exception e) {
                return UPDATE_ERROR;
            }

            // get last trakt update timestamp
            final long startTimeTrakt = prefs.getLong(SeriesGuidePreferences.KEY_LASTTRAKTUPDATE,
                    currentTime) / 1000;

            // get watched episodes from trakt
            Activity activity;
            try {
                activity = manager
                        .activityService()
                        .user(Utils.getTraktUsername(mAppContext))
                        .types(ActivityType.Episode)
                        .actions(ActivityAction.Checkin, ActivityAction.Seen,
                                ActivityAction.Scrobble).timestamp(startTimeTrakt).fire();
            } catch (TraktException te) {
                return UPDATE_ERROR;
            } catch (ApiException ae) {
                return UPDATE_ERROR;
            }

            // build an update batch
            final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
            for (ActivityItem item : activity.activity) {
                switch (item.action) {
                    case Seen: {
                        List<TvShowEpisode> episodes = item.episodes;
                        String showTvdbId = item.show.tvdbId;
                        for (TvShowEpisode episode : episodes) {
                            addEpisodeOp(batch, episode, showTvdbId);
                        }
                        break;
                    }
                    case Checkin:
                    case Scrobble: {
                        TvShowEpisode episode = item.episode;
                        String showTvdbId = item.show.tvdbId;
                        addEpisodeOp(batch, episode, showTvdbId);
                        break;
                    }
                }
            }

            // execute the batch
            try {
                mAppContext.getContentResolver()
                        .applyBatch(SeriesContract.CONTENT_AUTHORITY, batch);
            } catch (RemoteException e) {
                // Failed binder transactions aren't recoverable
                throw new RuntimeException("Problem applying batch operation", e);
            } catch (OperationApplicationException e) {
                // Failures like constraint violation aren't
                // recoverable
                throw new RuntimeException("Problem applying batch operation", e);
            }

            // store time of this update as seen by the trakt server
            prefs.edit()
                    .putLong(SeriesGuidePreferences.KEY_LASTTRAKTUPDATE,
                            activity.timestamps.current.getTime()).commit();

        }

        publishProgress(maxProgress, maxProgress);

        // update the latest episodes
        Utils.updateLatestEpisodes(mAppContext);

        // store time of update if it was successful
        if (currentTime != 0 && resultCode == UPDATE_SUCCESS) {
            prefs.edit().putLong(SeriesGuidePreferences.KEY_LASTUPDATE, currentTime).commit();
        }

        return resultCode;
    }

    @Override
    protected void onPostExecute(Integer result) {
        String message = null;
        int length = 0;
        switch (result) {
            case UPDATE_SUCCESS:
                AnalyticsUtils.getInstance(mAppContext).trackEvent("Shows", "Update Task",
                        "Success", 0);

                message = mAppContext.getString(R.string.update_success);
                length = Toast.LENGTH_SHORT;

                break;
            case UPDATE_ERROR:
                AnalyticsUtils.getInstance(mAppContext).trackEvent("Shows", "Update Task",
                        "SAX error", 0);

                message = mAppContext.getString(R.string.update_failed) + " "
                        + mAppContext.getString(R.string.update_saxerror);
                length = Toast.LENGTH_LONG;
                break;
            case UPDATE_OFFLINE:
                AnalyticsUtils.getInstance(mAppContext).trackEvent("Shows", "Update Task",
                        "Offline", 0);

                message = mAppContext.getString(R.string.update_failed) + " "
                        + mAppContext.getString(R.string.update_offline);
                length = Toast.LENGTH_LONG;
                break;
        }

        if (message != null) {
            // add a list of failed shows
            if (mFailedShows.length() != 0){
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

    private static void addEpisodeOp(final ArrayList<ContentProviderOperation> batch,
            TvShowEpisode episode, String showTvdbId) {
        batch.add(ContentProviderOperation.newUpdate(Episodes.buildEpisodesOfShowUri(showTvdbId))
                .withSelection(Episodes.NUMBER + "=? AND " + Episodes.SEASON + "=?", new String[] {
                        String.valueOf(episode.number), String.valueOf(episode.season)
                }).withValue(Episodes.WATCHED, true).build());
    }

}
