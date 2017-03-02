package com.battlelancer.seriesguide.backend;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.ui.ListsActivity;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.ListsTools;
import com.battlelancer.seriesguide.util.MovieTools;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.Utils;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.backend.account.Account;
import com.uwetrottmann.seriesguide.backend.episodes.Episodes;
import com.uwetrottmann.seriesguide.backend.lists.Lists;
import com.uwetrottmann.seriesguide.backend.movies.Movies;
import com.uwetrottmann.seriesguide.backend.shows.Shows;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.greenrobot.eventbus.EventBus;
import timber.log.Timber;

/**
 * Handles credentials and services for interacting with Hexagon.
 */
public class HexagonTools {

    private static final String HEXAGON_ERROR_CATEGORY = "Hexagon Error";
    private static final JsonFactory JSON_FACTORY = new AndroidJsonFactory();
    private static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();

    private static GoogleAccountCredential credential;

    private static Shows sShowsService;
    private static Episodes sEpisodesService;
    private static Movies sMoviesService;
    private static Lists sListsService;

    public static void trackFailedRequest(Context context, String action, IOException e) {
        if (e instanceof HttpResponseException) {
            HttpResponseException responseException = (HttpResponseException) e;
            Utils.trackCustomEvent(context, HEXAGON_ERROR_CATEGORY, action,
                    responseException.getStatusCode() + " " + responseException.getStatusMessage());
            // log like "action: 404 not found"
            Timber.e("%s: %s %s", action, responseException.getStatusCode(),
                    responseException.getStatusMessage());
        } else {
            Utils.trackCustomEvent(context, HEXAGON_ERROR_CATEGORY, action, e.getMessage());
            // log like "action: Unable to resolve host"
            Timber.e("%s: %s", action, e.getMessage());
        }
    }

    /**
     * Creates and returns a new instance for this hexagon service or null if not signed in.
     */
    @Nullable
    public static synchronized Account buildAccountService(Context context) {
        GoogleAccountCredential credential = getAccountCredential(context, true);
        if (credential.getSelectedAccountName() == null) {
            return null;
        }
        Account.Builder builder = new Account.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, credential
        );
        return CloudEndpointUtils.updateBuilder(context, builder).build();
    }

    /**
     * Returns the instance for this hexagon service or null if not signed in.
     */
    @Nullable
    public static synchronized Shows getShowsService(Context context) {
        GoogleAccountCredential credential = getAccountCredential(context, true);
        if (credential.getSelectedAccountName() == null) {
            return null;
        }
        if (sShowsService == null) {
            Shows.Builder builder = new Shows.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, credential
            );
            sShowsService = CloudEndpointUtils.updateBuilder(context, builder).build();
        }
        return sShowsService;
    }

    /**
     * Returns the instance for this hexagon service or null if not signed in.
     */
    @Nullable
    public static synchronized Episodes getEpisodesService(Context context) {
        GoogleAccountCredential credential = getAccountCredential(context, true);
        if (credential.getSelectedAccountName() == null) {
            return null;
        }
        if (sEpisodesService == null) {
            Episodes.Builder builder = new Episodes.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, credential
            );
            sEpisodesService = CloudEndpointUtils.updateBuilder(context, builder).build();
        }
        return sEpisodesService;
    }

    /**
     * Returns the instance for this hexagon service or null if not signed in.
     */
    @Nullable
    public static synchronized Movies getMoviesService(Context context) {
        GoogleAccountCredential credential = getAccountCredential(context, true);
        if (credential.getSelectedAccountName() == null) {
            return null;
        }
        if (sMoviesService == null) {
            Movies.Builder builder = new Movies.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, credential
            );
            sMoviesService = CloudEndpointUtils.updateBuilder(context, builder).build();
        }
        return sMoviesService;
    }

    /**
     * Returns the instance for this hexagon service or null if not signed in.
     */
    @Nullable
    public static synchronized Lists getListsService(Context context) {
        GoogleAccountCredential credential = getAccountCredential(context, true);
        if (credential.getSelectedAccountName() == null) {
            return null;
        }
        if (sListsService == null) {
            Lists.Builder builder = new Lists.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, credential
            );
            sListsService = CloudEndpointUtils.updateBuilder(context, builder).build();
        }
        return sListsService;
    }

    /**
     * Get the Google account credentials used for talking with Hexagon.
     *
     * <p>Make sure to check {@link GoogleAccountCredential#getSelectedAccount()} is not null (is
     * null if e.g. do not have permission to access the account or the account was removed).
     */
    private synchronized static GoogleAccountCredential getAccountCredential(Context context,
            boolean autoSignIn) {
        if (credential == null) {
            credential = GoogleAccountCredential.usingAudience(
                    context.getApplicationContext(), HexagonSettings.AUDIENCE);
        }
        // try to restore account if none is set
        if (autoSignIn && credential.getSelectedAccount() == null) {
            GoogleSignInOptions gso =
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .build();

            GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();
            ConnectionResult connectionResult = googleApiClient.blockingConnect();
            if (connectionResult.isSuccess()) {
                OptionalPendingResult<GoogleSignInResult> pendingResult
                        = Auth.GoogleSignInApi.silentSignIn(googleApiClient);
                GoogleSignInResult result = pendingResult.await();
                if (result.isSuccess()) {
                    GoogleSignInAccount signInAccount = result.getSignInAccount();
                    if (signInAccount != null) {
                        Timber.i("Silent sign-in successful.");
                        android.accounts.Account account = signInAccount.getAccount();
                        credential.setSelectedAccount(account);
                    } else {
                        Timber.e("Silent sign-in failed: GoogleSignInAccount is null.");
                    }
                } else {
                    Timber.e("Silent sign-in failed: %s",
                            GoogleSignInStatusCodes.getStatusCodeString(
                                    result.getStatus().getStatusCode()));
                }
                googleApiClient.disconnect();
            } else {
                Timber.e("Silent sign-in failed: no GoogleApiClient connection %s",
                        connectionResult);
            }
        }
        return credential;
    }

    /**
     * Sets the account name used for calls to Hexagon.
     */
    public static void storeAccount(@NonNull Context context,
            @Nullable GoogleSignInAccount account) {
        // store or remove account name in settings
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(HexagonSettings.KEY_ACCOUNT_NAME, account != null
                        ? account.getEmail()
                        : null)
                .apply();

        // try to set or remove account on credential
        getAccountCredential(context, false).setSelectedAccount(account != null
                ? account.getAccount()
                : null);
    }

    /**
     * Returns true if an account for Hexagon is set and the user is allowed to use Hexagon (has
     * access to X).
     */
    public static boolean isConfigured(Context context) {
        return Utils.hasAccessToX(context)
                && getAccountCredential(context, false).getSelectedAccount() != null;
    }

    /**
     * Syncs episodes, shows and movies with Hexagon.
     *
     * <p> Merges shows, episodes and movies after a sign-in. Consecutive syncs will only download
     * changes to shows, episodes and movies.
     */
    public static boolean syncWithHexagon(SgApp app, HashSet<Integer> existingShows,
            HashMap<Integer, SearchResult> newShows) {
        Timber.d("syncWithHexagon: syncing...");

        //// EPISODES
        boolean syncEpisodesSuccessful = syncEpisodes(app);
        Timber.d("syncWithHexagon: episode sync %s",
                syncEpisodesSuccessful ? "SUCCESSFUL" : "FAILED");

        //// SHOWS
        boolean syncShowsSuccessful = syncShows(app, existingShows, newShows);
        Timber.d("syncWithHexagon: show sync %s", syncShowsSuccessful ? "SUCCESSFUL" : "FAILED");

        //// MOVIES
        boolean syncMoviesSuccessful = syncMovies(app);
        Timber.d("syncWithHexagon: movie sync %s", syncMoviesSuccessful ? "SUCCESSFUL" : "FAILED");

        //// LISTS
        boolean syncListsSuccessful = syncLists(app);
        Timber.d("syncWithHexagon: lists sync %s", syncListsSuccessful ? "SUCCESSFUL" : "FAILED");

        Timber.d("syncWithHexagon: syncing...DONE");
        return syncEpisodesSuccessful
                && syncShowsSuccessful
                && syncMoviesSuccessful
                && syncListsSuccessful;
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

    private static boolean syncShows(SgApp app, HashSet<Integer> existingShows,
            HashMap<Integer, SearchResult> newShows) {
        boolean hasMergedShows = HexagonSettings.hasMergedShows(app);

        // download shows and apply property changes (if merging only overwrite some properties)
        boolean downloadSuccessful = ShowTools.Download.fromHexagon(app, existingShows,
                newShows, hasMergedShows);
        if (!downloadSuccessful) {
            return false;
        }

        // if merge required, upload all shows to Hexagon
        if (!hasMergedShows) {
            boolean uploadSuccessful = ShowTools.Upload.toHexagon(app);
            if (!uploadSuccessful) {
                return false;
            }
        }

        // add new shows
        if (newShows.size() > 0) {
            List<SearchResult> newShowsList = new LinkedList<>(newShows.values());
            TaskManager.getInstance(app).performAddTask(app, newShowsList, true, !hasMergedShows);
        } else if (!hasMergedShows) {
            // set shows as merged
            HexagonSettings.setHasMergedShows(app, true);
        }

        return true;
    }

    private static boolean syncMovies(SgApp app) {
        boolean hasMergedMovies = HexagonSettings.hasMergedMovies(app);

        // download movies and apply property changes, build list of new movies
        Set<Integer> newCollectionMovies = new HashSet<>();
        Set<Integer> newWatchlistMovies = new HashSet<>();
        boolean downloadSuccessful = MovieTools.Download.fromHexagon(app, newCollectionMovies,
                newWatchlistMovies, hasMergedMovies);
        if (!downloadSuccessful) {
            return false;
        }

        if (!hasMergedMovies) {
            boolean uploadSuccessful = MovieTools.Upload.toHexagon(app);
            if (!uploadSuccessful) {
                return false;
            }
        }

        // add new movies with the just downloaded properties
        SgSyncAdapter.UpdateResult result = app.getMovieTools()
                .addMovies(newCollectionMovies, newWatchlistMovies);
        boolean addingSuccessful = result == SgSyncAdapter.UpdateResult.SUCCESS;
        if (!hasMergedMovies) {
            // ensure all missing movies from Hexagon are added before merge is complete
            if (!addingSuccessful) {
                return false;
            }
            // set movies as merged
            PreferenceManager.getDefaultSharedPreferences(app)
                    .edit()
                    .putBoolean(HexagonSettings.KEY_MERGED_MOVIES, true)
                    .commit();
        }

        return addingSuccessful;
    }

    private static boolean syncLists(Context context) {
        boolean hasMergedLists = HexagonSettings.hasMergedLists(context);

        if (!ListsTools.downloadFromHexagon(context, hasMergedLists)) {
            return false;
        }

        if (hasMergedLists) {
            // on regular syncs, remove lists gone from hexagon
            if (!ListsTools.removeListsRemovedOnHexagon(context)) {
                return false;
            }
        } else {
            // upload all lists on initial data merge
            if (!ListsTools.uploadAllToHexagon(context)) {
                return false;
            }
        }

        // notify lists activity
        EventBus.getDefault().post(new ListsActivity.ListsChangedEvent());

        if (!hasMergedLists) {
            // set lists as merged
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putBoolean(HexagonSettings.KEY_MERGED_LISTS, true)
                    .commit();
        }

        return true;
    }
}
