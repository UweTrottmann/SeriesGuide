package com.battlelancer.seriesguide.extensions;

import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.api.Episode;
import com.battlelancer.seriesguide.api.Movie;
import com.battlelancer.seriesguide.api.SeriesGuideExtension;
import com.battlelancer.seriesguide.util.ServiceUtils;

/**
 * Searches YouTube for an episode or movie title. Useful for web shows and trailers!
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
        publishYoutubeAction(episodeIdentifier,
                String.format("%s %s", episode.getShowTitle(), episode.getTitle()));
    }

    @Override
    protected void onRequest(int movieIdentifier, Movie movie) {
        // we need a title to search for
        if (TextUtils.isEmpty(movie.getTitle())) {
            return;
        }
        publishYoutubeAction(movieIdentifier, movie.getTitle());
    }

    private void publishYoutubeAction(int identifier, String searchTerm) {
        publishAction(new Action.Builder(getString(R.string.extension_youtube), identifier)
                .viewIntent(ServiceUtils.buildYouTubeIntent(getApplicationContext(), searchTerm))
                .build());
    }
}
