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
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.battlelancer.seriesguide.util.Utils;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

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
            Utils.trackException(mContext, e);
            Log.w(TAG, e);
            return e.getMessage();
        } catch (OAuthNotAuthorizedException e) {
            Utils.trackException(mContext, e);
            Log.w(TAG, e);
            return e.getMessage();
        } catch (OAuthExpectationFailedException e) {
            Utils.trackException(mContext, e);
            Log.w(TAG, e);
            return e.getMessage();
        } catch (OAuthCommunicationException e) {
            Utils.trackException(mContext, e);
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
