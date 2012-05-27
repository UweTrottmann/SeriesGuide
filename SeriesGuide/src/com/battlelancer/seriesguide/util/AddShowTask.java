
package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.thetvdbapi.TheTVDB;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.entities.TvShow;
import com.jakewharton.trakt.enumerations.ExtendedParam;

import org.xml.sax.SAXException;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AddShowTask extends AsyncTask<Void, Integer, Void> {

    private static final int ADD_ALREADYEXISTS = 0;

    private static final int ADD_SUCCESS = 1;

    private static final int ADD_SAXERROR = 2;

    private static final int ADD_OFFLINE = 3;

    final private Context mContext;

    final private LinkedList<SearchResult> mAddQueue = new LinkedList<SearchResult>();

    private boolean mIsFinishedAddingShows = false;

    private String mCurrentShowName;

    public AddShowTask(Context context, SearchResult show) {
        mContext = context.getApplicationContext();
        mAddQueue.add(show);
    }

    public AddShowTask(Context context, LinkedList<SearchResult> shows) {
        mContext = context.getApplicationContext();
        mAddQueue.addAll(shows);
    }

    public LinkedList<SearchResult> getRemainingShows() {
        return mAddQueue;
    }

    /**
     * Add a show to the add queue. If this returns false, the show was not
     * added because the task is finishing up. Create a new one instead.
     * 
     * @param show
     * @return
     */
    public boolean addShow(SearchResult show) {
        if (mIsFinishedAddingShows) {
            return false;
        } else {
            mAddQueue.add(show);
            return true;
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        int result;
        boolean modifiedDB = false;

        if (!Utils.isNetworkConnected(mContext)) {
            publishProgress(ADD_OFFLINE);
            return null;
        }

        if (isCancelled()) {
            return null;
        }

        // get watched episodes from trakt (if enabled/possible)
        // already here, so we only have to get it once
        List<TvShow> shows = new ArrayList<TvShow>();
        if (Utils.isTraktCredentialsValid(mContext)) {
            try {
                ServiceManager manager = Utils.getServiceManagerWithAuth(mContext, false);

                shows = manager.userService().libraryShowsWatched(Utils.getTraktUsername(mContext))
                        .extended(ExtendedParam.Min).fire();
            } catch (Exception e1) {
                // something went wrong, just go on
            }
        }

        while (!mAddQueue.isEmpty()) {
            if (isCancelled()) {
                // only cancelled on config change, so don't rebuild fts
                // table yet
                return null;
            }

            if (!Utils.isNetworkConnected(mContext)) {
                publishProgress(ADD_OFFLINE);
                break;
            }

            SearchResult nextShow = mAddQueue.removeFirst();

            try {
                if (TheTVDB.addShow(nextShow.tvdbid, shows, mContext)) {
                    // success
                    result = ADD_SUCCESS;
                } else {
                    // already exists
                    result = ADD_ALREADYEXISTS;
                }
                modifiedDB = true;
            } catch (SAXException e) {
                result = ADD_SAXERROR;
            }

            mCurrentShowName = nextShow.title;
            publishProgress(result);
        }

        mIsFinishedAddingShows = true;
        // renew FTS3 table
        if (modifiedDB) {
            TheTVDB.onRenewFTSTable(mContext);
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        switch (values[0]) {
            case ADD_SUCCESS:
                Toast.makeText(mContext,
                        "\"" + mCurrentShowName + "\" " + mContext.getString(R.string.add_success),
                        Toast.LENGTH_SHORT).show();
                break;
            case ADD_ALREADYEXISTS:
                Toast.makeText(
                        mContext,
                        "\"" + mCurrentShowName + "\" "
                                + mContext.getString(R.string.add_already_exists),
                        Toast.LENGTH_LONG).show();
                break;
            case ADD_SAXERROR:
                Toast.makeText(
                        mContext,
                        mContext.getString(R.string.add_error_begin) + mCurrentShowName
                                + mContext.getString(R.string.add_error_end), Toast.LENGTH_LONG)
                        .show();
                break;
            case ADD_OFFLINE:
                Toast.makeText(mContext, R.string.offline, Toast.LENGTH_LONG).show();
                break;
        }
    }
}
