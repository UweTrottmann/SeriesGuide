package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.enums.Result;
import com.battlelancer.seriesguide.enums.TraktResult;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.settings.TraktOAuthSettings;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.AccessToken;
import com.uwetrottmann.trakt5.entities.Settings;
import com.uwetrottmann.trakt5.services.Users;
import java.io.IOException;
import javax.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import retrofit2.Response;

/**
 * Expects a valid trakt OAuth auth code. Retrieves the access token and username for the associated
 * user. If successful, the credentials are stored.
 */
public class ConnectTraktTask extends AsyncTask<String, Void, Integer> {

    public class FinishedEvent {
        /**
         * One of {@link com.battlelancer.seriesguide.enums.NetworkResult}.
         */
        public int resultCode;

        public FinishedEvent(int resultCode) {
            this.resultCode = resultCode;
        }
    }

    private final Context context;
    @Inject TraktV2 trakt;
    @Inject Users traktUsers;

    public ConnectTraktTask(Context context) {
        this.context = context;
        SgApp.getServicesComponent(context).inject(this);
    }

    @Override
    protected Integer doInBackground(String... params) {
        // check for connectivity
        if (!AndroidUtils.isNetworkConnected(context)) {
            return TraktResult.OFFLINE;
        }

        // get account data
        String authCode = params[0];

        // check if we have any usable data
        if (TextUtils.isEmpty(authCode)) {
            return TraktResult.AUTH_ERROR;
        }

        // get access token
        String accessToken = null;
        String refreshToken = null;
        long expiresIn = -1;
        try {
            Response<AccessToken> response = trakt.exchangeCodeForAccessToken(authCode);
            if (response.isSuccessful()) {
                accessToken = response.body().access_token;
                refreshToken = response.body().refresh_token;
                expiresIn = response.body().expires_in;
            } else {
                SgTrakt.trackFailedRequest(context, "get access token", response);
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(context, "get access token", e);
        }

        // did we obtain all required data?
        if (TextUtils.isEmpty(accessToken) || TextUtils.isEmpty(refreshToken) || expiresIn < 1) {
            return TraktResult.AUTH_ERROR;
        }

        // store the access token, refresh token and expiry time
        TraktCredentials.get(context).storeAccessToken(accessToken);
        if (!TraktCredentials.get(context).hasCredentials()) {
            return Result.ERROR; // saving access token failed, abort.
        }
        if (!TraktOAuthSettings.storeRefreshData(context, refreshToken, expiresIn)) {
            return Result.ERROR; // saving refresh token failed, abort.
        }

        // get user and display name
        String username = null;
        String displayname = null;
        try {
            Response<Settings> response = traktUsers.settings().execute();
            if (response.isSuccessful()) {
                if (response.body().user != null) {
                    username = response.body().user.username;
                    displayname = response.body().user.name;
                }
            } else {
                SgTrakt.trackFailedRequest(context, "get user settings", response);
                if (SgTrakt.isUnauthorized(response)) {
                    // access token already is invalid, remove it :(
                    TraktCredentials.get(context).removeCredentials();
                    return TraktResult.AUTH_ERROR;
                }
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(context, "get user settings", e);
            return AndroidUtils.isNetworkConnected(context)
                    ? TraktResult.API_ERROR : TraktResult.OFFLINE;
        }

        // did we obtain a username (display name is not required)?
        if (TextUtils.isEmpty(username)) {
            return TraktResult.API_ERROR;
        }
        TraktCredentials.get(context).storeUsername(username, displayname);

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
                context)
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

        editor.apply();

        return Result.SUCCESS;
    }

    @Override
    protected void onPostExecute(Integer resultCode) {
        if (resultCode == Result.SUCCESS) {
            // trigger a sync, notifies user via toast
            SgSyncAdapter.requestSyncImmediate(context, SgSyncAdapter.SyncType.DELTA, 0, true);
        }

        EventBus.getDefault().post(new FinishedEvent(resultCode));
    }
}
