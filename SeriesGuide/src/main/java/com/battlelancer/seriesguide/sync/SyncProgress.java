package com.battlelancer.seriesguide.sync;

import android.content.Context;
import android.support.annotation.Nullable;
import com.battlelancer.seriesguide.R;
import org.greenrobot.eventbus.EventBus;

public class SyncProgress {

    public enum Step {
        TVDB(R.string.tvdb, 0),
        TMDB(R.string.tmdb, 0),
        HEXAGON_EPISODES(R.string.hexagon, R.string.episodes),
        HEXAGON_SHOWS(R.string.hexagon, R.string.shows),
        HEXAGON_MOVIES(R.string.hexagon, R.string.movies),
        HEXAGON_LISTS(R.string.hexagon, R.string.lists),
        TRAKT_EPISODES(R.string.trakt, R.string.episodes),
        TRAKT_RATINGS(R.string.trakt, R.string.ratings),
        TRAKT_MOVIES(R.string.trakt, R.string.movies);

        public final int serviceRes;
        public final int typeRes;

        Step(int serviceRes, int typeRes) {
            this.serviceRes = serviceRes;
            this.typeRes = typeRes;
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

        public String getDescription(Context context) {
            StringBuilder statusText = new StringBuilder(
                    context.getString(R.string.sync_and_update));
            if (step != null) {
                statusText.append(" - ");
                statusText.append(context.getString(step.serviceRes));
                if (step.typeRes != 0) {
                    statusText.append(" - ");
                    statusText.append(context.getString(step.typeRes));
                }
            }
            return statusText.toString();
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
