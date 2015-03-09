/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
