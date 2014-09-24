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

package com.battlelancer.seriesguide.billing.amazon;

import android.content.Context;
import android.preference.PreferenceManager;

public class AmazonBillingSettings {

    public static final String KEY_BILLING_DISMISSED
            = "com.battlelancer.seriesguide.billing.amazon.firstrun";

    /**
     * Whether {@link com.battlelancer.seriesguide.billing.amazon.AmazonBillingActivity} has been
     * dismissed by the user. If {@code true}, it should not be automatically shown on app launch
     * again.
     */
    public static boolean wasBillingDismissed(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_BILLING_DISMISSED, false);
    }
}
