package com.battlelancer.seriesguide.extensions;

import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.api.Episode;
import com.battlelancer.seriesguide.api.SeriesGuideExtension;
import com.battlelancer.seriesguide.util.ServiceUtils;

/**
 * Performs a web search for a given episode using a search {@link android.content.Intent}.
 */
public class WebSearchExtension extends SeriesGuideExtension {

    public WebSearchExtension() {
        super("WebSearchExtension");
    }

    @Override
    protected void onRequest(int episodeIdentifier, Episode episode) {
        // we need at least a show or an episode title
        if (TextUtils.isEmpty(episode.getShowTitle()) || TextUtils.isEmpty(episode.getTitle())) {
            return;
        }

        publishAction(new Action.Builder(getString(R.string.web_search), episodeIdentifier)
                .viewIntent(ServiceUtils.buildWebSearchIntent(
                        episode.getShowTitle() + " " + episode.getTitle()))
                .build());
    }
}
