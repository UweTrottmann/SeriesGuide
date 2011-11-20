
package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.thetvdbapi.TheTVDB;

import org.xml.sax.SAXException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import java.util.concurrent.atomic.AtomicInteger;

public class UpdateTask extends AsyncTask<Void, Integer, Integer> {

    private static final int UPDATE_SUCCESS = 100;

    private static final int UPDATE_SAXERROR = 102;

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
        String ns = Context.NOTIFICATION_SERVICE;
        mNotificationManager = (NotificationManager) mAppContext.getSystemService(ns);

        int icon = R.drawable.icon;
        CharSequence tickerText = "Updating your shows...";
        long when = System.currentTimeMillis();

        mNotification = new Notification(icon, tickerText, when);
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

        RemoteViews contentView = new RemoteViews(mAppContext.getPackageName(),
                R.layout.update_notification);
        contentView.setImageViewResource(R.id.image, icon);
        contentView.setTextViewText(R.id.text, "Looking for updates...");
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
                    return UPDATE_SAXERROR;
                }
            }
        }

        int resultCode = UPDATE_SUCCESS;
        String id;

        for (int i = updateCount.get(); i < mShows.length; i++) {
            // fail early if cancelled or network connection is lost
            if (isCancelled()) {
                resultCode = UPDATE_INCOMPLETE;
                break;
            }
            if (!Utils.isNetworkAvailable(mAppContext)) {
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

            publishProgress(i, mShows.length + 1);

            for (int itry = 0; itry < 2; itry++) {
                try {
                    TheTVDB.updateShow(id, mAppContext);
                    break;
                } catch (SAXException saxe) {
                    // failed twice
                    if (itry == 1) {
                        resultCode = UPDATE_SAXERROR;
                        addFailedShow(mCurrentShowName);
                    }
                }
            }
            updateCount.incrementAndGet();
        }

        // renew FTS3 table (only if we updated shows)
        if (mShows.length != 0) {
            publishProgress(mShows.length, mShows.length + 1);
            TheTVDB.onRenewFTSTable(mAppContext);
        }

        publishProgress(mShows.length + 1, mShows.length + 1);

        // store time of update if it was successful
        if (currentTime != 0 && resultCode == UPDATE_SUCCESS) {
            prefs.edit().putLong(SeriesGuidePreferences.KEY_LASTUPDATE, currentTime).commit();
        }

        return resultCode;
    }

    @Override
    protected void onPostExecute(Integer result) {
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
            // if we're one before completion, we're rebuilding the search index
            text = mAppContext.getString(R.string.update_rebuildsearch) + "...";
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

}
