package com.battlelancer.seriesguide.extensions;

import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.api.Episode;
import com.battlelancer.seriesguide.api.Movie;
import com.battlelancer.seriesguide.api.SeriesGuideExtension;
import com.battlelancer.seriesguide.util.ServiceUtils;

/**
 * Searches the Google Play TV and movies section for an episode or movie.
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
        publishGooglePlayAction(episodeIdentifier,
                String.format("%s %s", episode.getShowTitle(), episode.getTitle()));
    }

    @Override
    protected void onRequest(int movieIdentifier, Movie movie) {
        // we need at least a movie title
        if (TextUtils.isEmpty(movie.getTitle())) {
            return;
        }
        publishGooglePlayAction(movieIdentifier, movie.getTitle());
    }

    private void publishGooglePlayAction(int identifier, String searchTerm) {
        publishAction(
                new Action.Builder(getString(R.string.extension_google_play), identifier)
                        .viewIntent(ServiceUtils.buildGooglePlayIntent(searchTerm,
                                getApplicationContext()))
                        .build());
    }
}
