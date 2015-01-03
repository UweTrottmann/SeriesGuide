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

package com.battlelancer.seriesguide.settings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SeriesGuideApplication;
import com.battlelancer.seriesguide.sync.AccountUtils;
import com.battlelancer.seriesguide.ui.ConnectTraktActivity;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.trakt.v2.TraktV2;
import timber.log.Timber;

/**
 * A singleton helping to manage the user's trakt credentials.
 */
public class TraktCredentials {

    private static final String KEY_USERNAME = "com.battlelancer.seriesguide.traktuser";

    private static TraktCredentials _instance;

    private Context mContext;

    private boolean mHasCredentials;

    private String mUsername;

    public static synchronized TraktCredentials get(Context context) {
        if (_instance == null) {
            _instance = new TraktCredentials(context);
        }
        return _instance;
    }

    private TraktCredentials(Context context) {
        mContext = context.getApplicationContext();
        mUsername = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getString(KEY_USERNAME, null);
        mHasCredentials = !TextUtils.isEmpty(getUsername()) && !TextUtils.isEmpty(getAccessToken());
    }

    /**
     * If there is a username and acess token.
     */
    public boolean hasCredentials() {
        return mHasCredentials;
    }

    /**
     * Removes the current trakt access token (but not the username), makes {@link
     * #hasCredentials()} return {@code false}. Will log error.
     */
    public void setCredentialsInvalid() {
        removeAccessToken();
        Timber.e("trakt credentials invalid, removed access token");

        NotificationCompat.Builder nb = new NotificationCompat.Builder(mContext);
        nb.setSmallIcon(R.drawable.ic_notification);
        nb.setContentTitle(mContext.getString(R.string.trakt_reconnect));
        nb.setContentText(mContext.getString(R.string.trakt_reconnect_details));
        nb.setTicker(mContext.getString(R.string.trakt_reconnect_details));

        PendingIntent intent = TaskStackBuilder.create(mContext)
                .addNextIntent(new Intent(mContext, ShowsActivity.class))
                .addNextIntent(new Intent(mContext, ConnectTraktActivity.class))
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        nb.setContentIntent(intent);

        nb.setAutoCancel(true);
        nb.setColor(mContext.getResources().getColor(R.color.accent_primary));
        nb.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        nb.setCategory(NotificationCompat.CATEGORY_ERROR);

        NotificationManager nm = (NotificationManager) mContext.getSystemService(
                Context.NOTIFICATION_SERVICE);
        nm.notify(SeriesGuideApplication.NOTIFICATION_TRAKT_AUTH_ID, nb.build());
    }

    /**
     * Only removes the access token, but keeps the username.
     */
    private void removeAccessToken() {
        // clear all in-memory credentials from Trakt service manager in any case
        TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(mContext);
        if (trakt != null) {
            trakt.setAccessToken(null);
        }

        mHasCredentials = false;

        setAccessToken(null);
    }

    /**
     * Removes the username and access token.
     */
    public void removeCredentials() {
        removeAccessToken();
        setUsername(null);
    }

    /**
     * Get the username.
     */
    public String getUsername() {
        return mUsername;
    }

    /**
     * Get the access token. Avoid keeping this in memory, maybe calling {@link #hasCredentials()}
     * is sufficient.
     */
    public String getAccessToken() {
        Account account = AccountUtils.getAccount(mContext);
        if (account == null) {
            return null;
        }

        AccountManager manager = AccountManager.get(mContext);
        return manager.getPassword(account);
    }

    /**
     * Stores the given credentials. Performs no sanitation, however, if any is null or empty throws
     * an exception.
     */
    public void setCredentials(String username, String accessToken) {
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(accessToken)) {
            throw new IllegalArgumentException("Username or access token is null or empty.");
        }
        mHasCredentials = setUsername(username) && setAccessToken(accessToken);
    }

    private boolean setUsername(String username) {
        mUsername = username;
        return PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                .putString(KEY_USERNAME, username).commit();
    }

    private boolean setAccessToken(String accessToken) {
        Account account = AccountUtils.getAccount(mContext);
        if (account == null) {
            // try to create a new account
            AccountUtils.createAccount(mContext);
        }

        account = AccountUtils.getAccount(mContext);
        if (account == null) {
            // give up
            return false;
        }

        AccountManager manager = AccountManager.get(mContext);
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
}
