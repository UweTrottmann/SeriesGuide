package com.uwetrottmann.seriesguide;

import android.content.Context;

public class GCMBroadcastReceiver extends com.google.android.gcm.GCMBroadcastReceiver {

    @Override
    protected String getGCMIntentServiceClassName(Context context) {
        return "com.uwetrottmann.seriesguide.GCMIntentService";
    }
}
