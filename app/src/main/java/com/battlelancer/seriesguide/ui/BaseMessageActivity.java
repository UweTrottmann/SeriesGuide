package com.battlelancer.seriesguide.ui;

import android.content.Context;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.jobs.FlagJob;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * A {@link BaseActivity} that displays a permanent snack bar
 * if a service action is running (e.g. any Cloud or Trakt action).
 * <p>
 * Service state is determined by the {@link ServiceActiveEvent}
 * and {@link ServiceCompletedEvent} events.
 * <p>
 * Implementers should override {@link #getSnackbarParentView()} and at best
 * supply a CoordinatorLayout to attach it to.
 */
public abstract class BaseMessageActivity extends BaseActivity {

    /**
     * Posted sticky while a service task is running.
     */
    public static class ServiceActiveEvent {
        private final boolean shouldSendToHexagon;
        private final boolean shouldSendToTrakt;

        public ServiceActiveEvent(boolean shouldSendToHexagon, boolean shouldSendToTrakt) {
            this.shouldSendToHexagon = shouldSendToHexagon;
            this.shouldSendToTrakt = shouldSendToTrakt;
        }

        public boolean shouldDisplayMessage() {
            return shouldSendToHexagon || shouldSendToTrakt;
        }

        public String getStatusMessage(Context context) {
            StringBuilder statusText = new StringBuilder();
            if (shouldSendToHexagon) {
                statusText.append(context.getString(R.string.hexagon_api_queued));
            }
            if (shouldSendToTrakt) {
                if (statusText.length() > 0) {
                    statusText.append(" ");
                }
                statusText.append(context.getString(R.string.trakt_submitqueued));
            }
            return statusText.toString();
        }
    }

    /**
     * Posted once a service action has completed. It may not have been successful.
     */
    public static class ServiceCompletedEvent {

        @Nullable public final String confirmationText;
        public boolean isSuccessful;
        @Nullable public final FlagJob flagJob;

        public ServiceCompletedEvent(@Nullable String confirmationText, boolean isSuccessful,
                @Nullable FlagJob flagJob) {
            this.confirmationText = confirmationText;
            this.isSuccessful = isSuccessful;
            this.flagJob = flagJob;
        }
    }

    private Snackbar snackbarProgress;

    @Override
    protected void onStart() {
        super.onStart();

        ServiceActiveEvent event = EventBus.getDefault().getStickyEvent(ServiceActiveEvent.class);
        handleServiceActiveEvent(event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventEpisodeTask(ServiceActiveEvent event) {
        handleServiceActiveEvent(event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventEpisodeTask(ServiceCompletedEvent event) {
        if (event.confirmationText != null) {
            // show a confirmation/error text
            Snackbar snackbarCompleted = Snackbar
                    .make(getSnackbarParentView(), event.confirmationText,
                            event.isSuccessful ? Snackbar.LENGTH_SHORT : Snackbar.LENGTH_LONG);
            // replaces any previous snackbar, including the indefinite progress one
            snackbarCompleted.show();
        } else {
            handleServiceActiveEvent(null);
        }
    }

    /**
     * Return a view to pass to {@link Snackbar#make(View, CharSequence, int) Snackbar.make},
     * ideally a {@link CoordinatorLayout CoordinatorLayout}.
     */
    protected View getSnackbarParentView() {
        return findViewById(android.R.id.content);
    }

    private void handleServiceActiveEvent(@Nullable ServiceActiveEvent event) {
        if (event != null && event.shouldDisplayMessage()) {
            if (snackbarProgress == null) {
                snackbarProgress = Snackbar.make(getSnackbarParentView(),
                        event.getStatusMessage(this), Snackbar.LENGTH_INDEFINITE);
            } else {
                snackbarProgress.setText(event.getStatusMessage(this));
                snackbarProgress.setDuration(BaseTransientBottomBar.LENGTH_INDEFINITE);
            }
            snackbarProgress.show();
        } else if (snackbarProgress != null) {
            snackbarProgress.dismiss();
        }
    }
}
