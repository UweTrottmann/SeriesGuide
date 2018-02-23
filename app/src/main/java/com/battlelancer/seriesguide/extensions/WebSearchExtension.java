package com.battlelancer.seriesguide.extensions;

import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.api.Episode;
import com.battlelancer.seriesguide.api.Movie;
import com.battlelancer.seriesguide.api.SeriesGuideExtension;
import com.battlelancer.seriesguide.util.ServiceUtils;

/**
 * Performs a web search for a given episode or movie title using a search {@link
 * android.content.Intent}.
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
        publishWebSearchAction(episodeIdentifier,
                String.format("%s %s", episode.getShowTitle(), episode.getTitle()));
    }

    @Override
    protected void onRequest(int movieIdentifier, Movie movie) {
        // we need at least a movie title
        if (TextUtils.isEmpty(movie.getTitle())) {
            return;
        }
        publishWebSearchAction(movieIdentifier, movie.getTitle());
    }

    private void publishWebSearchAction(int identifier, String searchTerm) {
        publishAction(new Action.Builder(getString(R.string.web_search), identifier)
                .viewIntent(ServiceUtils.buildWebSearchIntent(searchTerm))
                .build());
    }
}
