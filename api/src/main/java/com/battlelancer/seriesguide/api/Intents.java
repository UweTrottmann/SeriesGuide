package com.battlelancer.seriesguide.api;

import android.content.Intent;

/**
 * Helper methods to view shows or episodes within SeriesGuide.
 */
public class Intents {

    public static final String ACTION_VIEW_EPISODE
            = "com.battlelancer.seriesguide.api.action.VIEW_EPISODE";

    public static final String ACTION_VIEW_SHOW
            = "com.battlelancer.seriesguide.api.action.VIEW_SHOW";

    public static final String EXTRA_EPISODE_TMDBID = "episode_tmdbid";

    public static final String EXTRA_SHOW_TMDBID = "show_tmdbid";

    /**
     * Builds an implicit {@link android.content.Intent} to view an episode in SeriesGuide. Like
     * any Intent it may throw {@link android.content.ActivityNotFoundException}, e.g. if
     * SeriesGuide (or another app capable of handling this intent) is not available.
     * <p>
     * If the episode does not exist, the user will be asked if the show should be added.
     */
    public static Intent buildViewEpisodeIntent(int showTmdbId, int episodeTmdbId) {
        return new Intent(ACTION_VIEW_EPISODE)
                .putExtra(EXTRA_SHOW_TMDBID, showTmdbId)
                .putExtra(EXTRA_EPISODE_TMDBID, episodeTmdbId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
    }

    /**
     * Builds an implicit {@link android.content.Intent} to view a show in SeriesGuide. Like
     * any Intent it may throw {@link android.content.ActivityNotFoundException}, e.g. if
     * SeriesGuide (or another app capable of handling this intent) is not available.
     *
     * <p> If the show is not added to SeriesGuide, the user will be asked if it should be.
     */
    public static Intent buildViewShowIntent(int showTmdbId) {
        return new Intent(ACTION_VIEW_SHOW)
                .putExtra(EXTRA_SHOW_TMDBID, showTmdbId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
    }
}
