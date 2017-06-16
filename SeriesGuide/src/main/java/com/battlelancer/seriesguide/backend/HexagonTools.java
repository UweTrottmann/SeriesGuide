package com.battlelancer.seriesguide.backend;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.modules.ApplicationContext;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.sync.SyncProgress;
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
import com.google.android.gms.common.api.Status;
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
import dagger.Lazy;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.greenrobot.eventbus.EventBus;
import timber.log.Timber;

/**
 * Handles credentials and services for interacting with Hexagon.
 */
@Singleton // needs global state for lastSignInCheck + to avoid rebuilding services
public class HexagonTools {

    private static final String HEXAGON_ERROR_CATEGORY = "Hexagon Error";
    private static final String SIGN_IN_ERROR_CATEGORY = "Sign-in Error";
    private static final String ACTION_SILENT_SIGN_IN = "silent sign-in";
    private static final JsonFactory JSON_FACTORY = new AndroidJsonFactory();
    private static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
    private static final long SIGN_IN_CHECK_INTERVAL_MS = 5 * DateUtils.MINUTE_IN_MILLIS;

    private static GoogleSignInOptions googleSignInOptions;

    private final Context context;
    private final Lazy<MovieTools> movieTools;
    private GoogleApiClient googleApiClient;
    private GoogleAccountCredential credential;
    private long lastSignInCheck;
    private Shows showsService;
    private Episodes episodesService;
    private Movies moviesService;
    private Lists listsService;

    @Inject
    public HexagonTools(@ApplicationContext Context context, Lazy<MovieTools> movieTools) {
        this.context = context;
        this.movieTools = movieTools;
    }

    /**
     * Enables Hexagon, resets sync state and saves account data.
     *
     * @return <code>false</code> if sync state could not be reset.
     */
    public boolean setEnabled(@NonNull GoogleSignInAccount account) {
        if (!HexagonSettings.resetSyncState(context)) {
            return false;
        }
        if (!PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(HexagonSettings.KEY_ENABLED, true)
                .putBoolean(HexagonSettings.KEY_SHOULD_VALIDATE_ACCOUNT, false)
                .commit()) {
            return false;
        }
        storeAccount(account);
        return true;
    }

    /**
     * Disables Hexagon and removes any account data.
     */
    public void setDisabled() {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(HexagonSettings.KEY_ENABLED, false)
                .putBoolean(HexagonSettings.KEY_SHOULD_VALIDATE_ACCOUNT, false)
                .apply();
        storeAccount(null);
    }

    /**
     * Creates and returns a new instance for this hexagon service or null if not signed in.
     *
     * Warning: checks sign-in state, make sure to guard with {@link HexagonSettings#isEnabled}.
     */
    @Nullable
    public synchronized Account buildAccountService() {
        GoogleAccountCredential credential = getAccountCredential(true);
        if (credential.getSelectedAccount() == null) {
            return null;
        }
        Account.Builder builder = new Account.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, credential
        );
        return CloudEndpointUtils.updateBuilder(context, builder).build();
    }

    /**
     * Returns the instance for this hexagon service or null if not signed in.
     *
     * Warning: checks sign-in state, make sure to guard with {@link HexagonSettings#isEnabled}.
     */
    @Nullable
    public synchronized Shows getShowsService() {
        GoogleAccountCredential credential = getAccountCredential(true);
        if (credential.getSelectedAccount() == null) {
            return null;
        }
        if (showsService == null) {
            Shows.Builder builder = new Shows.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, credential
            );
            showsService = CloudEndpointUtils.updateBuilder(context, builder).build();
        }
        return showsService;
    }

    /**
     * Returns the instance for this hexagon service or null if not signed in.
     *
     * Warning: checks sign-in state, make sure to guard with {@link HexagonSettings#isEnabled}.
     */
    @Nullable
    public synchronized Episodes getEpisodesService() {
        GoogleAccountCredential credential = getAccountCredential(true);
        if (credential.getSelectedAccount() == null) {
            return null;
        }
        if (episodesService == null) {
            Episodes.Builder builder = new Episodes.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, credential
            );
            episodesService = CloudEndpointUtils.updateBuilder(context, builder).build();
        }
        return episodesService;
    }

    /**
     * Returns the instance for this hexagon service or null if not signed in.
     *
     * Warning: checks sign-in state, make sure to guard with {@link HexagonSettings#isEnabled}.
     */
    @Nullable
    public synchronized Movies getMoviesService() {
        GoogleAccountCredential credential = getAccountCredential(true);
        if (credential.getSelectedAccount() == null) {
            return null;
        }
        if (moviesService == null) {
            Movies.Builder builder = new Movies.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, credential
            );
            moviesService = CloudEndpointUtils.updateBuilder(context, builder).build();
        }
        return moviesService;
    }

    /**
     * Returns the instance for this hexagon service or null if not signed in.
     */
    @Nullable
    public synchronized Lists getListsService() {
        GoogleAccountCredential credential = getAccountCredential(true);
        if (credential.getSelectedAccount() == null) {
            return null;
        }
        if (listsService == null) {
            Lists.Builder builder = new Lists.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, credential
            );
            listsService = CloudEndpointUtils.updateBuilder(context, builder).build();
        }
        return listsService;
    }

    /**
     * Get the Google account credentials to talk with Hexagon.
     *
     * <p>Make sure to check {@link GoogleAccountCredential#getSelectedAccount()} is not null (the
     * account might have gotten signed out).
     *
     * @param checkSignInState If enabled, tries to silently sign in with Google. If it fails, sets
     * the {@link HexagonSettings#KEY_SHOULD_VALIDATE_ACCOUNT} flag. If successful, clears the
     * flag.
     */
    private synchronized GoogleAccountCredential getAccountCredential(boolean checkSignInState) {
        if (credential == null) {
            credential = GoogleAccountCredential.usingAudience(context.getApplicationContext(),
                    HexagonSettings.AUDIENCE);
        }
        if (checkSignInState) {
            checkSignInState();
        }
        return credential;
    }

    private void checkSignInState() {
        if (credential.getSelectedAccount() != null && !isTimeForSignInStateCheck()) {
            Timber.d("%s: just checked state, skip", ACTION_SILENT_SIGN_IN);
        }
        lastSignInCheck = SystemClock.elapsedRealtime();

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, getGoogleSignInOptions())
                    .build();
        }

        android.accounts.Account account = null;
        ConnectionResult connectionResult = googleApiClient.blockingConnect();

        if (connectionResult.isSuccess()) {
            OptionalPendingResult<GoogleSignInResult> pendingResult
                    = Auth.GoogleSignInApi.silentSignIn(googleApiClient);

            GoogleSignInResult result = pendingResult.await();
            if (result.isSuccess()) {
                GoogleSignInAccount signInAccount = result.getSignInAccount();
                if (signInAccount != null) {
                    Timber.i("%s: successful", ACTION_SILENT_SIGN_IN);
                    account = signInAccount.getAccount();
                    credential.setSelectedAccount(account);
                } else {
                    trackSignInFailure(ACTION_SILENT_SIGN_IN, "GoogleSignInAccount is null");
                }
            } else {
                trackSignInFailure(ACTION_SILENT_SIGN_IN, result.getStatus());
            }

            googleApiClient.disconnect();
        } else {
            trackSignInFailure(ACTION_SILENT_SIGN_IN, connectionResult);
        }

        boolean shouldFixAccount = account == null;
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(HexagonSettings.KEY_SHOULD_VALIDATE_ACCOUNT, shouldFixAccount)
                .apply();
    }

    private boolean isTimeForSignInStateCheck() {
        return lastSignInCheck + SIGN_IN_CHECK_INTERVAL_MS < SystemClock.elapsedRealtime();
    }

    /**
     * Sets the account used for calls to Hexagon and saves the email address to display it in UI.
     */
    private void storeAccount(@Nullable GoogleSignInAccount account) {
        // store or remove account name in settings
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(HexagonSettings.KEY_ACCOUNT_NAME, account != null
                        ? account.getEmail()
                        : null)
                .apply();

        // try to set or remove account on credential
        getAccountCredential(false).setSelectedAccount(account != null
                ? account.getAccount()
                : null);
    }

    @NonNull
    public static GoogleSignInOptions getGoogleSignInOptions() {
        if (googleSignInOptions == null) {
            googleSignInOptions = new GoogleSignInOptions
                    .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();
        }
        return googleSignInOptions;
    }

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

    public void trackSignInFailure(String action, ConnectionResult connectionResult) {
        String failureMessage = connectionResult.getErrorCode() + " "
                + connectionResult.getErrorMessage();
        trackSignInFailure(action, failureMessage);
    }

    public void trackSignInFailure(String action, Status status) {
        String failureMessage = GoogleSignInStatusCodes.getStatusCodeString(status.getStatusCode());
        trackSignInFailure(action, failureMessage);
    }

    public void trackSignInFailure(String action, String failureMessage) {
        Utils.trackCustomEvent(context, SIGN_IN_ERROR_CATEGORY, action, failureMessage);
        Timber.e("%s: %s", action, failureMessage);
    }

    /**
     * Syncs episodes, shows and movies with Hexagon.
     *
     * <p> Merges shows, episodes and movies after a sign-in. Consecutive syncs will only download
     * changes to shows, episodes and movies.
     */
    public boolean syncWithHexagon(HashSet<Integer> existingShows,
            HashMap<Integer, SearchResult> newShows, SyncProgress progress) {
        Timber.d("syncWithHexagon: syncing...");

        //// EPISODES
        progress.publish(SyncProgress.Step.HEXAGON_EPISODES);
        boolean syncEpisodesSuccessful = syncEpisodes();
        Timber.d("syncWithHexagon: episode sync %s",
                syncEpisodesSuccessful ? "SUCCESSFUL" : "FAILED");

        //// SHOWS
        progress.publish(SyncProgress.Step.HEXAGON_SHOWS);
        boolean syncShowsSuccessful = syncShows(existingShows, newShows);
        Timber.d("syncWithHexagon: show sync %s", syncShowsSuccessful ? "SUCCESSFUL" : "FAILED");

        //// MOVIES
        progress.publish(SyncProgress.Step.HEXAGON_MOVIES);
        boolean syncMoviesSuccessful = syncMovies();
        Timber.d("syncWithHexagon: movie sync %s", syncMoviesSuccessful ? "SUCCESSFUL" : "FAILED");

        //// LISTS
        progress.publish(SyncProgress.Step.HEXAGON_LISTS);
        boolean syncListsSuccessful = syncLists();
        Timber.d("syncWithHexagon: lists sync %s", syncListsSuccessful ? "SUCCESSFUL" : "FAILED");

        Timber.d("syncWithHexagon: syncing...DONE");
        return syncEpisodesSuccessful
                && syncShowsSuccessful
                && syncMoviesSuccessful
                && syncListsSuccessful;
    }

    private boolean syncEpisodes() {
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

            boolean success = EpisodeTools.Download.flagsFromHexagon(context, this, showTvdbId);
            if (!success) {
                // try again next time
                mergeSuccessful = false;
                continue;
            }

            success = EpisodeTools.Upload.flagsToHexagon(context, this, showTvdbId);
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
        boolean changedDownloadSuccessful = EpisodeTools.Download.flagsFromHexagon(context, this);

        return mergeSuccessful && changedDownloadSuccessful;
    }

    private boolean syncShows(HashSet<Integer> existingShows,
            HashMap<Integer, SearchResult> newShows) {
        boolean hasMergedShows = HexagonSettings.hasMergedShows(context);

        // download shows and apply property changes (if merging only overwrite some properties)
        boolean downloadSuccessful = ShowTools.Download.fromHexagon(context, this, existingShows,
                newShows, hasMergedShows);
        if (!downloadSuccessful) {
            return false;
        }

        // if merge required, upload all shows to Hexagon
        if (!hasMergedShows) {
            boolean uploadSuccessful = ShowTools.Upload.toHexagon(context, this);
            if (!uploadSuccessful) {
                return false;
            }
        }

        // add new shows
        if (newShows.size() > 0) {
            List<SearchResult> newShowsList = new LinkedList<>(newShows.values());
            TaskManager.getInstance().performAddTask(context, newShowsList, true, !hasMergedShows);
        } else if (!hasMergedShows) {
            // set shows as merged
            HexagonSettings.setHasMergedShows(context, true);
        }

        return true;
    }

    @SuppressLint("ApplySharedPref")
    private boolean syncMovies() {
        boolean hasMergedMovies = HexagonSettings.hasMergedMovies(context);

        // download movies and apply property changes, build list of new movies
        Set<Integer> newCollectionMovies = new HashSet<>();
        Set<Integer> newWatchlistMovies = new HashSet<>();
        boolean downloadSuccessful = MovieTools.Download.fromHexagon(context, this,
                newCollectionMovies, newWatchlistMovies, hasMergedMovies);
        if (!downloadSuccessful) {
            return false;
        }

        if (!hasMergedMovies) {
            boolean uploadSuccessful = MovieTools.Upload.toHexagon(context, this);
            if (!uploadSuccessful) {
                return false;
            }
        }

        // add new movies with the just downloaded properties
        SgSyncAdapter.UpdateResult result = movieTools.get()
                .addMovies(newCollectionMovies, newWatchlistMovies);
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

    @SuppressLint("ApplySharedPref")
    private boolean syncLists() {
        boolean hasMergedLists = HexagonSettings.hasMergedLists(context);

        if (!ListsTools.downloadFromHexagon(context, this, hasMergedLists)) {
            return false;
        }

        if (hasMergedLists) {
            // on regular syncs, remove lists gone from hexagon
            if (!ListsTools.removeListsRemovedOnHexagon(context, this)) {
                return false;
            }
        } else {
            // upload all lists on initial data merge
            if (!ListsTools.uploadAllToHexagon(context, this)) {
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
