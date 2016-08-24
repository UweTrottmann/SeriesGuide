package com.battlelancer.seriesguide.extensions;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import com.battlelancer.seriesguide.api.Action;

import static com.battlelancer.seriesguide.api.constants.IncomingConstants.EXTRA_TOKEN;
import static com.battlelancer.seriesguide.api.constants.OutgoingConstants.ACTION_PUBLISH_ACTION;
import static com.battlelancer.seriesguide.api.constants.OutgoingConstants.EXTRA_ACTION;

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

            Action action = null;
            if (intent.hasExtra(EXTRA_ACTION)) {
                Bundle bundle = intent.getBundleExtra(EXTRA_ACTION);
                if (bundle != null) {
                    action = Action.fromBundle(bundle);
                }
            }

            ExtensionManager.getInstance(this).handlePublishedAction(token, action);
        }
    }
}
