package com.battlelancer.seriesguide.extensions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static com.battlelancer.seriesguide.api.constants.OutgoingConstants.ACTION_PUBLISH_ACTION;

/**
 * Receives published extension actions and passes them on for processing to {@link
 * ExtensionActionService}.
 */
public class ExtensionActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_PUBLISH_ACTION.equals(intent.getAction())) {
            ExtensionActionService.enqueue(context, intent);
        }
    }
}
