/*
 * Copyright 2011 Uwe Trottmann
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

package com.battlelancer.seriesguide.getglueapi;

import com.actionbarsherlock.app.ActionBar;
import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.ui.BaseActivity;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Executes the OAuthRequestTokenTask to retrieve a request token and authorize
 * it by the user. After the request is authorized, this will get the callback.
 */
public class PrepareRequestTokenActivity extends BaseActivity {

    final String TAG = "PrepareRequestTokenActivity";

    private OAuthConsumer mConsumer;

    private OAuthProvider mProvider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.oauthscreen);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.oauthmessage));
        actionBar.setDisplayShowTitleEnabled(true);

        Resources res = getResources();
        this.mConsumer = new CommonsHttpOAuthConsumer(res.getString(R.string.getglue_consumer_key),
                res.getString(R.string.getglue_consumer_secret));
        this.mProvider = new CommonsHttpOAuthProvider(GetGlue.REQUEST_URL, GetGlue.ACCESS_URL,
                GetGlue.AUTHORIZE_URL);

        Log.i(TAG, "Starting task to retrieve request token.");
        new OAuthRequestTokenTask(this, mConsumer, mProvider).execute();
    }

    /**
     * Called when the OAuthRequestTokenTask finishes (user has authorized the
     * request token). The callback URL will be intercepted here.
     */
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        final Uri uri = intent.getData();
        if (uri != null && uri.getScheme().equals(GetGlue.OAUTH_CALLBACK_SCHEME)) {
            Log.i(TAG, "Callback received, retrieving Access Token");

            new RetrieveAccessTokenTask(mConsumer, mProvider,
                    PreferenceManager.getDefaultSharedPreferences(this)).execute(uri);

            finish();
        }
    }

    public class RetrieveAccessTokenTask extends AsyncTask<Uri, Void, Integer> {

        private static final int AUTH_FAILED = 0;

        private static final int AUTH_SUCCESS = 1;

        private SharedPreferences mPrefs;

        private OAuthProvider mProvider;

        private OAuthConsumer mConsumer;

        public RetrieveAccessTokenTask(OAuthConsumer consumer, OAuthProvider provider,
                SharedPreferences prefs) {
            mPrefs = prefs;
            mProvider = provider;
            mConsumer = consumer;
        }

        /**
         * Retrieve the oauth_verifier, and store the oauth and
         * oauth_token_secret for future API calls.
         */
        @Override
        protected Integer doInBackground(Uri... params) {
            final Uri uri = params[0];
            final String oauth_verifier = uri.getQueryParameter("oauth_verifier");

            try {
                mProvider.retrieveAccessToken(mConsumer, oauth_verifier);

                mPrefs.edit().putString(GetGlue.OAUTH_TOKEN, mConsumer.getToken())
                        .putString(GetGlue.OAUTH_TOKEN_SECRET, mConsumer.getTokenSecret()).commit();

                Log.i(TAG, "OAuth - Access Token Retrieved");
                return AUTH_SUCCESS;
            } catch (OAuthMessageSignerException e) {
                Log.e(TAG, "OAuth - Access Token Retrieval Error", e);
            } catch (OAuthNotAuthorizedException e) {
                Log.e(TAG, "OAuth - Access Token Retrieval Error", e);
            } catch (OAuthExpectationFailedException e) {
                Log.e(TAG, "OAuth - Access Token Retrieval Error", e);
            } catch (OAuthCommunicationException e) {
                Log.e(TAG, "OAuth - Access Token Retrieval Error", e);
            }

            return AUTH_FAILED;
        }

        @Override
        protected void onPostExecute(Integer result) {
            switch (result) {
                case AUTH_SUCCESS:
                    break;
                case AUTH_FAILED:
                    Toast.makeText(getApplicationContext(), getString(R.string.getglue_authfailed),
                            Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

}
