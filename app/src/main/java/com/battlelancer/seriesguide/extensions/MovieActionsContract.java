package com.battlelancer.seriesguide.extensions;

import androidx.fragment.app.Fragment;

/**
 * If a fragment wants to display a list of {@linkplain com.battlelancer.seriesguide.api.Action}s
 * for movies, it should follow these guidelines.
 */
public interface MovieActionsContract {

    /**
     * The recommended value to delay the call to {@link #loadMovieActions()} from {@link
     * #loadMovieActionsDelayed()}.
     */
    int ACTION_LOADER_DELAY_MILLIS = 200;

    /**
     * Implementing fragments should load the latest episode actions with {@link
     * EpisodeActionsLoader}.
     */
    void loadMovieActions();

    /**
     * Implementing fragments should call {@link #loadMovieActions()} with a delay (for example by
     * posting a {@link Runnable} with {@link android.os.Handler#postDelayed(Runnable, long)},
     * delayed by {@link #ACTION_LOADER_DELAY_MILLIS}). If this is called again before the delay
     * expires, the delay should be reset.
     *
     * <p>Call this in for example {@link Fragment#onResume()}.
     */
    void loadMovieActionsDelayed();

    /**
     * Implementing fragments should call {@link #loadMovieActionsDelayed()} when appropriate (for
     * example check {@link ExtensionManager.MovieActionReceivedEvent#movieTmdbId}).
     */
    void onEventMainThread(ExtensionManager.MovieActionReceivedEvent event);
}
