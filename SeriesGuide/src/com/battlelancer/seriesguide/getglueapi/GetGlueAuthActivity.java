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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;
import com.battlelancer.seriesguide.ui.BaseActivity;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.R;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

/**
 * Executes the OAuthRequestTokenTask to retrieve a request token and authorize
 * it by the user. After the request is authorized, this will get the callback.
 */
public class GetGlueAuthActivity extends BaseActivity {

    final String TAG = "PrepareRequestTokenActivity";

    private OAuthConsumer mConsumer;

    private OAuthProvider mProvider;

    private WebView mWebview;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_PROGRESS);

        mWebview = new WebView(this);
        setContentView(mWebview);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.oauthmessage));
        actionBar.setDisplayShowTitleEnabled(true);

        setSupportProgressBarVisibility(true);

        final SherlockFragmentActivity activity = this;
        mWebview.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                /*
                 * Activities and WebViews measure progress with different
                 * scales. The progress meter will automatically disappear when
                 * we reach 100%.
                 */
                activity.setSupportProgress(progress * 1000);
            }
        });
        mWebview.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description,
                    String failingUrl) {
                Toast.makeText(activity,
                        getString(R.string.getglue_authfailed) + " " + description,
                        Toast.LENGTH_LONG).show();

                finish();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(GetGlue.OAUTH_CALLBACK_URL)) {
                    Uri uri = Uri.parse(url);
                    new RetrieveAccessTokenTask(mConsumer, mProvider,
                            PreferenceManager.getDefaultSharedPreferences(activity)).execute(uri);

                    finish();
                    return true;
                }
                return false;
            }
        });

        // mWebview.getSettings().setJavaScriptEnabled(true);

        Resources res = getResources();
        this.mConsumer = new DefaultOAuthConsumer(res.getString(R.string.getglue_consumer_key),
                res.getString(R.string.getglue_consumer_secret));
        this.mProvider = new DefaultOAuthProvider(GetGlue.REQUEST_URL, GetGlue.ACCESS_URL,
                GetGlue.AUTHORIZE_URL);

        Log.i(TAG, "Starting task to retrieve request token.");
        new OAuthRequestTokenTask(this, mConsumer, mProvider, mWebview).execute();
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

    public static class OAuthRequestTokenTask extends AsyncTask<Void, String, String> {
        final String TAG = "OAuthRequestTokenTask";

        private Context mContext;

        private OAuthConsumer mConsumer;

        private OAuthProvider mProvider;

        private WebView mWebView;

        public OAuthRequestTokenTask(Context context, OAuthConsumer consumer,
                OAuthProvider provider, WebView webView) {
            mContext = context;
            mConsumer = consumer;
            mProvider = provider;
            mWebView = webView;
        }

        /**
         * Retrieve the OAuth Request Token and present a browser to the user to
         * authorize the token.
         */
        @Override
        protected String doInBackground(Void... params) {
            try {
                Log.i(TAG, "Retrieving request token from GetGlue servers");
                String authUrl = mProvider.retrieveRequestToken(mConsumer,
                        GetGlue.OAUTH_CALLBACK_URL);

                Log.i(TAG, "Popping a browser with the authorize URL");
                publishProgress(authUrl);
            } catch (OAuthMessageSignerException e) {
                Utils.trackException(mContext, TAG, e);
                return e.getMessage();
            } catch (OAuthNotAuthorizedException e) {
                Utils.trackException(mContext, TAG, e);
                return e.getMessage();
            } catch (OAuthExpectationFailedException e) {
                Utils.trackException(mContext, TAG, e);
                return e.getMessage();
            } catch (OAuthCommunicationException e) {
                Utils.trackException(mContext, TAG, e);
                return e.getMessage();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            mWebView.loadUrl(values[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Toast.makeText(mContext, result, Toast.LENGTH_LONG).show();
                ((GetGlueAuthActivity) mContext).finish();
            }
        }

    }
}
