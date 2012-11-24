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
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.battlelancer.seriesguide.getglueapi.GetGlueXmlParser.GetGlueObject;
import com.battlelancer.seriesguide.getglueapi.GetGlueXmlParser.Interaction;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.client.ClientProtocolException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;

public class GetGlue {

    private static final String TAG = "GetGlue";

    public static final String GETGLUE_APIPATH_V2 = "http://api.getglue.com/v2/";

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

    public static final String GETGLUE_FIND_OBJECTS = "glue/findObjects?q=";

    private static final String GETGLUE_CHECKIN = "user/addCheckin?objectId=";

    private static final String GETGLUE_CHECKIN_IMDBID_PREFIX = "http://www.imdb.com/title/";

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

        private String mObjectId;

        private String mComment;

        private Context mContext;

        public CheckInTask(String objectId, String comment, Context context) {
            mObjectId = objectId;
            mComment = comment;
            mContext = context;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (!AndroidUtils.isNetworkConnected(mContext)) {
                return CHECKIN_OFFLINE;
            }

            // build URL
            StringBuilder url = new StringBuilder();
            url.append(GETGLUE_APIPATH_V2).append(GETGLUE_CHECKIN);
            
            if (!mObjectId.startsWith(GetGlueObject.TVSHOWS)) {
                url.append(GETGLUE_CHECKIN_IMDBID_PREFIX);
            }
            
            url.append(mObjectId).append(GETGLUE_SOURCE);
            
            if (!TextUtils.isEmpty(mComment)) {
                // Encode only whitespaces
                mComment = mComment.replace(" ", "%20");
                url.append("&comment=").append(mComment);
            }

            HttpURLConnection request = buildGetGlueRequest(url.toString(), mContext);
            if (request == null) {
                return CHECKIN_FAILED;
            }

            // clear before potentially using in post
            mComment = "";

            InputStream responseIn = null;
            try {
                request.connect();

                GetGlueXmlParser getGlueXmlParser = new GetGlueXmlParser();
                responseIn = request.getInputStream();

                int statuscode = request.getResponseCode();
                if (statuscode == HttpURLConnection.HTTP_OK) {
                    List<Interaction> interactions = getGlueXmlParser.parseInteractions(responseIn);
                    if (interactions.size() > 0) {
                        mComment = interactions.get(0).title;
                    }
                    return CHECKIN_SUCCESSFUL;
                } else {
                    GetGlueXmlParser.Error error = getGlueXmlParser.parseError(responseIn);
                    if (error != null) {
                        mComment = error.toString();
                    }
                }
            } catch (ClientProtocolException e) {
                Utils.trackExceptionAndLog(mContext, TAG, e);
            } catch (IOException e) {
                Utils.trackExceptionAndLog(mContext, TAG, e);
            } catch (IllegalStateException e) {
                Utils.trackExceptionAndLog(mContext, TAG, e);
            } catch (XmlPullParserException e) {
                Utils.trackExceptionAndLog(mContext, TAG, e);
            } finally {
                if (responseIn != null) {
                    try {
                        responseIn.close();
                    } catch (IOException e) {
                        Log.w(TAG, e);
                        Utils.trackExceptionAndLog(mContext, TAG, e);
                    }
                }
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

    /**
     * Creates a {@link HttpURLConnection} for the given url string and signs it
     * using the stored GetGlue OAuth consumer credentials.
     */
    public static HttpURLConnection buildGetGlueRequest(String url, Context context) {
        final Resources res = context.getResources();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        OAuthConsumer consumer = new DefaultOAuthConsumer(
                res.getString(R.string.getglue_consumer_key),
                res.getString(R.string.getglue_consumer_secret));
        consumer.setTokenWithSecret(prefs.getString(OAUTH_TOKEN, ""),
                prefs.getString(OAUTH_TOKEN_SECRET, ""));

        HttpURLConnection request = null;
        try {
            request = AndroidUtils.buildHttpUrlConnection(url);
            consumer.sign(request);
            return request;
        } catch (OAuthMessageSignerException e) {
            Utils.trackExceptionAndLog(context, TAG, e);
        } catch (OAuthExpectationFailedException e) {
            Utils.trackExceptionAndLog(context, TAG, e);
        } catch (OAuthCommunicationException e) {
            Utils.trackExceptionAndLog(context, TAG, e);
        } catch (IOException e) {
            Utils.trackExceptionAndLog(context, TAG, e);
        }
        return null;
    }

    public static void clearCredentials(final SharedPreferences prefs) {
        prefs.edit().putString(OAUTH_TOKEN, "").putString(OAUTH_TOKEN_SECRET, "").commit();
    }
}
