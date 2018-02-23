package com.battlelancer.seriesguide.extensions;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.api.Action;

import static com.battlelancer.seriesguide.api.constants.IncomingConstants.EXTRA_TOKEN;
import static com.battlelancer.seriesguide.api.constants.OutgoingConstants.ACTION_TYPE_EPISODE;
import static com.battlelancer.seriesguide.api.constants.OutgoingConstants.EXTRA_ACTION;
import static com.battlelancer.seriesguide.api.constants.OutgoingConstants.EXTRA_ACTION_TYPE;

/**
 * Processes actions published by enabled extensions.
 */
public class ExtensionActionService extends JobIntentService {

    public static void enqueue(Context context, Intent actionIntent) {
        enqueueWork(context, ExtensionActionService.class, SgApp.JOB_ID_EXTENSION_ACTIONS_SERVICE,
                actionIntent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        // an extension published a new action
        String token = intent.getStringExtra(EXTRA_TOKEN);

        // extract the action
        Action action = null;
        if (intent.hasExtra(EXTRA_ACTION)) {
            Bundle bundle = intent.getBundleExtra(EXTRA_ACTION);
            if (bundle != null) {
                action = Action.fromBundle(bundle);
            }
        }

        // extensions may send either movie or episode actions as of API 1.3.0
        int type = ACTION_TYPE_EPISODE;
        if (intent.hasExtra(EXTRA_ACTION_TYPE)) {
            type = intent.getIntExtra(EXTRA_ACTION_TYPE, ACTION_TYPE_EPISODE);
        }

        ExtensionManager.get().handlePublishedAction(getApplicationContext(), token, action, type);
    }
}
