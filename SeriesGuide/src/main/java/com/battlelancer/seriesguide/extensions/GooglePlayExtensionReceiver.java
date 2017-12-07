package com.battlelancer.seriesguide.extensions;

import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.api.SeriesGuideExtension;
import com.battlelancer.seriesguide.api.SeriesGuideExtensionReceiver;

public class GooglePlayExtensionReceiver extends SeriesGuideExtensionReceiver {
    @Override
    protected int getJobId() {
        return SgApp.JOB_ID_EXTENSION_GOOGLE_PLAY;
    }

    @Override
    protected Class<? extends SeriesGuideExtension> getExtensionClass() {
        return GooglePlayExtension.class;
    }
}
