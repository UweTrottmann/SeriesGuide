package com.battlelancer.seriesguide.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import com.battlelancer.seriesguide.settings.TmdbSettings;
import com.battlelancer.seriesguide.tmdbapi.SgTmdb;
import com.uwetrottmann.tmdb2.entities.Configuration;
import com.uwetrottmann.tmdb2.services.ConfigurationService;
import java.io.IOException;
import retrofit2.Response;

public class TmdbSync {

    Context context;
    ConfigurationService configurationService;
    SyncProgress progress;

    TmdbSync(Context context, ConfigurationService configurationService, SyncProgress progress) {
        this.context = context;
        this.configurationService = configurationService;
        this.progress = progress;
    }

    /**
     * Downloads and stores the latest image url configuration from themoviedb.org.
     */
    public void updateConfiguration(SharedPreferences prefs) {
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
                }
            } else {
                progress.recordError();
                SgTmdb.trackFailedRequest(context, "get config", response);
            }
        } catch (IOException e) {
            progress.recordError();
            SgTmdb.trackFailedRequest(context, "get config", e);
        }
    }
}
