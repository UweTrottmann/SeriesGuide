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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.enums.Result;
import com.battlelancer.seriesguide.enums.TraktResult;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.settings.TraktOAuthSettings;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.traktapi.SgTraktV2;
import com.battlelancer.seriesguide.ui.BaseOAuthActivity;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.Settings;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * Expects a valid trakt OAuth auth code. Retrieves the access token and username for the associated
 * user. If successful, the credentials are stored.
 */
public class ConnectTraktTask extends AsyncTask<String, Void, Integer> {

    public interface OnTaskFinishedListener {

        /**
         * Returns one of {@link com.battlelancer.seriesguide.enums.NetworkResult}.
         */
        public void onTaskFinished(int resultCode);
    }

    private final Context mContext;

    private OnTaskFinishedListener mListener;

    public ConnectTraktTask(Context context, OnTaskFinishedListener listener) {
        mContext = context;
        mListener = listener;
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    protected Integer doInBackground(String... params) {
        // check for connectivity
        if (!AndroidUtils.isNetworkConnected(mContext)) {
            return TraktResult.OFFLINE;
        }

        // get account data
        String authCode = params[0];

        // check if we have any usable data
        if (TextUtils.isEmpty(authCode)) {
            return TraktResult.API_ERROR;
        }

        // get access token
        String accessToken = null;
        String refreshToken = null;
        long expiresIn = -1;
        try {
            OAuthAccessTokenResponse response = TraktV2.getAccessToken(
                    BuildConfig.TRAKT_CLIENT_ID,
                    BuildConfig.TRAKT_CLIENT_SECRET,
                    BaseOAuthActivity.OAUTH_CALLBACK_URL_LOCALHOST,
                    authCode
            );
            if (response != null) {
                accessToken = response.getAccessToken();
                refreshToken = response.getRefreshToken();
                expiresIn = response.getExpiresIn();
            }
        } catch (OAuthSystemException | OAuthProblemException e) {
            accessToken = null;
            Timber.e(e, "Getting access token failed");
        }

        // did we obtain all required data?
        if (TextUtils.isEmpty(accessToken) || TextUtils.isEmpty(refreshToken) || expiresIn < 1) {
            return TraktResult.API_ERROR;
        }

        // get user name
        String username = null;
        TraktV2 temporaryTrakt = new SgTraktV2(mContext)
                .setApiKey(BuildConfig.TRAKT_CLIENT_ID)
                .setAccessToken(accessToken);
        try {
            Settings settings = temporaryTrakt.users().settings();
            if (settings != null && settings.user != null) {
                username = settings.user.username;
            }
        } catch (RetrofitError e) {
            Timber.e(e, "Getting user name failed");
            return AndroidUtils.isNetworkConnected(mContext)
                    ? TraktResult.API_ERROR : TraktResult.OFFLINE;
        } catch (OAuthUnauthorizedException e) {
            Timber.e(e, "Getting user name failed");
            return TraktResult.AUTH_ERROR;
        }

        // did we obtain a username?
        if (TextUtils.isEmpty(username)) {
            return TraktResult.API_ERROR;
        }

        // store the new credentials
        TraktCredentials.get(mContext).setCredentials(username, accessToken);
        // store refresh token and expiry date
        if (!TraktOAuthSettings.storeRefreshData(mContext, refreshToken, expiresIn)) {
            // save failed
            return Result.ERROR;
        }

        // try to get service manager
        TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(mContext);
        if (trakt == null) {
            // looks like credentials weren't saved properly
            return Result.ERROR;
        }
        // set new credentials
        trakt.setAccessToken(accessToken);

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext)
                .edit();

        // make next sync merge local watched and collected episodes with those on trakt
        editor.putBoolean(TraktSettings.KEY_HAS_MERGED_EPISODES, false);
        // make next sync merge local movies with those on trakt
        editor.putBoolean(TraktSettings.KEY_HAS_MERGED_MOVIES, false);

        // make sure the next sync will run a full episode sync
        editor.putLong(TraktSettings.KEY_LAST_FULL_EPISODE_SYNC, 0);
        // make sure the next sync will download all watched movies
        editor.putLong(TraktSettings.KEY_LAST_MOVIES_WATCHED_AT, 0);
        // make sure the next sync will download all ratings
        editor.putLong(TraktSettings.KEY_LAST_SHOWS_RATED_AT, 0);
        editor.putLong(TraktSettings.KEY_LAST_EPISODES_RATED_AT, 0);
        editor.putLong(TraktSettings.KEY_LAST_MOVIES_RATED_AT, 0);

        editor.commit();

        return Result.SUCCESS;
    }

    @Override
    protected void onPostExecute(Integer resultCode) {
        if (resultCode == Result.SUCCESS) {
            // trigger a sync, notifies user via toast
            SgSyncAdapter.requestSyncImmediate(mContext, SgSyncAdapter.SyncType.DELTA, 0, true);
        }

        if (mListener != null) {
            mListener.onTaskFinished(resultCode);
        }
    }
}
