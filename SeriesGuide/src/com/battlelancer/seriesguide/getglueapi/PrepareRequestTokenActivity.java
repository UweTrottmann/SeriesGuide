
package com.battlelancer.seriesguide.getglueapi;

import com.actionbarsherlock.app.ActionBar;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.getglueapi.GetGlue.CheckInTask;
import com.battlelancer.seriesguide.ui.BaseActivity;
import com.battlelancer.seriesguide.util.ShareUtils;

import org.scribe.builder.ServiceBuilder;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import android.content.Context;
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

    private OAuthService mService;

    private Token mRequestToken;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.oauthscreen);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.oauthmessage));
        actionBar.setDisplayShowTitleEnabled(true);

        Resources res = getResources();
        mService = new ServiceBuilder().provider(GetGlueApi.class)
                .apiKey(res.getString(R.string.getglue_consumer_key))
                .apiSecret(res.getString(R.string.getglue_consumer_secret))
                .callback(GetGlue.OAUTH_CALLBACK_URL).build();

        Log.i(TAG, "Starting task to retrieve request token.");
        new OAuthRequestTokenTask(this, mService).execute();
    }

    protected synchronized void setRequestToken(Token requestToken) {
        mRequestToken = requestToken;
    }

    protected synchronized Token getRequestToken() {
        return mRequestToken;
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

            new RetrieveAccessTokenTask(mService, getRequestToken(), this).execute(uri);

            finish();
        }
    }

    public class RetrieveAccessTokenTask extends AsyncTask<Uri, Void, Integer> {

        private static final int AUTH_FAILED = 0;

        private static final int AUTH_SUCCESS = 1;

        private SharedPreferences mPrefs;

        private OAuthService mService;

        private Context mContext;

        public RetrieveAccessTokenTask(OAuthService service, Token requestToken, Context context) {
            mService = service;
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            mRequestToken = requestToken;
            mContext = context;
        }

        /**
         * Retrieve the oauth_verifier, and store the oauth and
         * oauth_token_secret for future API calls.
         */
        @Override
        protected Integer doInBackground(Uri... params) {
            if (mRequestToken == null) {
                Log.e(TAG, "OAuth - Request Token invalid");
                return AUTH_FAILED;
            }

            final Uri uri = params[0];
            Verifier verifier = new Verifier(uri.getQueryParameter("oauth_verifier"));

            try {
                Token accessToken = mService.getAccessToken(mRequestToken, verifier);

                mPrefs.edit().putString(GetGlue.OAUTH_TOKEN, accessToken.getToken())
                        .putString(GetGlue.OAUTH_TOKEN_SECRET, accessToken.getSecret()).commit();

                Log.i(TAG, "OAuth - Access Token Retrieved");
            } catch (OAuthException e) {
                Log.e(TAG, "OAuth - Access Token Retrieval Error", e);
                return AUTH_FAILED;
            }

            return AUTH_SUCCESS;
        }

        @Override
        protected void onPostExecute(Integer result) {
            switch (result) {
                case AUTH_SUCCESS:
                    Bundle extras = getIntent().getExtras();
                    String comment = extras.getString(ShareUtils.KEY_GETGLUE_COMMENT);
                    String imdbId = extras.getString(ShareUtils.KEY_GETGLUE_IMDBID);
                    new CheckInTask(imdbId, comment, mContext).execute();
                    break;
                case AUTH_FAILED:
                    Toast.makeText(getApplicationContext(), getString(R.string.checkinfailed),
                            Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

}
