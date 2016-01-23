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

package com.battlelancer.seriesguide.traktapi;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.TraktResult;
import com.battlelancer.seriesguide.ui.BaseOAuthActivity;
import com.battlelancer.seriesguide.util.ConnectTraktTask;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.trakt.v2.TraktV2;
import java.math.BigInteger;
import java.security.SecureRandom;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import timber.log.Timber;

/**
 * Starts a trakt OAuth 2.0 authorization flow using the default browser or an embedded {@link
 * android.webkit.WebView} as a fallback.
 */
public class TraktAuthActivity extends BaseOAuthActivity {

    private static final String KEY_STATE = "state";
    private String state;
    private ConnectTraktTaskFragment taskFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Fragment fragment = getSupportFragmentManager().findFragmentByTag("trakt-connect-task");
        if (fragment == null) {
            taskFragment = new ConnectTraktTaskFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(taskFragment, "trakt-connect-task")
                    .commit();
        } else {
            taskFragment = (ConnectTraktTaskFragment) fragment;
        }

        if (savedInstanceState != null) {
            // restore state on recreation
            // (e.g. if launching external browser dropped us out of memory)
            state = savedInstanceState.getString(KEY_STATE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_STATE, state);
    }

    @Override
    @Nullable
    protected String getAuthorizationUrl() {
        state = new BigInteger(130, new SecureRandom()).toString(32);
        try {
            OAuthClientRequest request = TraktV2.getAuthorizationRequest(
                    BuildConfig.TRAKT_CLIENT_ID,
                    BaseOAuthActivity.OAUTH_CALLBACK_URL_CUSTOM,
                    state,
                    null);
            return request.getLocationUri();
        } catch (OAuthSystemException e) {
            Timber.e(e, "Building auth request failed.");
            activateFallbackButtons();
            setMessage(getAuthErrorMessage() + "\n\n(" + e.getMessage() + ")");
        }

        return null;
    }

    @Override
    protected String getAuthErrorMessage() {
        return getString(R.string.trakt_error_credentials);
    }

    @Override
    protected void fetchTokensAndFinish(@Nullable String authCode, @Nullable String state) {
        if (taskFragment.getTask() != null
                && taskFragment.getTask().getStatus() != AsyncTask.Status.FINISHED) {
            // connect task is still running
            setMessage(getString(R.string.waitplease), true);
            return;
        }

        // if state does not match what we sent, drop the auth code
        if (this.state == null || !this.state.equals(state)) {
            Timber.e(OAuthProblemException.error("invalid_state",
                    "State is null or does not match."), "fetchTokensAndFinish: failed.");
            activateFallbackButtons();
            setMessage(getAuthErrorMessage() + "\n\n(State is null or does not match. "
                    + "May indicate cross-site request forgery.)");
            return;
        }

        if (TextUtils.isEmpty(authCode)) {
            // no valid auth code, remain in activity and show fallback buttons
            activateFallbackButtons();
            setMessage(getAuthErrorMessage() + "\n\n(No auth code returned.)");
            return;
        }

        // fetch access token with given OAuth auth code
        setMessage(getString(R.string.waitplease), true);
        ConnectTraktTask task = new ConnectTraktTask(getApplicationContext());
        Utils.executeInOrder(task, authCode);
        taskFragment.setTask(task);
    }

    public void onEventMainThread(ConnectTraktTask.FinishedEvent event) {
        taskFragment.setTask(null);

        int resultCode = event.resultCode;
        if (resultCode == TraktResult.SUCCESS) {
            // if we got here, looks like credentials were stored successfully
            finish();
            return;
        }

        // handle errors
        int errorResId;
        switch (resultCode) {
            case TraktResult.OFFLINE:
                errorResId = R.string.offline;
                break;
            case TraktResult.API_ERROR:
                errorResId = R.string.trakt_error_general;
                break;
            case TraktResult.AUTH_ERROR:
            case TraktResult.ERROR:
            default:
                errorResId = R.string.trakt_error_credentials;
                break;
        }
        setMessage(getString(errorResId));
        activateFallbackButtons();
    }
}
