package com.battlelancer.seriesguide.extensions;

import android.content.Intent;
import android.net.Uri;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.api.Episode;
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
        String domain = AmazonSettings.getAmazonCountryDomain(getApplicationContext());
        String uri = SEARCH_URI_PROTOCOL + domain + SEARCH_URI_PATH
                + episode.getShowTitle() + " " + episode.getTitle();

        publishAction(new Action.Builder(getString(R.string.extension_amazon), episodeIdentifier)
                .viewIntent(new Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse(uri)))
                .build());
    }
}
