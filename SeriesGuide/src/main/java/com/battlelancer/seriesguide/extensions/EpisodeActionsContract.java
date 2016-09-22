package com.battlelancer.seriesguide.extensions;

/**
 * If a fragment wants to display a list of {@linkplain com.battlelancer.seriesguide.api.Action}s
 * for episodes, it should follow these guidelines.
 */
public interface EpisodeActionsContract {

    /**
     * The recommended value to delay the call to {@link #loadEpisodeActions()} from {@link
     * #loadEpisodeActionsDelayed()}.
     */
    int ACTION_LOADER_DELAY_MILLIS = 200;

    /**
     * Implementing fragments should load the latest episode actions with {@link
     * com.battlelancer.seriesguide.loaders.EpisodeActionsLoader}.
     */
    void loadEpisodeActions();

    /**
     * Implementing fragments should call {@link #loadEpisodeActions()} with a delay (e.g. by
     * posting a {@link java.lang.Runnable} with {@link android.os.Handler#postDelayed(Runnable,
     * long)}, delayed by {@link #ACTION_LOADER_DELAY_MILLIS}). If this is called again before the
     * delay expires, the delay should be reset.
     *
     * <p>Call this in e.g. {@link android.support.v4.app.Fragment#onResume()}.
     */
    void loadEpisodeActionsDelayed();

    /**
     * Implementing fragments should call {@link #loadEpisodeActionsDelayed()} when appropriate
     * (e.g. check {@link com.battlelancer.seriesguide.extensions.ExtensionManager.EpisodeActionReceivedEvent#episodeTvdbId}).
     */
    void onEventMainThread(ExtensionManager.EpisodeActionReceivedEvent event);
}
