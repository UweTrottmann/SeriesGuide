package com.battlelancer.seriesguide.extensions;

import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.api.Episode;
import com.battlelancer.seriesguide.api.SeriesGuideExtension;
import com.battlelancer.seriesguide.util.ServiceUtils;

/**
 * Searches YouTube for an episode. Useful for web shows!
 */
public class YouTubeExtension extends SeriesGuideExtension {

    public YouTubeExtension() {
        super("YouTubeExtension");
    }

    @Override
    protected void onRequest(int episodeIdentifier, Episode episode) {
        // we need at least a show or an episode title
        if (TextUtils.isEmpty(episode.getShowTitle()) || TextUtils.isEmpty(episode.getTitle())) {
            return;
        }

        publishAction(new Action.Builder(getString(R.string.extension_youtube), episodeIdentifier)
                .viewIntent(ServiceUtils.buildYouTubeIntent(getApplicationContext(),
                        episode.getShowTitle() + " " + episode.getTitle()))
                .build());
    }
}
