package com.battlelancer.seriesguide.extensions;

import android.content.Intent;
import android.net.Uri;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.api.Episode;
import com.battlelancer.seriesguide.api.Movie;
import com.battlelancer.seriesguide.api.SeriesGuideExtension;

/**
 * Searches vodster.de for shows and movies.
 */
public class VodsterExtension extends SeriesGuideExtension {

    public static final String VODSTER_SEARCH_URL = "http://www.vodster.de?";

    public VodsterExtension() {
        super("VodsterExtension");
    }

    @Override
    protected void onRequest(int episodeIdentifier, Episode episode) {
        publishVodsterAction(episodeIdentifier, "tvdb=" + episode.getShowTvdbId());
    }

    @Override
    protected void onRequest(int movieIdentifier, Movie movie) {
        publishVodsterAction(movieIdentifier, "tmdb=" + movie.getTmdbId());
    }

    private void publishVodsterAction(int identifier, String query) {
        String uri = VODSTER_SEARCH_URL + query;
        publishAction(new Action.Builder(getString(R.string.extension_vodster), identifier)
                .viewIntent(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(uri)))
                .build());
    }
}
