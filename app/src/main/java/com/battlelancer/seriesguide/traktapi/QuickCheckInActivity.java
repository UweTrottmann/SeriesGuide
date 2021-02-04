package com.battlelancer.seriesguide.traktapi;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentActivity;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.service.NotificationService;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * Blank activity, just used to quickly check into a show/episode.
 */
public class QuickCheckInActivity extends FragmentActivity {

    private static final String EXTRA_LONG_EPISODE_ID = "episode_id";

    public static Intent intent(long episodeId, Context context) {
        return new Intent(context, QuickCheckInActivity.class)
                .putExtra(EXTRA_LONG_EPISODE_ID, episodeId)
                .addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
    }

    @Override
    protected void onCreate(Bundle arg0) {
        // make the activity show the wallpaper, nothing else
        setTheme(android.R.style.Theme_Holo_Wallpaper_NoTitleBar);
        super.onCreate(arg0);

        long episodeId = getIntent().getLongExtra(EXTRA_LONG_EPISODE_ID, 0);
        if (episodeId == 0) {
            finish();
            return;
        }

        // show check-in dialog
        if (!CheckInDialogFragment.show(this, getSupportFragmentManager(), episodeId)) {
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onEvent(GenericCheckInDialogFragment.CheckInDialogDismissedEvent event) {
        // if check-in dialog is dismissed, finish ourselves as well
        finish();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onEvent(TraktTask.TraktActionCompleteEvent event) {
        if (event.traktAction != TraktAction.CHECKIN_EPISODE) {
            return;
        }
        // display status toast about trakt action
        event.handle(this);

        // dismiss notification on successful check-in
        if (event.wasSuccessful) {
            NotificationManagerCompat manager = NotificationManagerCompat.from(
                    getApplicationContext());
            manager.cancel(SgApp.NOTIFICATION_EPISODE_ID);
            // replicate delete intent
            NotificationService.handleDeleteIntent(this, getIntent());
        }
    }
}
