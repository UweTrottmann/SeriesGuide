package com.battlelancer.seriesguide.sync;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.battlelancer.seriesguide.R;
import java.util.LinkedList;
import java.util.List;
import org.greenrobot.eventbus.EventBus;
import timber.log.Timber;

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

    public static class SyncEvent {
        /** If {@code null} syncing has finished. */
        @Nullable public final Step step;
        /** Contains any steps that had an error. */
        @NonNull public final List<Step> stepsWithError;

        public SyncEvent(@Nullable Step step, @NonNull List<Step> stepsWithError) {
            this.step = step;
            this.stepsWithError = stepsWithError;
        }

        public String getDescription(Context context) {
            StringBuilder statusText = new StringBuilder(
                    context.getString(R.string.sync_and_update));

            Step stepToDisplay = getStepToDisplay();
            if (stepToDisplay != null) {
                statusText.append(" - ");
                statusText.append(context.getString(stepToDisplay.serviceRes));
                if (stepToDisplay.typeRes != 0) {
                    statusText.append(" - ");
                    statusText.append(context.getString(stepToDisplay.typeRes));
                }
            }

            return statusText.toString();
        }

        @Nullable
        private Step getStepToDisplay() {
            if (step != null) {
                return step;
            } else if (stepsWithError.size() > 0) {
                // display first step that had error
                return stepsWithError.get(0);
            } else {
                return null;
            }
        }
    }

    @NonNull private final List<Step> stepsWithError = new LinkedList<>();
    @Nullable private Step currentStep;

    public void publish(Step step) {
        currentStep = step;
        EventBus.getDefault().postSticky(new SyncEvent(step, stepsWithError));
        Timber.d("Syncing: %s...", step.name());
    }

    /**
     * Record an error for the last published step.
     */
    public void recordError() {
        if (currentStep != null) {
            stepsWithError.add(currentStep);
            Timber.d("Syncing: %s...FAILED", currentStep.name());
        }
    }

    public void publishFinished() {
        EventBus.getDefault().postSticky(new SyncEvent(null, stepsWithError));
    }
}
