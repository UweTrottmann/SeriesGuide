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

package com.battlelancer.seriesguide.backend;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.preference.PreferenceManager;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.MovieTools;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.Utils;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.backend.account.Account;
import com.uwetrottmann.seriesguide.backend.episodes.Episodes;
import com.uwetrottmann.seriesguide.backend.movies.Movies;
import com.uwetrottmann.seriesguide.backend.shows.Shows;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import timber.log.Timber;

/**
 * Handles credentials and services for interacting with Hexagon.
 */
public class HexagonTools {

    private static GoogleAccountCredential sAccountCredential;

    private static final JsonFactory JSON_FACTORY = new AndroidJsonFactory();
    private static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();

    private static Shows sShowsService;
    private static Episodes sEpisodesService;
    private static Movies sMoviesService;

    /**
     * Creates and returns a new instance for this hexagon service.
     */
    public static synchronized Account buildAccountService(Context context) {
        Account.Builder builder = new Account.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, getAccountCredential(context)
        );
        return CloudEndpointUtils.updateBuilder(builder).build();
    }

    /**
     * Returns the instance for this hexagon service.
     */
    public static synchronized Shows getShowsService(Context context) {
        if (sShowsService == null) {
            Shows.Builder builder = new Shows.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, getAccountCredential(context)
            );
            sShowsService = CloudEndpointUtils.updateBuilder(builder).build();
        }
        return sShowsService;
    }

    /**
     * Returns the instance for this hexagon service.
     */
    public static synchronized Episodes getEpisodesService(Context context) {
        if (sEpisodesService == null) {
            Episodes.Builder builder = new Episodes.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, getAccountCredential(context)
            );
            sEpisodesService = CloudEndpointUtils.updateBuilder(builder).build();
        }
        return sEpisodesService;
    }

    /**
     * Returns the instance for this hexagon service.
     */
    public static synchronized Movies getMoviesService(Context context) {
        if (sMoviesService == null) {
            Movies.Builder builder = new Movies.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, getAccountCredential(context)
            );
            sMoviesService = CloudEndpointUtils.updateBuilder(builder).build();
        }
        return sMoviesService;
    }

    /**
     * Checks if it is possible to retrieve a valid OAuth2 token for the given account, hence, it
     * can be used for connecting to Hexagon.
     */
    public static boolean validateAccount(Context context, String accountName) {
        GoogleAccountCredential credential = GoogleAccountCredential.usingAudience(
                context.getApplicationContext(), HexagonSettings.AUDIENCE);
        credential.setSelectedAccountName(accountName);

        try {
            credential.getToken();
        } catch (IOException | GoogleAuthException e) {
            Timber.e(e, "validateAccount: failed to get valid OAuth2 token.");
            return false;
        }

        return true;
    }

    /**
     * Gets the account credentials used for talking with Hexagon.
     */
    public synchronized static GoogleAccountCredential getAccountCredential(Context context) {
        if (sAccountCredential == null) {
            GoogleAccountCredential credential = GoogleAccountCredential.usingAudience(
                    context.getApplicationContext(), HexagonSettings.AUDIENCE);
            credential.setSelectedAccountName(HexagonSettings.getAccountName(context));
            sAccountCredential = credential;
        }
        return sAccountCredential;
    }

    /**
     * Sets the account name used for calls to Hexagon.
     */
    public static void storeAccountName(Context context, String accountName) {
        // store account name in settings
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(HexagonSettings.KEY_ACCOUNT_NAME, accountName)
                .commit();

        // ensure account
        GoogleAccountCredential credential = getAccountCredential(context);
        credential.setSelectedAccountName(accountName);
    }

    /**
     * Returns true if an account for Hexagon is set and the user is allowed to use Hexagon (has
     * access to X).
     */
    public static boolean isSignedIn(Context context) {
        return Utils.hasAccessToX(context)
                && getAccountCredential(context).getSelectedAccountName() != null;
    }

    /**
     * Syncs episodes, shows and movies with Hexagon.
     *
     * <p> Merges shows, episodes and movies after a sign-in. Consecutive syncs will only download
     * changes to shows, episodes and movies.
     */
    public static boolean syncWithHexagon(Context context, HashSet<Integer> existingShows,
            HashMap<Integer, SearchResult> newShows) {
        Timber.d("syncWithHexagon: syncing...");

        //// EPISODES
        boolean syncEpisodesSuccessful = syncEpisodes(context);
        Timber.d("syncWithHexagon: episode sync "
                + (syncEpisodesSuccessful ? "SUCCESSFUL" : "FAILED"));

        //// SHOWS
        boolean syncShowsSuccessful = syncShows(context, existingShows, newShows);
        Timber.d("syncWithHexagon: show sync " + (syncShowsSuccessful ? "SUCCESSFUL" : "FAILED"));

        //// MOVIES
        boolean syncMoviesSuccessful = syncMovies(context);
        Timber.d("syncWithHexagon: movie sync " + (syncMoviesSuccessful ? "SUCCESSFUL" : "FAILED"));

        Timber.d("syncWithHexagon: syncing...DONE");
        return syncEpisodesSuccessful && syncShowsSuccessful && syncMoviesSuccessful;
    }

    private static boolean syncEpisodes(Context context) {
        // get shows that need episode merging
        Cursor query = context.getContentResolver().query(SeriesGuideContract.Shows.CONTENT_URI,
                new String[] { SeriesGuideContract.Shows._ID },
                SeriesGuideContract.Shows.HEXAGON_MERGE_COMPLETE + "=0",
                null, null);
        if (query == null) {
            return false;
        }

        // try merging episodes for them
        boolean mergeSuccessful = true;
        while (query.moveToNext()) {
            // abort if connection is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                return false;
            }

            int showTvdbId = query.getInt(0);

            boolean success = EpisodeTools.Download.flagsFromHexagon(context, showTvdbId);
            if (!success) {
                // try again next time
                mergeSuccessful = false;
                continue;
            }

            success = EpisodeTools.Upload.flagsToHexagon(context, showTvdbId);
            if (success) {
                // set merge as completed
                ContentValues values = new ContentValues();
                values.put(SeriesGuideContract.Shows.HEXAGON_MERGE_COMPLETE, true);
                context.getContentResolver()
                        .update(SeriesGuideContract.Shows.buildShowUri(showTvdbId), values,
                                null, null);
            } else {
                mergeSuccessful = false;
            }
        }
        query.close();

        // download changed episodes and update properties on existing episodes
        boolean changedDownloadSuccessful = EpisodeTools.Download.flagsFromHexagon(context);

        return mergeSuccessful && changedDownloadSuccessful;
    }

    private static boolean syncShows(Context context, HashSet<Integer> existingShows,
            HashMap<Integer, SearchResult> newShows) {
        boolean hasMergedShows = HexagonSettings.hasMergedShows(context);

        // download shows and apply property changes (if merging only overwrite some properties)
        boolean downloadSuccessful = ShowTools.Download.fromHexagon(context, existingShows,
                newShows, hasMergedShows);
        if (!downloadSuccessful) {
            return false;
        }

        // if merge required, upload all shows to Hexagon
        if (!hasMergedShows) {
            boolean uploadSuccessful = ShowTools.Upload.toHexagon(context);
            if (!uploadSuccessful) {
                return false;
            }
        }

        // add new shows
        if (newShows.size() > 0) {
            List<SearchResult> newShowsList = new LinkedList<>(newShows.values());
            TaskManager.getInstance(context).performAddTask(newShowsList, true, !hasMergedShows);
        } else if (!hasMergedShows) {
            // set shows as merged
            HexagonSettings.setHasMergedShows(context, true);
        }

        return true;
    }

    private static boolean syncMovies(Context context) {
        boolean hasMergedMovies = HexagonSettings.hasMergedMovies(context);

        // download movies and apply property changes, build list of new movies
        Set<Integer> newCollectionMovies = new HashSet<>();
        Set<Integer> newWatchlistMovies = new HashSet<>();
        boolean downloadSuccessful = MovieTools.Download.fromHexagon(context, newCollectionMovies,
                newWatchlistMovies, hasMergedMovies);
        if (!downloadSuccessful) {
            return false;
        }

        if (!hasMergedMovies) {
            boolean uploadSuccessful = MovieTools.Upload.toHexagon(context);
            if (!uploadSuccessful) {
                return false;
            }
        }

        // add new movies with the just downloaded properties
        SgSyncAdapter.UpdateResult result = MovieTools.Download.addMovies(context,
                newCollectionMovies, newWatchlistMovies);
        boolean addingSuccessful = result == SgSyncAdapter.UpdateResult.SUCCESS;
        if (!hasMergedMovies) {
            // ensure all missing movies from Hexagon are added before merge is complete
            if (!addingSuccessful) {
                return false;
            }
            // set movies as merged
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putBoolean(HexagonSettings.KEY_MERGED_MOVIES, true)
                    .commit();
        }

        return addingSuccessful;
    }
}
