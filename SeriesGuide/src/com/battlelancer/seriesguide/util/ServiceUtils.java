/*
 * Copyright 2013 Uwe Trottmann
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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.ui.dialogs.TraktCredentialsDialogFragment;
import com.jakewharton.trakt.ServiceManager;
import com.uwetrottmann.seriesguide.R;

/**
 * Helper methods to interact with third-party services used within SeriesGuide.
 */
public class ServiceUtils {

    private static ServiceManager sTraktServiceManagerInstance;

    private static ServiceManager sTraktServiceManagerWithAuthInstance;

    private static com.uwetrottmann.tmdb.ServiceManager sTmdbServiceManagerInstance;

    /**
     * Get a tmdb-java ServiceManager with our API key set.
     */
    public static synchronized com.uwetrottmann.tmdb.ServiceManager getTmdbServiceManager(
            Context context) {
        if (sTmdbServiceManagerInstance == null) {
            sTmdbServiceManagerInstance = new com.uwetrottmann.tmdb.ServiceManager();
            sTmdbServiceManagerInstance.setReadTimeout(10000);
            sTmdbServiceManagerInstance.setConnectionTimeout(15000);
            sTmdbServiceManagerInstance.setApiKey(context.getResources().getString(
                    R.string.tmdb_apikey));
        }

        return sTmdbServiceManagerInstance;
    }

    /**
     * Get a trakt-java ServiceManager with just our API key set. NO user auth
     * data.
     * 
     * @param context
     * @return
     */
    public static synchronized ServiceManager getTraktServiceManager(Context context) {
        if (ServiceUtils.sTraktServiceManagerInstance == null) {
            ServiceUtils.sTraktServiceManagerInstance = new ServiceManager();
            ServiceUtils.sTraktServiceManagerInstance.setReadTimeout(10000);
            ServiceUtils.sTraktServiceManagerInstance.setConnectionTimeout(15000);
            ServiceUtils.sTraktServiceManagerInstance.setApiKey(context.getResources().getString(
                    R.string.trakt_apikey));
            // this made some problems, so sadly disabled for now
            // manager.setUseSsl(true);
        }
    
        return ServiceUtils.sTraktServiceManagerInstance;
    }

    /**
     * Get the trakt-java ServiceManger with user credentials and our API key
     * set.
     * 
     * @param context
     * @param refreshCredentials Set this flag to refresh the user credentials.
     * @return
     * @throws Exception When decrypting the password failed.
     */
    public static synchronized ServiceManager getTraktServiceManagerWithAuth(Context context,
            boolean refreshCredentials) {
        if (ServiceUtils.sTraktServiceManagerWithAuthInstance == null) {
            ServiceUtils.sTraktServiceManagerWithAuthInstance = new ServiceManager();
            ServiceUtils.sTraktServiceManagerWithAuthInstance.setReadTimeout(10000);
            ServiceUtils.sTraktServiceManagerWithAuthInstance.setConnectionTimeout(15000);
            ServiceUtils.sTraktServiceManagerWithAuthInstance.setApiKey(context.getResources()
                    .getString(
                            R.string.trakt_apikey));
            // this made some problems, so sadly disabled for now
            // manager.setUseSsl(true);

            refreshCredentials = true;
        }

        if (refreshCredentials) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context
                    .getApplicationContext());

            final String username = prefs.getString(SeriesGuidePreferences.KEY_TRAKTUSER, null);
            String password = prefs.getString(SeriesGuidePreferences.KEY_TRAKTPWD, null);

            if (!TextUtils.isEmpty(password)) {
                // decryption might return null, so wrap in separate condition
                password = SimpleCrypto.decrypt(password, context);
            }

            if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
                ServiceUtils.sTraktServiceManagerWithAuthInstance.setAuthentication(username,
                        password);
            } else {
                // clear all trakt credentials
                TraktCredentialsDialogFragment.clearTraktCredentials(prefs);
                ServiceUtils.sTraktServiceManagerWithAuthInstance.setAuthentication(null, null);
                return null;
            }
        }

        return ServiceUtils.sTraktServiceManagerWithAuthInstance;
    }

    public static String getTraktUsername(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context
                .getApplicationContext());

        return prefs.getString(SeriesGuidePreferences.KEY_TRAKTUSER, "");
    }

    public static boolean isTraktCredentialsValid(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context
                .getApplicationContext());
        String username = prefs.getString(SeriesGuidePreferences.KEY_TRAKTUSER, "");
        String password = prefs.getString(SeriesGuidePreferences.KEY_TRAKTPWD, "");

        return (!username.equalsIgnoreCase("") && !password.equalsIgnoreCase(""));
    }
}
