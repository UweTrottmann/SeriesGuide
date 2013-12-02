package com.battlelancer.seriesguide.backend;

import android.content.Context;

public class GCMBroadcastReceiver extends com.google.android.gcm.GCMBroadcastReceiver {

    @Override
    protected String getGCMIntentServiceClassName(Context context) {
        return "com.battlelancer.seriesguide.backend.GCMIntentService";
    }
}
