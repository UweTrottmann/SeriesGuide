package com.battlelancer.seriesguide.api;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.JobIntentService;

/**
 * Base class to expose a {@link SeriesGuideExtension} service. See the documentation of {@link
 * SeriesGuideExtension} for how to create an extension.
 */
public abstract class SeriesGuideExtensionReceiver extends BroadcastReceiver {

    /**
     * The {@link Intent} action that this receiver should declare an
     * <code>&lt;intent-filter&gt;</code> for to let SeriesGuide pick it up.
     */
    public static final String ACTION_SERIESGUIDE_EXTENSION
            = "com.battlelancer.seriesguide.api.SeriesGuideExtension";

    /**
     * A unique job id within your app. Used to run the {@link SeriesGuideExtension} {@link
     * JobIntentService}.
     */
    protected abstract int getJobId();

    /**
     * The class implementing {@link SeriesGuideExtension}.
     */
    protected abstract Class<? extends SeriesGuideExtension> getExtensionClass();

    @Override
    public void onReceive(Context context, Intent intent) {
        SeriesGuideExtension.enqueue(context, getExtensionClass(), getJobId(), intent);
    }
}
