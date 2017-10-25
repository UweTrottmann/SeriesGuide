package com.battlelancer.seriesguide.settings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.sync.AccountUtils;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.ui.ConnectTraktActivity;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.AccessToken;
import java.io.IOException;
import retrofit2.Response;
import timber.log.Timber;

/**
 * A singleton helping to manage the user's trakt credentials.
 */
public class TraktCredentials {

    private static final String KEY_USERNAME = "com.battlelancer.seriesguide.traktuser";
    private static final String KEY_DISPLAYNAME = "com.battlelancer.seriesguide.traktuser.name";

    private static TraktCredentials _instance;

    private Context context;
    private boolean hasCredentials;
    private String username;

    public static synchronized TraktCredentials get(Context context) {
        if (_instance == null) {
            _instance = new TraktCredentials(context);
        }
        return _instance;
    }

    private TraktCredentials(Context context) {
        this.context = context.getApplicationContext();
        username = PreferenceManager.getDefaultSharedPreferences(this.context)
                .getString(KEY_USERNAME, null);
        hasCredentials = !TextUtils.isEmpty(getAccessToken());
    }

    /**
     * If there is a username and access token.
     */
    public boolean hasCredentials() {
        return hasCredentials;
    }

    /**
     * Removes the current trakt access token (but not the username), so {@link #hasCredentials()}
     * will return {@code false}, and shows a notification asking the user to re-connect.
     */
    public synchronized void setCredentialsInvalid() {
        if (!hasCredentials) {
            // already invalidated credentials
            return;
        }

        removeAccessToken();
        Timber.e("trakt credentials invalid, removed access token");

        NotificationCompat.Builder nb =
                new NotificationCompat.Builder(context, SgApp.NOTIFICATION_CHANNEL_ERRORS);
        NotificationSettings.setDefaultsForChannelErrors(context, nb);

        nb.setSmallIcon(R.drawable.ic_notification);
        nb.setContentTitle(context.getString(R.string.trakt_reconnect));
        nb.setContentText(context.getString(R.string.trakt_reconnect_details));
        nb.setTicker(context.getString(R.string.trakt_reconnect_details));

        PendingIntent intent = TaskStackBuilder.create(context)
                .addNextIntent(new Intent(context, ShowsActivity.class))
                .addNextIntent(new Intent(context, ConnectTraktActivity.class))
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        nb.setContentIntent(intent);
        nb.setAutoCancel(true);

        NotificationManager nm = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(SgApp.NOTIFICATION_TRAKT_AUTH_ID, nb.build());
        }
    }

    /**
     * Only removes the access token, but keeps the username.
     */
    private void removeAccessToken() {
        hasCredentials = false;
        setAccessToken(null);
    }

    /**
     * Removes the username and access token.
     */
    public synchronized void removeCredentials() {
        removeAccessToken();
        setUsername(null);
    }

    /**
     * Get the username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get the optional display name.
     */
    @Nullable
    public String getDisplayName() {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_DISPLAYNAME, null);
    }

    /**
     * Get the access token. Avoid keeping this in memory, maybe calling {@link #hasCredentials()}
     * is sufficient.
     */
    public String getAccessToken() {
        Account account = AccountUtils.getAccount(context);
        if (account == null) {
            return null;
        }

        AccountManager manager = AccountManager.get(context);
        return manager.getPassword(account);
    }

    /**
     * Stores the given access token.
     */
    public synchronized void storeAccessToken(@NonNull String accessToken) {
        if (TextUtils.isEmpty(accessToken)) {
            throw new IllegalArgumentException("Access token is null or empty.");
        }
        hasCredentials = setAccessToken(accessToken);
    }

    /**
     * Stores the given user name and display name.
     */
    public synchronized boolean storeUsername(@NonNull String username,
            @Nullable String displayname) {
        if (TextUtils.isEmpty(username)) {
            throw new IllegalArgumentException("Username is null or empty.");
        }
        return setUsername(username)
                && !TextUtils.isEmpty(displayname) && setDisplayname(displayname);
    }

    private boolean setUsername(String username) {
        this.username = username;
        return PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(KEY_USERNAME, username)
                .commit();
    }

    private boolean setDisplayname(String displayname) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(KEY_DISPLAYNAME, displayname)
                .commit();
    }

    private boolean setAccessToken(String accessToken) {
        Account account = AccountUtils.getAccount(context);
        if (account == null) {
            // try to create a new account
            AccountUtils.createAccount(context);
        }

        account = AccountUtils.getAccount(context);
        if (account == null) {
            // give up
            return false;
        }

        AccountManager manager = AccountManager.get(context);
        manager.setPassword(account, accessToken);

        return true;
    }

    /**
     * Checks for existing trakt credentials. If there aren't any valid ones, launches the trakt
     * connect flow.
     *
     * @return <b>true</b> if credentials are valid, <b>false</b> if invalid and launching trakt
     * connect flow.
     */
    public static boolean ensureCredentials(Context context) {
        if (!TraktCredentials.get(context).hasCredentials()) {
            // launch trakt connect flow
            context.startActivity(new Intent(context, ConnectTraktActivity.class));
            return false;
        }
        return true;
    }

    /**
     * Tries to refresh the current access token. Returns {@code false} on failure.
     */
    public synchronized boolean refreshAccessToken(TraktV2 trakt) {
        // do we even have a refresh token?
        String oldRefreshToken = TraktOAuthSettings.getRefreshToken(context);
        if (TextUtils.isEmpty(oldRefreshToken)) {
            Timber.d("refreshAccessToken: no refresh token, give up.");
            return false;
        }

        // try to get a new access token from trakt
        String accessToken = null;
        String refreshToken = null;
        long expiresIn = -1;
        try {
            Response<AccessToken> response = trakt.refreshAccessToken();
            if (response.isSuccessful()) {
                AccessToken token = response.body();
                accessToken = token.access_token;
                refreshToken = token.refresh_token;
                expiresIn = token.expires_in;
            } else {
                if (!SgTrakt.isUnauthorized(response)) {
                    SgTrakt.trackFailedRequest(context, "refresh access token", response);
                }
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(context, "refresh access token", e);
        }

        // did we obtain all required data?
        if (TextUtils.isEmpty(accessToken) || TextUtils.isEmpty(refreshToken) || expiresIn < 1) {
            Timber.e("refreshAccessToken: failed.");
            return false;
        }

        // store the new access token, refresh token and expiry date
        if (!setAccessToken(accessToken)
                || !TraktOAuthSettings.storeRefreshData(context, refreshToken, expiresIn)) {
            Timber.e("refreshAccessToken: saving failed");
            return false;
        }

        Timber.d("refreshAccessToken: success.");
        return true;
    }
}
