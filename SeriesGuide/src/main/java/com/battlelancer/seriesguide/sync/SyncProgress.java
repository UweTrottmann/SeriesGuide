package com.battlelancer.seriesguide.sync;

import android.support.annotation.Nullable;
import org.greenrobot.eventbus.EventBus;

public class SyncProgress {

    public enum Step {
        TVDB("TheTVDB"),
        TMDB("TMDb"),
        HEXAGON_EPISODES("Cloud (episodes)"),
        HEXAGON_SHOWS("Cloud (shows)"),
        HEXAGON_MOVIES("Cloud (movies)"),
        HEXAGON_LISTS("Cloud (lists)");

        public final String description;

        Step(String description) {
            this.description = description;
        }
    }

    public enum Result {SUCCESS, FAILURE}

    public static class SyncEvent {
        @Nullable public final Step step;
        @Nullable public final Result result;

        public SyncEvent(@Nullable Step step, @Nullable Result result) {
            this.step = step;
            this.result = result;
        }
    }

    public void publish(Step step) {
        EventBus.getDefault().postSticky(new SyncEvent(step, null));
    }

    public void publish(Result result) {
        SyncEvent lastSyncEvent = EventBus.getDefault().getStickyEvent(SyncEvent.class);
        SyncEvent syncEvent;
        if (lastSyncEvent != null) {
            syncEvent = new SyncEvent(lastSyncEvent.step, result);
        } else {
            syncEvent = new SyncEvent(null, result);
        }
        EventBus.getDefault().postSticky(syncEvent);
    }
}
