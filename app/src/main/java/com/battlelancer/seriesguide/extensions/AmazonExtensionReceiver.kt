package com.battlelancer.seriesguide.extensions;

import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.api.SeriesGuideExtension;
import com.battlelancer.seriesguide.api.SeriesGuideExtensionReceiver;

public class AmazonExtensionReceiver extends SeriesGuideExtensionReceiver {
    @Override
    protected int getJobId() {
        return SgApp.JOB_ID_EXTENSION_AMAZON;
    }

    @Override
    protected Class<? extends SeriesGuideExtension> getExtensionClass() {
        return AmazonExtension.class;
    }
}
