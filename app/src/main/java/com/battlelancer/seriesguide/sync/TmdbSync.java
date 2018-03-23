package com.battlelancer.seriesguide.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import com.battlelancer.seriesguide.settings.TmdbSettings;
import com.battlelancer.seriesguide.tmdbapi.SgTmdb;
import com.uwetrottmann.tmdb2.entities.Configuration;
import com.uwetrottmann.tmdb2.services.ConfigurationService;
import retrofit2.Response;

public class TmdbSync {

    Context context;
    ConfigurationService configurationService;

    TmdbSync(Context context, ConfigurationService configurationService) {
        this.context = context;
        this.configurationService = configurationService;
    }

    /**
     * Downloads and stores the latest image url configuration from themoviedb.org.
     */
    public boolean updateConfiguration(SharedPreferences prefs) {
        try {
            Response<Configuration> response = configurationService.configuration().execute();
            if (response.isSuccessful()) {
                Configuration config = response.body();
                if (config != null && config.images != null
                        && !TextUtils.isEmpty(config.images.secure_base_url)) {
                    prefs.edit()
                            .putString(TmdbSettings.KEY_TMDB_BASE_URL,
                                    config.images.secure_base_url)
                            .apply();
                    return true;
                }
            } else {
                SgTmdb.trackFailedRequest(context, "get config", response);
            }
        } catch (Exception e) {
            SgTmdb.trackFailedRequest(context, "get config", e);
        }
        return false;
    }
}
