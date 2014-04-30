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

package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.thetvdbapi.TheTVDB;
import com.battlelancer.seriesguide.thetvdbapi.TvdbException;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.TvShow;
import com.jakewharton.trakt.enumerations.Extended;
import com.jakewharton.trakt.services.UserService;
import com.uwetrottmann.androidutils.AndroidUtils;
import de.greenrobot.event.EventBus;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * Adds shows to the local database, tries to get watched and collected episodes
 * if a trakt account is connected.
 */
public class AddShowTask extends AsyncTask<Void, Integer, Void> {

    public class OnShowAddedEvent {
        private String message;
        private int toastLength;

        public OnShowAddedEvent(String message, int toastLength) {
            this.message = message;
            this.toastLength = toastLength;
        }

        public void handle(Context context) {
            Toast.makeText(context, message, toastLength).show();
        }
    }

    private static final int ADD_ALREADYEXISTS = 0;

    private static final int ADD_SUCCESS = 1;

    private static final int ADD_ERROR = 2;

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
        Timber.d("Trying to add shows to queue...");
        if (mIsFinishedAddingShows) {
            Timber.d("FAILED. Already finishing up.");
            return false;
        } else {
            mAddQueue.addAll(show);
            Timber.d("SUCCESS.");
            return true;
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        Timber.d("Starting to add shows...");

        // don't even get started
        if (mAddQueue.isEmpty()) {
            Timber.d("Finished. Queue was empty.");
            return null;
        }

        int result;
        boolean modifiedDB = false;

        if (!AndroidUtils.isNetworkConnected(mContext)) {
            Timber.d("Finished. No internet connection.");
            publishProgress(ADD_OFFLINE);
            return null;
        }

        if (isCancelled()) {
            Timber.d("Finished. Cancelled.");
            return null;
        }

        // get watched episodes from trakt (if enabled/possible)
        // already here, so we only have to get it once
        List<TvShow> watched = new ArrayList<TvShow>();
        List<TvShow> collection = new ArrayList<TvShow>();
        Trakt manager = ServiceUtils.getTraktWithAuth(mContext);
        if (manager != null) {
            Timber.d("Getting watched and collected episodes from trakt.");
            String username = TraktCredentials.get(mContext).getUsername();
            try {
                UserService userService = manager.userService();
                watched = userService.libraryShowsWatched(username, Extended.MIN);
                collection = userService.libraryShowsCollection(username, Extended.MIN);
            } catch (RetrofitError e) {
                // something went wrong, continue anyhow
                Timber.w(e, "Getting watched and collected episodes failed");
            }
        }

        while (!mAddQueue.isEmpty()) {
            Timber.d("Starting to add next show...");
            if (isCancelled()) {
                Timber.d("Finished. Cancelled.");
                // only cancelled on config change, so don't rebuild fts
                // table yet
                return null;
            }

            if (!AndroidUtils.isNetworkConnected(mContext)) {
                Timber.d("Finished. No connection.");
                publishProgress(ADD_OFFLINE);
                break;
            }

            SearchResult nextShow = mAddQueue.removeFirst();

            try {
                if (TheTVDB.addShow(nextShow.tvdbid, watched, collection, mContext)) {
                    // success
                    result = ADD_SUCCESS;

                    // remove isRemoved flag on Hexagon
                    ShowTools.get(mContext).sendIsRemoved(nextShow.tvdbid, false);
                } else {
                    // already exists
                    result = ADD_ALREADYEXISTS;
                }
                modifiedDB = true;
            } catch (TvdbException e) {
                result = ADD_ERROR;
                Timber.e(e, "Adding show failed");
            }

            mCurrentShowName = nextShow.title;
            publishProgress(result);
            Timber.d("Finished adding show. (Result code: " + result + ")");
        }

        mIsFinishedAddingShows = true;
        // renew FTS3 table
        if (modifiedDB) {
            Timber.d("Renewing search table.");
            TheTVDB.onRenewFTSTable(mContext);
        }

        Timber.d("Finished adding shows.");
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (mIsSilentMode) {
            Timber.d("SILENT MODE: do not show progress toast");
            return;
        }

        OnShowAddedEvent event = null;
        switch (values[0]) {
            case ADD_SUCCESS:
                // do nothing, user will see show added to show list
                return;
            case ADD_ALREADYEXISTS:
                event = new OnShowAddedEvent(
                        mContext.getString(R.string.add_already_exists, mCurrentShowName),
                        Toast.LENGTH_LONG
                );
                break;
            case ADD_ERROR:
                event = new OnShowAddedEvent(
                        mContext.getString(R.string.add_error, mCurrentShowName),
                        Toast.LENGTH_LONG);
                break;
            case ADD_OFFLINE:
                event = new OnShowAddedEvent(mContext.getString(R.string.offline),
                        Toast.LENGTH_LONG);
                break;
        }

        if (event != null) {
            EventBus.getDefault().post(event);
        }
    }
}
