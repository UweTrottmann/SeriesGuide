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
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.getglueapi.GetGlueXmlParser.Interaction;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.androidutils.AndroidUtils;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class GetGlue {

    private static final String TAG = "GetGlue";

    private static final String GETGLUE_APIPATH_V2 = "http://api.getglue.com/v2/";

    private static final String GETGLUE_SOURCE = "&source=http://seriesgui.de/&app=SeriesGuide";

    public static final String REQUEST_URL = "https://api.getglue.com/oauth/request_token";

    public static final String ACCESS_URL = "https://api.getglue.com/oauth/access_token";

    public static final String AUTHORIZE_URL = "http://getglue.com/oauth/authorize?style=mobile";

    public static final String OAUTH_CALLBACK_SCHEME = "sgoauth";

    public static final String OAUTH_CALLBACK_HOST = "getgluecallback";

    public static final String OAUTH_CALLBACK_URL = OAUTH_CALLBACK_SCHEME + "://"
            + OAUTH_CALLBACK_HOST;

    public static final String OAUTH_TOKEN = "oauth_token";

    public static final String OAUTH_TOKEN_SECRET = "oauth_token_secret";

    private static final String GETGLUE_CHECKIN_IMDBID = "user/addCheckin?objectId=http://www.imdb.com/title/";

    public static boolean isAuthenticated(final SharedPreferences prefs) {
        String token = prefs.getString(OAUTH_TOKEN, "");
        String secret = prefs.getString(OAUTH_TOKEN_SECRET, "");

        if (token == "" || secret == "") {
            return false;
        }

        return true;
    }

    public static class CheckInTask extends AsyncTask<Void, Void, Integer> {

        private static final int CHECKIN_SUCCESSFUL = 0;

        private static final int CHECKIN_FAILED = 1;

        private static final int CHECKIN_OFFLINE = 2;

        private String mImdbId;

        private String mComment;

        private Context mContext;

        private SharedPreferences mPrefs;

        public CheckInTask(String imdbId, String comment, Context context) {
            mImdbId = imdbId;
            mComment = comment;
            mContext = context;
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (!AndroidUtils.isNetworkConnected(mContext)) {
                return CHECKIN_OFFLINE;
            }

            final Resources res = mContext.getResources();
            try {
                mComment = URLEncoder.encode(mComment, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.w(TAG, e);
                mComment = e.getMessage();
                return CHECKIN_FAILED;
            }
            String url = GETGLUE_APIPATH_V2 + GETGLUE_CHECKIN_IMDBID + mImdbId + GETGLUE_SOURCE
                    + "&comment=" + mComment;

            // create a consumer object and configure it with the access
            // token and token secret obtained from the service provider
            OAuthConsumer consumer = new CommonsHttpOAuthConsumer(
                    res.getString(R.string.getglue_consumer_key),
                    res.getString(R.string.getglue_consumer_secret));
            consumer.setTokenWithSecret(mPrefs.getString(OAUTH_TOKEN, ""),
                    mPrefs.getString(OAUTH_TOKEN_SECRET, ""));

            HttpGet request = new HttpGet(url);
            HttpClient httpClient = AndroidUtils.getHttpClient();

            try {
                consumer.sign(request);
            } catch (OAuthMessageSignerException e) {
                Utils.trackException(mContext, e);
                Log.w(TAG, e);
                return CHECKIN_FAILED;
            } catch (OAuthExpectationFailedException e) {
                Utils.trackException(mContext, e);
                Log.w(TAG, e);
                return CHECKIN_FAILED;
            } catch (OAuthCommunicationException e) {
                Utils.trackException(mContext, e);
                Log.w(TAG, e);
                return CHECKIN_FAILED;
            }

            try {
                HttpResponse response = httpClient.execute(request);
                GetGlueXmlParser getGlueXmlParser = new GetGlueXmlParser();
                InputStream responseIn = response.getEntity().getContent();

                int statuscode = response.getStatusLine().getStatusCode();
                if (statuscode == HttpStatus.SC_OK) {
                    Interaction interaction = getGlueXmlParser.parseInteractions(responseIn).get(0);
                    mComment = interaction.title;
                    return CHECKIN_SUCCESSFUL;
                } else {
                    GetGlueXmlParser.Error error = getGlueXmlParser.parseError(responseIn);
                    mComment = error.toString();
                }
            } catch (ClientProtocolException e) {
                Utils.trackException(mContext, e);
                Log.w(TAG, e);
            } catch (IOException e) {
                Utils.trackException(mContext, e);
                Log.w(TAG, e);
            } catch (IllegalStateException e) {
                Utils.trackException(mContext, e);
                Log.w(TAG, e);
            } catch (XmlPullParserException e) {
                Utils.trackException(mContext, e);
                Log.w(TAG, e);
            }

            return CHECKIN_FAILED;
        }

        @Override
        protected void onPostExecute(Integer result) {
            switch (result) {
                case CHECKIN_SUCCESSFUL:
                    Toast.makeText(mContext, mContext.getString(R.string.checkinsuccess, mComment),
                            Toast.LENGTH_SHORT).show();
                    EasyTracker.getTracker().trackEvent("Sharing", "GetGlue", "Success", (long) 0);
                    break;
                case CHECKIN_FAILED:
                    Toast.makeText(mContext, mContext.getString(R.string.checkinfailed),
                            Toast.LENGTH_LONG).show();
                    EasyTracker.getTracker().trackEvent("Sharing", "GetGlue", mComment, (long) 0);
                    break;
                case CHECKIN_OFFLINE:
                    Toast.makeText(mContext, R.string.offline, Toast.LENGTH_LONG).show();
            }
        }

    }

    public static void clearCredentials(final SharedPreferences prefs) {
        prefs.edit().putString(OAUTH_TOKEN, "").putString(OAUTH_TOKEN_SECRET, "").commit();
    }
}
