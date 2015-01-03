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

import android.app.Activity;
import android.content.Context;

/**
 * No-op dummy of Amazon IAP manager.
 */
public class AmazonIapManager {

    public static void setup(Context context) {
        // no op
    }

    public static AmazonIapManager get() {
        return null;
    }

    public void requestUserDataAndPurchaseUpdates() {
        // no op
    }

    public void activate() {
        // no op
    }

    public void deactivate() {
        // no op
    }

    public void validateSubscription(Activity activity) {
        // no op
    }
}
