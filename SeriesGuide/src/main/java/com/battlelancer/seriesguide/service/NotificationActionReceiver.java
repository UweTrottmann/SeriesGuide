package com.battlelancer.seriesguide.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.enums.EpisodeFlags;

/**
 * Listens to notification actions, currently only setting an episode watched.
 */
public class NotificationActionReceiver extends BroadcastReceiver {

    public static final String EXTRA_EPISODE_TVDBID
            = "com.battlelancer.seriesguide.EXTRA_EPISODE_TVDBID";

    @Override
    public void onReceive(Context context, Intent intent) {
        int episodeTvdbvId = intent.getIntExtra(EXTRA_EPISODE_TVDBID, -1);
        if (episodeTvdbvId <= 0) {
            return;
        }

        // dismiss the notification
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        manager.cancel(SgApp.NOTIFICATION_EPISODE_ID);
        // replicate delete intent
        NotificationService.handleDeleteIntent(context, intent);

        // mark episode watched
        EpisodeFlagService.enqueueChangeEpisodeFlag(context, episodeTvdbvId, EpisodeFlags.WATCHED);
    }

}
