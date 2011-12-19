
package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.thetvdbapi.SearchResult;
import com.battlelancer.thetvdbapi.TheTVDB;

import org.xml.sax.SAXException;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.util.LinkedList;

public class AddShowTask extends AsyncTask<Void, Integer, Void> {

    private static final int ADD_ALREADYEXISTS = 0;

    private static final int ADD_SUCCESS = 1;

    private static final int ADD_SAXERROR = 2;

    final private Context mContext;

    final private LinkedList<SearchResult> mAddQueue = new LinkedList<SearchResult>();

    private boolean mIsFinishedAddingShows = false;

    private String mCurrentShowName;

    public AddShowTask(Context activity, SearchResult show) {
        mContext = activity;
        mAddQueue.add(show);
    }

    public AddShowTask(Context activity, LinkedList<SearchResult> shows) {
        mContext = activity;
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

        while (!mAddQueue.isEmpty()) {
            if (isCancelled()) {
                // only cancelled on config change, so don't rebuild fts
                // table yet
                return null;
            }

            SearchResult nextShow = mAddQueue.removeFirst();

            try {
                if (TheTVDB.addShow(nextShow.tvdbid, mContext.getApplicationContext())) {
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
            TheTVDB.onRenewFTSTable(mContext.getApplicationContext());
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        switch (values[0]) {
            case ADD_SUCCESS:
                Toast.makeText(mContext.getApplicationContext(),
                        "\"" + mCurrentShowName + "\" " + mContext.getString(R.string.add_success),
                        Toast.LENGTH_SHORT).show();
                break;
            case ADD_ALREADYEXISTS:
                Toast.makeText(
                        mContext.getApplicationContext(),
                        "\"" + mCurrentShowName + "\" "
                                + mContext.getString(R.string.add_already_exists),
                        Toast.LENGTH_LONG).show();
                break;
            case ADD_SAXERROR:
                Toast.makeText(
                        mContext.getApplicationContext(),
                        mContext.getString(R.string.add_error_begin) + mCurrentShowName
                                + mContext.getString(R.string.add_error_end) + " ("
                                + mContext.getString(R.string.saxerror_title) + ")",
                        Toast.LENGTH_LONG).show();
                break;
        }
    }
}
