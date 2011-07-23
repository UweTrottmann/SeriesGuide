
package com.battlelancer.seriesguide.service;

import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.ui.EpisodeDetailsActivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OnAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int episodeId = intent.getIntExtra(Episodes._ID, -1);
        if (episodeId == -1) {
            return;
        }

        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(ns);

        int icon = R.drawable.ic_stat_notification;
        CharSequence tickerText = "Upcoming episode - Fringe: Olivia (8:00 PM on FOX)";
        long when = System.currentTimeMillis();

        Notification notification = new Notification(icon, tickerText, when);
        notification.defaults |= Notification.DEFAULT_LIGHTS;
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        CharSequence contentTitle = "Upcoming episode";
        CharSequence contentText = "Fringe: Olivia (8:00 PM on FOX)";
        Intent notificationIntent = new Intent(context, EpisodeDetailsActivity.class);
        notificationIntent.putExtra(Episodes._ID, String.valueOf(episodeId));
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        notification.setLatestEventInfo(context.getApplicationContext(), contentTitle, contentText,
                contentIntent);

        mNotificationManager.notify(episodeId, notification);
    }

}
