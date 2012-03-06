
package com.battlelancer.seriesguide.getglueapi;

import org.scribe.exceptions.OAuthException;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class OAuthRequestTokenTask extends AsyncTask<Void, Void, String> {
    final String TAG = getClass().getName();

    private PrepareRequestTokenActivity mContext;

    private OAuthService mService;

    public OAuthRequestTokenTask(PrepareRequestTokenActivity context, OAuthService service) {
        mContext = context;
        mService = service;
    }

    /**
     * Retrieve the OAuth Request Token and present a browser to the user to
     * authorize the token.
     */
    @Override
    protected String doInBackground(Void... params) {

        try {
            Log.i(TAG, "Retrieving request token from GetGlue servers");
            Token requestToken = mService.getRequestToken();
            String authUrl = mService.getAuthorizationUrl(requestToken);
            // TODO: convert to listener
            mContext.setRequestToken(requestToken);

            Log.i(TAG, "Open a browser to get request token authorized");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                    .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY
                            | Intent.FLAG_FROM_BACKGROUND);
            mContext.startActivity(intent);
        } catch (OAuthException e) {
            return e.getMessage();
        }

        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        if (result != null) {
            Toast.makeText(mContext, result, Toast.LENGTH_LONG).show();
            ((PrepareRequestTokenActivity) mContext).finish();
        }
    }

}
