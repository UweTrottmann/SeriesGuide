
package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SeriesGuideData;
import com.battlelancer.seriesguide.SeriesGuidePreferences;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.thetvdbapi.TheTVDB;

import org.xml.sax.SAXException;

import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewStub;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.atomic.AtomicInteger;

public class UpdateTask extends AsyncTask<Void, Integer, Integer> {

    private static final int UPDATE_SUCCESS = 100;

    private static final int UPDATE_SAXERROR = 102;

    private static final int UPDATE_OFFLINE = 103;

    private static final int UPDATE_INCOMPLETE = 104;

    public String[] mShows = null;

    public String mFailedShows = "";

    private final ShowsActivity mShowsActivity;

    public final AtomicInteger mUpdateCount = new AtomicInteger();

    private boolean mIsFullUpdate = false;

    private View mProgressOverlay;

    private ProgressBar mUpdateProgress;

    private String mCurrentShowName;

    private TextView mUpdateStatus;

    public UpdateTask(boolean isFullUpdate, ShowsActivity context) {
        mShowsActivity = context;
        mIsFullUpdate = isFullUpdate;
    }

    public UpdateTask(String[] shows, int index, String failedShows, ShowsActivity context) {
        mShowsActivity = context;
        mShows = shows;
        mUpdateCount.set(index);
        mFailedShows = failedShows;
    }

    @Override
    protected void onPreExecute() {
        // see if we already inflated the progress overlay
        mProgressOverlay = mShowsActivity.findViewById(R.id.overlay_update);
        if (mProgressOverlay == null) {
            mProgressOverlay = ((ViewStub) mShowsActivity.findViewById(R.id.stub_update)).inflate();
        }
        mShowsActivity.showOverlay(mProgressOverlay);
    }

    @Override
    protected Integer doInBackground(Void... params) {
        // setup the progress overlay
        mUpdateProgress = (ProgressBar) mProgressOverlay.findViewById(R.id.ProgressBarShowListDet);
        mUpdateProgress.setIndeterminate(true);

        mUpdateStatus = (TextView) mProgressOverlay.findViewById(R.id.textViewUpdateStatus);
        mUpdateStatus.setText("");

        final View cancelButton = mProgressOverlay.findViewById(R.id.overlayCancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mShowsActivity.onCancelTasks();
            }
        });

        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(mShowsActivity.getApplicationContext());
        final ContentResolver resolver = mShowsActivity.getContentResolver();
        final AtomicInteger updateCount = mUpdateCount;
        long currentServerTime = 0;
        if (mShows == null) {

            try {
                currentServerTime = TheTVDB.getServerTime(mShowsActivity);
            } catch (SAXException e1) {
                return UPDATE_SAXERROR;
            }
            final long previousUpdateTime = Long.valueOf(prefs.getString(
                    SeriesGuidePreferences.KEY_LASTUPDATETIME, "0"));

            // new update task
            if (mIsFullUpdate || isFullUpdateNeeded(currentServerTime, previousUpdateTime)) {
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
                    mShows = TheTVDB.deltaUpdateShows(previousUpdateTime, mShowsActivity);
                } catch (SAXException e) {
                    return UPDATE_SAXERROR;
                }
            }
        }

        int resultCode = UPDATE_SUCCESS;
        String id;
        mUpdateProgress.setIndeterminate(false);

        for (int i = updateCount.get(); i < mShows.length; i++) {
            // fail early if cancelled or network connection is lost
            if (isCancelled()) {
                resultCode = UPDATE_INCOMPLETE;
                break;
            }
            if (!SeriesGuideData.isNetworkAvailable(mShowsActivity)) {
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
                    TheTVDB.updateShow(id, mShowsActivity);
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

        publishProgress(mShows.length, mShows.length + 1);

        // renew FTS3 table
        TheTVDB.onRenewFTSTable(mShowsActivity);

        publishProgress(mShows.length + 1, mShows.length + 1);

        // store time of update if it was successful
        if (currentServerTime != 0 && resultCode == UPDATE_SUCCESS) {
            prefs.edit()
                    .putString(SeriesGuidePreferences.KEY_LASTUPDATETIME,
                            String.valueOf(currentServerTime)).commit();
        }

        return resultCode;
    }

    private boolean isFullUpdateNeeded(long currentServerTime, long previousUpdateTime) {
        // check if more than 28 days have passed
        // we compare with local time to avoid an additional network call
        if (currentServerTime - previousUpdateTime > 3600 * 24 * 28) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
        mShowsActivity.setFailedShowsString(mFailedShows);

        switch (result) {
            case UPDATE_SUCCESS:
                AnalyticsUtils.getInstance(mShowsActivity).trackEvent("Shows", "Update Task",
                        "Success", 0);

                Toast.makeText(mShowsActivity, mShowsActivity.getString(R.string.update_success),
                        Toast.LENGTH_SHORT).show();
                break;
            case UPDATE_SAXERROR:
                AnalyticsUtils.getInstance(mShowsActivity).trackEvent("Shows", "Update Task",
                        "SAX error", 0);

                mShowsActivity.showDialog(ShowsActivity.UPDATE_SAXERROR_DIALOG);
                break;
            case UPDATE_OFFLINE:
                AnalyticsUtils.getInstance(mShowsActivity).trackEvent("Shows", "Update Task",
                        "Offline", 0);

                mShowsActivity.showDialog(ShowsActivity.UPDATE_OFFLINE_DIALOG);
                break;
        }

        mShowsActivity.updateLatestEpisode();
        mShowsActivity.hideOverlay(mProgressOverlay);
    }

    @Override
    protected void onCancelled() {
        mShowsActivity.hideOverlay(mProgressOverlay);
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        mUpdateProgress.setMax(values[1]);
        mUpdateProgress.setProgress(values[0]);
        if (values[0] == values[1]) {
            // clear the text field if we are finishing up
            mUpdateStatus.setText("");
        } else if (values[0] + 1 == values[1]) {
            // if we're one before completion, we're rebuilding the search index
            mUpdateStatus.setText(mShowsActivity.getString(R.string.update_rebuildsearch) + "...");
        } else {
            mUpdateStatus.setText(mCurrentShowName + "...");
        }
    }

    private void addFailedShow(String seriesName) {
        if (mFailedShows.length() != 0) {
            mFailedShows += ", ";
        }
        mFailedShows += seriesName;
    }

}
