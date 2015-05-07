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
import android.preference.PreferenceManager;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.thetvdbapi.TheTVDB;
import com.battlelancer.seriesguide.thetvdbapi.TvdbException;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.BaseShow;
import com.uwetrottmann.trakt.v2.enums.Extended;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import com.uwetrottmann.trakt.v2.services.Sync;
import de.greenrobot.event.EventBus;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * Adds shows to the local database, tries to get watched and collected episodes if a trakt account
 * is connected.
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
    private static final int ADD_TRAKT_API_ERROR = 4;
    private static final int ADD_TRAKT_AUTH_ERROR = 5;

    private final Context context;
    private final LinkedList<SearchResult> addQueue = new LinkedList<>();

    private boolean isFinishedAddingShows = false;
    private boolean isSilentMode;
    private boolean isMergingShows;
    private String currentShowName;

    public AddShowTask(Context context, List<SearchResult> shows, boolean isSilentMode,
            boolean isMergingShows) {
        // use an activity independent context
        this.context = context.getApplicationContext();
        addQueue.addAll(shows);
        this.isSilentMode = isSilentMode;
        this.isMergingShows = isMergingShows;
    }

    /**
     * Adds shows to the add queue. If this returns false, the shows were not added because the task
     * is finishing up. Create a new one instead.
     */
    public boolean addShows(List<SearchResult> show, boolean isSilentMode, boolean isMergingShows) {
        if (isFinishedAddingShows) {
            Timber.d("addShows: failed, already finishing up.");
            return false;
        } else {
            this.isSilentMode = isSilentMode;
            // never reset isMergingShows once true, so merged flag is correctly set on completion
            this.isMergingShows = this.isMergingShows || isMergingShows;
            addQueue.addAll(show);
            Timber.d("addShows: added shows to queue.");
            return true;
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        Timber.d("Starting to add shows...");

        // don't even get started
        if (addQueue.isEmpty()) {
            Timber.d("Finished. Queue was empty.");
            return null;
        }

        if (!AndroidUtils.isNetworkConnected(context)) {
            Timber.d("Finished. No internet connection.");
            publishProgress(ADD_OFFLINE);
            return null;
        }

        if (isCancelled()) {
            Timber.d("Finished. Cancelled.");
            return null;
        }

        // get watched episodes from trakt (only if not connected to Hexagon) once
        List<BaseShow> collection = new ArrayList<>();
        List<BaseShow> watched = new ArrayList<>();
        if (!HexagonTools.isSignedIn(context)) {
            TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(context);
            if (trakt != null) {
                Timber.d("Getting watched and collected episodes from trakt.");
                try {
                    Sync sync = trakt.sync();
                    collection = sync.collectionShows(Extended.DEFAULT_MIN);
                    watched = sync.watchedShows(Extended.DEFAULT_MIN);
                } catch (RetrofitError e) {
                    // something went wrong, continue anyhow
                    Timber.w(e, "Getting watched and collected episodes failed");
                    publishProgress(ADD_TRAKT_API_ERROR);
                    return null;
                } catch (OAuthUnauthorizedException e) {
                    TraktCredentials.get(context).setCredentialsInvalid();
                    publishProgress(ADD_TRAKT_AUTH_ERROR);
                    return null;
                }
            }
        }

        int result;
        boolean addedAtLeastOneShow = false;
        boolean failedMergingShows = false;
        while (!addQueue.isEmpty()) {
            Timber.d("Starting to add next show...");
            if (isCancelled()) {
                Timber.d("Finished. Cancelled.");
                // only cancelled on config change, so don't rebuild fts
                // table yet
                return null;
            }

            if (!AndroidUtils.isNetworkConnected(context)) {
                Timber.d("Finished. No connection.");
                publishProgress(ADD_OFFLINE);
                failedMergingShows = true;
                break;
            }

            SearchResult nextShow = addQueue.removeFirst();

            try {
                boolean addedShow = TheTVDB.addShow(context, nextShow.tvdbid, watched, collection);
                result = addedShow ? ADD_SUCCESS : ADD_ALREADYEXISTS;
                addedAtLeastOneShow = addedShow
                        || addedAtLeastOneShow; // do not overwrite previous success
            } catch (TvdbException e) {
                // prevent a hexagon merge from failing if a show can not be added
                // because it does not exist (any longer)
                if (!(isMergingShows && e.getItemDoesNotExist())) {
                    failedMergingShows = true;
                }
                result = ADD_ERROR;
                Timber.e(e, "Adding show failed");
            }

            currentShowName = nextShow.title;
            publishProgress(result);
            Timber.d("Finished adding show. (Result code: " + result + ")");
        }

        isFinishedAddingShows = true;

        // when merging shows down from Hexagon, set success flag
        if (isMergingShows && !failedMergingShows) {
            HexagonSettings.setHasMergedShows(context, true);
        }

        if (addedAtLeastOneShow) {
            // make sure the next sync will download all ratings
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putLong(TraktSettings.KEY_LAST_SHOWS_RATED_AT, 0)
                    .putLong(TraktSettings.KEY_LAST_EPISODES_RATED_AT, 0)
                    .commit();

            // renew FTS3 table
            Timber.d("Renewing search table.");
            DBUtils.rebuildFtsTable(context);
        }

        Timber.d("Finished adding shows.");
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (isSilentMode) {
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
                        context.getString(R.string.add_already_exists, currentShowName),
                        Toast.LENGTH_LONG
                );
                break;
            case ADD_ERROR:
                event = new OnShowAddedEvent(
                        context.getString(R.string.add_error, currentShowName),
                        Toast.LENGTH_LONG);
                break;
            case ADD_OFFLINE:
                event = new OnShowAddedEvent(context.getString(R.string.offline),
                        Toast.LENGTH_LONG);
                break;
            case ADD_TRAKT_API_ERROR:
                event = new OnShowAddedEvent(context.getString(R.string.trakt_error_general),
                        Toast.LENGTH_LONG);
                break;
            case ADD_TRAKT_AUTH_ERROR:
                event = new OnShowAddedEvent(context.getString(R.string.trakt_error_credentials),
                        Toast.LENGTH_LONG);
                break;
        }

        if (event != null) {
            EventBus.getDefault().post(event);
        }
    }
}
