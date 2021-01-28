package com.battlelancer.seriesguide.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationManagerCompat;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags;
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools;

/**
 * Listens to notification actions, currently only setting an episode watched.
 */
public class NotificationActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (NotificationService.ACTION_CLEARED.equals(intent.getAction())) {
            NotificationService.handleDeleteIntent(context, intent);
            return;
        }

        long episodeId = intent.getLongExtra(NotificationService.EXTRA_EPISODE_ID, -1);
        if (episodeId <= 0) {
            return; // not notification set watched action
        }

        // mark episode watched
        EpisodeTools.episodeWatched(context, episodeId, EpisodeFlags.WATCHED);

        // dismiss the notification
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        manager.cancel(SgApp.NOTIFICATION_EPISODE_ID);
        // replicate delete intent
        NotificationService.handleDeleteIntent(context, intent);
    }
}
