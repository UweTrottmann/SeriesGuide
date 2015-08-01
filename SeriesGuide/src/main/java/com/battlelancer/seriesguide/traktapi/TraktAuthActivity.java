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

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.BaseOAuthActivity;
import com.battlelancer.seriesguide.ui.ConnectTraktCredentialsFragment;
import com.uwetrottmann.trakt.v2.TraktV2;
import java.math.BigInteger;
import java.security.SecureRandom;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import timber.log.Timber;

/**
 * Starts a trakt OAuth 2.0 authorization flow using an embedded {@link android.webkit.WebView}.
 */
public class TraktAuthActivity extends BaseOAuthActivity {

    private String state;

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.connect_trakt));
    }

    @Override
    protected String getAuthorizationUrl() {
        state = new BigInteger(130, new SecureRandom()).toString(32);
        try {
            OAuthClientRequest request = TraktV2.getAuthorizationRequest(
                    BuildConfig.TRAKT_CLIENT_ID,
                    BaseOAuthActivity.OAUTH_CALLBACK_URL_LOCALHOST,
                    state,
                    null);
            return request.getLocationUri();
        } catch (OAuthSystemException e) {
            Timber.e(e, "Building auth request failed");
        }

        return null;
    }

    @Override
    protected String getAuthErrorMessage() {
        return getString(R.string.trakt_error_credentials);
    }

    @Override
    protected void fetchTokens(@Nullable String authCode, @Nullable String state) {
        // if state does not match what we sent, drop the auth code
        if (this.state != null && !this.state.equals(state)) {
            authCode = null;
        }
        // return auth code to credentials fragment
        Intent intent = new Intent();
        intent.putExtra(ConnectTraktCredentialsFragment.KEY_OAUTH_CODE, authCode);
        setResult(RESULT_OK, intent);
        finish();
    }

}
