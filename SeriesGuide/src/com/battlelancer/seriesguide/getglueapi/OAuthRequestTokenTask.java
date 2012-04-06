
package com.battlelancer.seriesguide.getglueapi;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class OAuthRequestTokenTask extends AsyncTask<Void, Void, String> {
    final String TAG = "OAuthRequestTokenTask";

    private Context mContext;

    private OAuthConsumer mConsumer;

    private OAuthProvider mProvider;

    public OAuthRequestTokenTask(Context context, OAuthConsumer consumer, OAuthProvider provider) {
        mContext = context;
        mConsumer = consumer;
        mProvider = provider;
    }

    /**
     * Retrieve the OAuth Request Token and present a browser to the user to
     * authorize the token.
     */
    @Override
    protected String doInBackground(Void... params) {
        try {
            Log.i(TAG, "Retrieving request token from GetGlue servers");
            String authUrl = mProvider.retrieveRequestToken(mConsumer, GetGlue.OAUTH_CALLBACK_URL);

            Log.i(TAG, "Popping a browser with the authorize URL");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                    .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY
                            | Intent.FLAG_FROM_BACKGROUND);
            mContext.startActivity(intent);
        } catch (OAuthMessageSignerException e) {
            Log.w(TAG, e);
            return e.getMessage();
        } catch (OAuthNotAuthorizedException e) {
            Log.w(TAG, e);
            return e.getMessage();
        } catch (OAuthExpectationFailedException e) {
            Log.w(TAG, e);
            return e.getMessage();
        } catch (OAuthCommunicationException e) {
            Log.w(TAG, e);
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
