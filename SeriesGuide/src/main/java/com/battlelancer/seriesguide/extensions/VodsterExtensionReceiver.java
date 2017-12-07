package com.battlelancer.seriesguide.extensions;

import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.api.SeriesGuideExtension;
import com.battlelancer.seriesguide.api.SeriesGuideExtensionReceiver;

public class VodsterExtensionReceiver extends SeriesGuideExtensionReceiver {
    @Override
    protected int getJobId() {
        return SgApp.JOB_ID_EXTENSION_VODSTER;
    }

    @Override
    protected Class<? extends SeriesGuideExtension> getExtensionClass() {
        return VodsterExtension.class;
    }
}
