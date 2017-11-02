package com.battlelancer.seriesguide.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.util.Utils;
import timber.log.Timber;

public class NotificationAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // postpone notification service launch a minute,
            // we don't want to slow down booting
            Timber.d("Postponing notifications service launch");
            Intent i = new Intent(context, NotificationAlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + DateUtils.MINUTE_IN_MILLIS, pi);
            }
        } else {
            // run the notification service right away
            Timber.d("Run notifications service right away");
            Utils.runNotificationService(context);
        }
    }
}
