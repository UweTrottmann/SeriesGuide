package com.battlelancer.seriesguide.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.NotificationManagerCompat;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.EpisodeTools;

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

        int episodeTvdbvId = intent.getIntExtra(NotificationService.EXTRA_EPISODE_TVDBID, -1);
        if (episodeTvdbvId <= 0) {
            return; // not notification set watched action
        }

        // query for episode details
        Cursor query = context.getContentResolver()
                .query(SeriesGuideContract.Episodes.buildEpisodeWithShowUri(episodeTvdbvId),
                        new String[] {
                                SeriesGuideContract.Shows.REF_SHOW_ID,
                                SeriesGuideContract.Episodes.SEASON,
                                SeriesGuideContract.Episodes.NUMBER }, null, null, null);
        if (query == null) {
            return;
        }
        if (!query.moveToFirst()) {
            query.close();
            return;
        }
        int showTvdbId = query.getInt(0);
        int season = query.getInt(1);
        int episode = query.getInt(2);
        query.close();

        // mark episode watched
        EpisodeTools.episodeWatched(context, showTvdbId, episodeTvdbvId, season, episode,
                EpisodeFlags.WATCHED);

        // dismiss the notification
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        manager.cancel(SgApp.NOTIFICATION_EPISODE_ID);
        // replicate delete intent
        NotificationService.handleDeleteIntent(context, intent);
    }
}
