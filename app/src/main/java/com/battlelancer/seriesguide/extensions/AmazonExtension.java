package com.battlelancer.seriesguide.extensions;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.api.Episode;
import com.battlelancer.seriesguide.api.Movie;
import com.battlelancer.seriesguide.api.SeriesGuideExtension;
import com.battlelancer.seriesguide.settings.AmazonSettings;

/**
 * Provides a search link to the users preferred Amazon website.
 */
public class AmazonExtension extends SeriesGuideExtension {

    public static final String SEARCH_URI_PROTOCOL = "http://";
    public static final String SEARCH_URI_PATH = "/s/field-keywords=";

    public AmazonExtension() {
        super("AmazonExtension");
    }

    @Override
    protected void onRequest(int episodeIdentifier, Episode episode) {
        // we need at least a show or an episode title
        if (TextUtils.isEmpty(episode.getShowTitle()) || TextUtils.isEmpty(episode.getTitle())) {
            return;
        }
        publishAmazonAction(episodeIdentifier, episode.getShowTitle() + " " + episode.getTitle());
    }

    @Override
    protected void onRequest(int movieIdentifier, Movie movie) {
        // we need at least a movie title
        if (TextUtils.isEmpty(movie.getTitle())) {
            return;
        }
        publishAmazonAction(movieIdentifier, movie.getTitle());
    }

    private void publishAmazonAction(int identifier, String searchTerm) {
        String domain = AmazonSettings.getAmazonCountryDomain(getApplicationContext());
        String uri = String.format("%s%s%s%s", SEARCH_URI_PROTOCOL, domain, SEARCH_URI_PATH,
                searchTerm);

        publishAction(new Action.Builder(getString(R.string.extension_amazon), identifier)
                .viewIntent(new Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse(uri)))
                .build());
    }
}
