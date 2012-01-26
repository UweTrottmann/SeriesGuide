
package com.battlelancer.seriesguide.getglueapi;

import com.battlelancer.seriesguide.beta.R;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import java.net.URLEncoder;

public class GetGlue {

    // private static final String TAG = GetGlue.class.getName();

    private static final String GETGLUE_APIPATH_V2 = "http://api.getglue.com/v2/";

    private static final String GETGLUE_SOURCE = "&source=http://seriesguide.uwetrottmann.com/&app=SeriesGuide";

    public static final String REQUEST_URL = "http://api.getglue.com/oauth/request_token";

    public static final String ACCESS_URL = "http://api.getglue.com/oauth/access_token";

    public static final String AUTHORIZE_URL = "http://getglue.com/oauth/authorize?style=mobile";

    public static final String OAUTH_CALLBACK_SCHEME = "seriesguide-oauth-getglue";

    public static final String OAUTH_CALLBACK_HOST = "callback";

    public static final String OAUTH_CALLBACK_URL = OAUTH_CALLBACK_SCHEME + "://"
            + OAUTH_CALLBACK_HOST;

    public static boolean isAuthenticated(SharedPreferences prefs) {

        String token = prefs.getString(OAuth.OAUTH_TOKEN, "");
        String secret = prefs.getString(OAuth.OAUTH_TOKEN_SECRET, "");

        if (token == "" || secret == "") {
            return false;
        }

        return true;
    }

    public static void checkIn(SharedPreferences prefs, String imdbId, String comment,
            Context context) throws Exception {

        String token = prefs.getString(OAuth.OAUTH_TOKEN, "");
        String secret = prefs.getString(OAuth.OAUTH_TOKEN_SECRET, "");

        comment = URLEncoder.encode(comment, "UTF-8");
        String requestString = GETGLUE_APIPATH_V2;
        requestString += "user/addCheckin?objectId=http://www.imdb.com/title/" + imdbId
                + GETGLUE_SOURCE + "&comment=" + comment;

        // create a consumer object and configure it with the access
        // token and token secret obtained from the service provider
        Resources res = context.getResources();
        OAuthConsumer consumer = new CommonsHttpOAuthConsumer(
                res.getString(R.string.getglue_consumer_key),
                res.getString(R.string.getglue_consumer_secret));
        consumer.setTokenWithSecret(token, secret);

        // create an HTTP request to a protected resource
        HttpGet request = new HttpGet(requestString);

        // sign the request
        consumer.sign(request);

        // send the request
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = httpClient.execute(request);

        int statuscode = response.getStatusLine().getStatusCode();
        if (statuscode != HttpStatus.SC_OK) {
            throw new Exception("Unexpected server response " + response.getStatusLine() + " for "
                    + request.getRequestLine());
        }
    }

    public static void clearCredentials(SharedPreferences prefs) {
        prefs.edit().putString(OAuth.OAUTH_TOKEN, "").putString(OAuth.OAUTH_TOKEN_SECRET, "")
                .commit();
    }
}
