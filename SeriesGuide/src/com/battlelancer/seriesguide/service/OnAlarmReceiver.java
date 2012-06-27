
package com.battlelancer.seriesguide.service;

import com.battlelancer.seriesguide.util.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OnAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // run the notification service
        Utils.runNotificationService(context);
    }

}
