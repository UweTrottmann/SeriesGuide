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

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.thetvdbapi.TheTVDB;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.entities.TvShow;
import com.jakewharton.trakt.enumerations.ExtendedParam;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Adds shows to the local database, tries to get watched and collected episodes
 * if a trakt account is connected.
 */
public class AddShowTask extends AsyncTask<Void, Integer, Void> {

    private static final int ADD_ALREADYEXISTS = 0;

    private static final int ADD_SUCCESS = 1;

    private static final int ADD_SAXERROR = 2;

    private static final int ADD_OFFLINE = 3;

    final private Context mContext;

    final private LinkedList<SearchResult> mAddQueue = new LinkedList<SearchResult>();

    private boolean mIsFinishedAddingShows = false;

    private String mCurrentShowName;

    private boolean mIsSilentMode;

    public AddShowTask(Context context, List<SearchResult> shows, boolean isSilentMode) {
        // use an activity independent context
        mContext = context.getApplicationContext();
        mAddQueue.addAll(shows);
        mIsSilentMode = isSilentMode;
    }

    /**
     * Adds shows to the add queue. If this returns false, the shows were not
     * added because the task is finishing up. Create a new one instead.
     */
    public boolean addShows(List<SearchResult> show) {
        if (mIsFinishedAddingShows) {
            return false;
        } else {
            mAddQueue.addAll(show);
            return true;
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        // don't even get started
        if (mAddQueue.isEmpty()) {
            return null;
        }

        int result;
        boolean modifiedDB = false;

        if (!AndroidUtils.isNetworkConnected(mContext)) {
            publishProgress(ADD_OFFLINE);
            return null;
        }

        if (isCancelled()) {
            return null;
        }

        // get watched episodes from trakt (if enabled/possible)
        // already here, so we only have to get it once
        List<TvShow> watched = new ArrayList<TvShow>();
        List<TvShow> collection = new ArrayList<TvShow>();
        if (ServiceUtils.isTraktCredentialsValid(mContext)) {
            ServiceManager manager = ServiceUtils.getTraktServiceManagerWithAuth(mContext, false);
            if (manager != null) {
                try {
                    watched = manager.userService()
                            .libraryShowsWatched(ServiceUtils.getTraktUsername(mContext))
                            .extended(ExtendedParam.Min).fire();

                    collection = manager.userService()
                            .libraryShowsCollection(ServiceUtils.getTraktUsername(mContext))
                            .extended(ExtendedParam.Min).fire();
                } catch (ApiException e) {
                    // something went wrong, just go on
                } catch (TraktException e) {

                }
            }
        }

        while (!mAddQueue.isEmpty()) {
            if (isCancelled()) {
                // only cancelled on config change, so don't rebuild fts
                // table yet
                return null;
            }

            if (!AndroidUtils.isNetworkConnected(mContext)) {
                publishProgress(ADD_OFFLINE);
                break;
            }

            SearchResult nextShow = mAddQueue.removeFirst();

            try {
                if (TheTVDB.addShow(nextShow.tvdbid, watched, collection, mContext)) {
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
        if (mIsSilentMode) {
            return;
        }

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
