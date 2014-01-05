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
 *
 */

package com.battlelancer.seriesguide.settings;

import com.battlelancer.seriesguide.enums.TraktStatus;
import com.battlelancer.seriesguide.sync.AccountUtils;
import com.battlelancer.seriesguide.ui.ConnectTraktActivity;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.Response;
import com.uwetrottmann.androidutils.AndroidUtils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import retrofit.RetrofitError;

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
        mContext = context;
        mUsername = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getString(KEY_USERNAME, null);
        mHasCredentials = !TextUtils.isEmpty(getUsername())
                && !TextUtils.isEmpty(getPassword());
    }

    /**
     * If there is a username and password.
     */
    public boolean hasCredentials() {
        return mHasCredentials;
    }

    /**
     * Only removes the password, but keeps the username.
     */
    public void removePassword() {
        // clear all in-memory credentials from Trakt service manager in any case
        ServiceUtils.getTraktWithAuth(mContext).setAuthentication(null, null);

        mHasCredentials = false;

        setPassword(null);
    }

    /**
     * Removes the username and password.
     */
    public void removeCredentials() {
        removePassword();
        setUsername(null);
    }

    /**
     * Get the username.
     */
    public String getUsername() {
        return mUsername;
    }

    /**
     * Get the password. Avoid keeping this in memory, maybe calling {@link #hasCredentials()} is
     * sufficient.
     */
    public String getPassword() {
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
    public void setCredentials(String username, String password) {
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            throw new IllegalArgumentException("Username or password is null or empty.");
        }
        mHasCredentials = setUsername(username) && setPassword(password);
    }

    private boolean setUsername(String username) {
        mUsername = username;
        return PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                .putString(KEY_USERNAME, username).commit();
    }

    private boolean setPassword(String password) {
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
        manager.setPassword(account, password);

        return true;
    }

    /**
     * Checks for existing trakt credentials. If there aren't any valid ones, launches the trakt
     * connect flow.
     *
     * @return <b>true</b> if credentials are valid, <b>false</b> if invalid and launching trakt
     * connect flow.
     */
    public boolean ensureCredentials() {
        if (!hasCredentials()) {
            // launch trakt connect flow
            mContext.startActivity(new Intent(mContext, ConnectTraktActivity.class));
            return false;
        }
        return true;
    }

    /**
     * Creates a network request to check if the current trakt credentials are still valid. Will
     * assume valid credentials if there was no response from trakt (due to a network error,
     * etc.).<br> <b>Never</b> run this on the main thread.
     */
    public void validateCredentials() {
        // check for connectivity
        if (!AndroidUtils.isNetworkConnected(mContext)) {
            return;
        }

        // try to get a trakt instance with auth data
        Trakt manager = ServiceUtils.getTraktWithAuth(mContext);
        if (manager == null) {
            // no credentials available
            return;
        }
        try {
            Response r = manager.accountService().test();
            if (r != null && TraktStatus.FAILURE.equals(r.status)) {
                // credentials invalid according to trakt, remove the password
                removePassword();
            }
        } catch (RetrofitError ignored) {
        }
        /*
         * Ignore exceptions, trakt may be offline, etc. We expect the user to
         * disconnect and reconnect himself.
         */
    }

}
