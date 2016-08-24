package com.battlelancer.seriesguide.extensions;

import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.api.Episode;
import com.battlelancer.seriesguide.api.SeriesGuideExtension;
import com.battlelancer.seriesguide.util.ServiceUtils;

/**
 * Searches the Google Play TV and movies section for an episode.
 */
public class GooglePlayExtension extends SeriesGuideExtension {

    public GooglePlayExtension() {
        super("GooglePlayExtension");
    }

    @Override
    protected void onRequest(int episodeIdentifier, Episode episode) {
        // we need at least a show or an episode title
        if (TextUtils.isEmpty(episode.getShowTitle()) || TextUtils.isEmpty(episode.getTitle())) {
            return;
        }

        publishAction(
                new Action.Builder(getString(R.string.extension_google_play), episodeIdentifier)
                        .viewIntent(ServiceUtils.buildGooglePlayIntent(
                                episode.getShowTitle() + " " + episode.getTitle(),
                                getApplicationContext()))
                        .build());
    }
}
