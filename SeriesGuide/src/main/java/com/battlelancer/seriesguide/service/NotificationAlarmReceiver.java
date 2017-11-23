package com.battlelancer.seriesguide.service;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.text.format.DateUtils;
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

            // as jobs are not allowed to run while the device is idle, use an AsyncTask instead
            final PendingResult pendingResult = goAsync();
            final NotificationService notificationService = new NotificationService(context);
            @SuppressLint("StaticFieldLeak")
            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... voids) {
                    notificationService.run();
                    pendingResult.finish();
                    return null;
                }
            };
            // run in serial to avoid alarm scheduling conflicts
            // separate serial executor as AsyncTask one might be busy longer than onReceive timeout
            task.executeOnExecutor(NotificationService.SERIAL_EXECUTOR);
        }
    }
}
