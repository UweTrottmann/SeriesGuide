/*
 * Copyright 2015 Uwe Trottmann
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

package com.battlelancer.seriesguide.customtabs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.battlelancer.seriesguide.ui.HelpActivity;
import com.battlelancer.seriesguide.util.Utils;

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
        Utils.trackAction(context, "Help", "Feedback (Chrome)");
    }

}
