package com.battlelancer.seriesguide.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.battlelancer.seriesguide.ui.HelpActivity;

/**
 * Receives feedback intent from chrome custom tab.
 */
public class FeedbackBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent chooserIntent = HelpActivity.getFeedbackEmailIntent(context);
        // need to set new task flag, as this is executed from a custom tab
        chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooserIntent);
    }

}
