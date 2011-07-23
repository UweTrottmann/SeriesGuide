
package com.battlelancer.seriesguide.getglueapi;

import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.SeriesGuideData;
import com.battlelancer.seriesguide.ui.BaseActivity;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBar;
import android.util.Log;
import android.widget.Toast;

/**
 * Prepares a OAuthConsumer and OAuthProvider OAuthConsumer is configured with
 * the consumer key & consumer secret. OAuthProvider is configured with the 3
 * OAuth endpoints. Execute the OAuthRequestTokenTask to retrieve the request,
 * and authorize the request. After the request is authorized, a callback is
 * made here.
 */
public class PrepareRequestTokenActivity extends BaseActivity {

    final String TAG = getClass().getName();

    private OAuthConsumer consumer;

    private OAuthProvider provider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.oauthscreen);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.oauthmessage));
        actionBar.setDisplayShowTitleEnabled(true);

        try {
            this.consumer = new CommonsHttpOAuthConsumer(Constants.CONSUMER_KEY,
                    Constants.CONSUMER_SECRET);
            this.provider = new CommonsHttpOAuthProvider(GetGlue.REQUEST_URL, GetGlue.ACCESS_URL,
                    GetGlue.AUTHORIZE_URL);

            Log.i(TAG, "Starting task to retrieve request token.");
            new OAuthRequestTokenTask(this, consumer, provider).execute();
        } catch (Exception e) {
            Log.e(TAG, "Error creating consumer / provider", e);
        }
    }

    /**
     * Called when the OAuthRequestTokenTask finishes (user has authorized the
     * request token). The callback URL will be intercepted here.
     */
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Uri uri = intent.getData();
        if (uri != null && uri.getScheme().equals(GetGlue.OAUTH_CALLBACK_SCHEME)) {
            Log.i(TAG, "Callback received : " + uri);
            Log.i(TAG, "Retrieving Access Token");
            new RetrieveAccessTokenTask(consumer, provider, prefs).execute(uri);
            finish();
        }
    }

    public class RetrieveAccessTokenTask extends AsyncTask<Uri, Void, String> {

        private OAuthProvider provider;

        private OAuthConsumer consumer;

        private SharedPreferences prefs;

        public RetrieveAccessTokenTask(OAuthConsumer consumer, OAuthProvider provider,
                SharedPreferences prefs) {
            this.consumer = consumer;
            this.provider = provider;
            this.prefs = prefs;
        }

        /**
         * Retrieve the oauth_verifier, and store the oauth and
         * oauth_token_secret for future API calls.
         */
        @Override
        protected String doInBackground(Uri... params) {
            final Uri uri = params[0];
            final String oauth_verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);

            try {
                provider.retrieveAccessToken(consumer, oauth_verifier);

                final Editor edit = prefs.edit();
                edit.putString(OAuth.OAUTH_TOKEN, consumer.getToken());
                edit.putString(OAuth.OAUTH_TOKEN_SECRET, consumer.getTokenSecret());
                edit.commit();

                String token = prefs.getString(OAuth.OAUTH_TOKEN, "");
                String secret = prefs.getString(OAuth.OAUTH_TOKEN_SECRET, "");

                consumer.setTokenWithSecret(token, secret);

                executeAfterAccessTokenRetrieval();

                Log.i(TAG, "OAuth - Access Token Retrieved");

            } catch (Exception e) {
                Log.e(TAG, "OAuth - Access Token Retrieval Error", e);
                return e.getMessage();
            }

            return null;
        }

        private void executeAfterAccessTokenRetrieval() throws Exception {
            Bundle extras = getIntent().getExtras();
            String comment = extras.getString(SeriesGuideData.KEY_GETGLUE_COMMENT);
            String imdbId = extras.getString(SeriesGuideData.KEY_GETGLUE_IMDBID);

            GetGlue.checkIn(prefs, imdbId, comment);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Toast.makeText(getApplicationContext(),
                        getString(R.string.checkinfailed) + " - " + result, Toast.LENGTH_LONG)
                        .show();
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.checkinsuccess),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

}
