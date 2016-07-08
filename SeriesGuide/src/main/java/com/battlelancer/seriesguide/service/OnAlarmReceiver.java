package com.battlelancer.seriesguide.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.battlelancer.seriesguide.util.Utils;
import timber.log.Timber;

public class OnAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // postpone notification service launch a minute,
            // we don't want to slow down booting
            Timber.d("Postponing notifications service launch");
            Utils.runNotificationServiceDelayed(context);
        } else {
            // run the notification service right away
            Timber.d("Run notifications service right away");
            Utils.runNotificationService(context);
        }
    }
}
