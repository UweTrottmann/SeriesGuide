
package com.battlelancer.seriesguide.getglueapi;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.AnalyticsUtils;

import org.scribe.builder.ServiceBuilder;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class GetGlue {

    private static final String GETGLUE_APIPATH_V2 = "http://api.getglue.com/v2/";

    private static final String GETGLUE_SOURCE = "&source=http://seriesguide.uwetrottmann.com/&app=SeriesGuide";

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
            final Resources res = mContext.getResources();
            try {
                mComment = URLEncoder.encode(mComment, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                mComment = e.getMessage();
                return CHECKIN_FAILED;
            }
            String url = GETGLUE_APIPATH_V2 + GETGLUE_CHECKIN_IMDBID + mImdbId + GETGLUE_SOURCE
                    + "&comment=" + mComment;

            // create a consumer object and configure it with the access
            // token and token secret obtained from the service provider
            try {
                OAuthService service = new ServiceBuilder().provider(GetGlueApi.class)
                        .apiKey(res.getString(R.string.getglue_consumer_key))
                        .apiSecret(res.getString(R.string.getglue_consumer_secret)).build();

                OAuthRequest request = new OAuthRequest(Verb.GET, url);
                Token accessToken = new Token(mPrefs.getString(OAUTH_TOKEN, ""), mPrefs.getString(
                        OAUTH_TOKEN_SECRET, ""));
                service.signRequest(accessToken, request);

                request.send();
            } catch (OAuthException e) {
                e.printStackTrace();
                mComment = e.getMessage();
                return CHECKIN_FAILED;
            }

            return CHECKIN_SUCCESSFUL;
        }

        @Override
        protected void onPostExecute(Integer result) {
            switch (result) {
                case CHECKIN_SUCCESSFUL:
                    Toast.makeText(mContext, R.string.checkinsuccess, Toast.LENGTH_SHORT).show();
                    AnalyticsUtils.getInstance(mContext).trackEvent("Sharing", "GetGlue",
                            "Success", 0);
                    break;
                case CHECKIN_FAILED:
                    Toast.makeText(mContext,
                            mContext.getString(R.string.checkinfailed) + " - " + mComment,
                            Toast.LENGTH_LONG).show();
                    AnalyticsUtils.getInstance(mContext).trackEvent("Sharing", "GetGlue", mComment,
                            0);
                    break;
            }
        }

    }

    public static void clearCredentials(final SharedPreferences prefs) {
        prefs.edit().putString(OAUTH_TOKEN, "").putString(OAUTH_TOKEN_SECRET, "").commit();
    }
}
