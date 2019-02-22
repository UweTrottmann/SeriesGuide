package com.battlelancer.seriesguide.backend;

import android.content.Context;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.modules.ApplicationContext;
import com.battlelancer.seriesguide.sync.NetworkJobProcessor;
import com.battlelancer.seriesguide.util.Errors;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.uwetrottmann.seriesguide.backend.account.Account;
import com.uwetrottmann.seriesguide.backend.episodes.Episodes;
import com.uwetrottmann.seriesguide.backend.lists.Lists;
import com.uwetrottmann.seriesguide.backend.movies.Movies;
import com.uwetrottmann.seriesguide.backend.shows.Shows;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

/**
 * Handles credentials and services for interacting with Hexagon.
 */
@Singleton // needs global state for lastSignInCheck + to avoid rebuilding services
public class HexagonTools {

    private static final String ACTION_SILENT_SIGN_IN = "silent sign-in";
    private static final JsonFactory JSON_FACTORY = new AndroidJsonFactory();
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final long SIGN_IN_CHECK_INTERVAL_MS = 5 * DateUtils.MINUTE_IN_MILLIS;

    private static GoogleSignInOptions googleSignInOptions;

    private final Context context;
    private GoogleSignInClient googleSignInClient;
    private GoogleAccountCredential credential;
    private long lastSignInCheck;
    private Shows showsService;
    private Episodes episodesService;
    private Movies moviesService;
    private Lists listsService;

    @Inject
    public HexagonTools(@ApplicationContext Context context) {
        this.context = context;
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
        // clear jobs before isEnabled may return true
        new NetworkJobProcessor(context).removeObsoleteJobs();
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
     * <p>
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
     * <p>
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
     * <p>
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
     * <p>
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
     *                         the {@link HexagonSettings#KEY_SHOULD_VALIDATE_ACCOUNT} flag. If
     *                         successful, clears the flag.
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
            return;
        }
        lastSignInCheck = SystemClock.elapsedRealtime();

        if (googleSignInClient == null) {
            googleSignInClient = GoogleSignIn.getClient(context, getGoogleSignInOptions());
        }

        android.accounts.Account account = null;
        Task<GoogleSignInAccount> signInTask = googleSignInClient.silentSignIn();

        try {
            GoogleSignInAccount signInAccount = Tasks.await(signInTask);
            if (signInAccount != null) {
                Timber.i("%s: successful", ACTION_SILENT_SIGN_IN);
                account = signInAccount.getAccount();
                credential.setSelectedAccount(account);
            } else {
                Errors.logAndReport(ACTION_SILENT_SIGN_IN,
                        new HexagonAuthError(ACTION_SILENT_SIGN_IN,
                                "GoogleSignInAccount is null"));
            }
        } catch (Exception e) {
            Errors.logAndReport(ACTION_SILENT_SIGN_IN,
                    HexagonAuthError.build(ACTION_SILENT_SIGN_IN, e));
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
}
