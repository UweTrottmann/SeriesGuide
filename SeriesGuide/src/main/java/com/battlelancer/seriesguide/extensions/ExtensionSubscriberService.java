package com.battlelancer.seriesguide.extensions;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import com.battlelancer.seriesguide.api.Action;

import static com.battlelancer.seriesguide.api.constants.IncomingConstants.EXTRA_TOKEN;
import static com.battlelancer.seriesguide.api.constants.OutgoingConstants.ACTION_PUBLISH_ACTION;
import static com.battlelancer.seriesguide.api.constants.OutgoingConstants.ACTION_TYPE_EPISODE;
import static com.battlelancer.seriesguide.api.constants.OutgoingConstants.EXTRA_ACTION;
import static com.battlelancer.seriesguide.api.constants.OutgoingConstants.EXTRA_ACTION_TYPE;

/**
 * Catches actions published by enabled extensions.
 */
public class ExtensionSubscriberService extends IntentService {

    public ExtensionSubscriberService() {
        super("ExtensionSubscriberService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // despite guarantees from IntentService, it may get passed a null intent, so check for it
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String intentAction = intent.getAction();
        if (ACTION_PUBLISH_ACTION.equals(intentAction)) {
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

            // extensions may send movie actions as of API 1.3.0
            // older extensions only support episode actions
            int type = ACTION_TYPE_EPISODE;
            if (intent.hasExtra(EXTRA_ACTION_TYPE)) {
                type = intent.getIntExtra(EXTRA_ACTION_TYPE, ACTION_TYPE_EPISODE);
            }

            ExtensionManager.get()
                    .handlePublishedAction(getApplicationContext(), token, action, type);
        }
    }
}
