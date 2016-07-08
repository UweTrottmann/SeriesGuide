package com.battlelancer.seriesguide.thetvdbapi;

import android.content.Context;
import android.content.SharedPreferences;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.thetvdb.TheTvdb;
import okhttp3.OkHttpClient;

/**
 * Extends {@link TheTvdb} to use our own caching OkHttp client and a preferences file to store the
 * current JSON web token long term. The token is currently valid 24 hours, so preserve it if the
 * app gets killed to avoid an additional login endpoint call.
 */
public class SgTheTvdb extends TheTvdb {

    private static final String PREFERENCE_FILE = "thetvdb-prefs";
    private static final String KEY_JSON_WEB_TOKEN = "token";

    private final Context context;
    private final SharedPreferences preferences;

    public SgTheTvdb(Context context) {
        super(BuildConfig.TVDB_API_KEY);
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);
    }

    @Override
    public String jsonWebToken() {
        return preferences.getString(KEY_JSON_WEB_TOKEN, null);
    }

    @Override
    public void jsonWebToken(String value) {
        preferences.edit()
                .putString(KEY_JSON_WEB_TOKEN, value)
                .apply();
    }

    @Override
    protected synchronized OkHttpClient okHttpClient() {
        return ServiceUtils.getCachingOkHttpClient(context);
    }
}
