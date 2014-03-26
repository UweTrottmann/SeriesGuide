/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.extensions;

/**
 * If a fragment wants to display a list of {@linkplain com.battlelancer.seriesguide.api.Action}s,
 * it should follow these guidelines.
 */
public interface ActionsFragmentContract {

    /**
     * The recommended value to delay the call to {@link #loadEpisodeActions()} from {@link
     * #loadEpisodeActionsDelayed()}.
     */
    public static final int ACTION_LOADER_DELAY_MILLIS = 200;

    /**
     * Implementing fragments should load the latest episode actions with {@link
     * com.battlelancer.seriesguide.loaders.EpisodeActionsLoader}.
     */
    abstract void loadEpisodeActions();

    /**
     * Implementing fragments should call {@link #loadEpisodeActions()} with a delay (e.g. by
     * posting a {@link java.lang.Runnable} with {@link android.os.Handler#postDelayed(Runnable,
     * long)}, delayed by {@link #ACTION_LOADER_DELAY_MILLIS}). If this is called again before the
     * delay expires, the delay should be reset.
     * <p/>Call this in e.g. {@link android.support.v4.app.Fragment#onResume()}.
     */
    void loadEpisodeActionsDelayed();

    /**
     * Implementing fragments should call {@link #loadEpisodeActionsDelayed()} when appropriate
     * (e.g. check {@link com.battlelancer.seriesguide.extensions.ExtensionManager.EpisodeActionReceivedEvent#episodeTvdbId}).
     */
    public abstract void onEventMainThread(ExtensionManager.EpisodeActionReceivedEvent event);
}
