package com.battlelancer.seriesguide.extensions;

import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.api.SeriesGuideExtension;
import com.battlelancer.seriesguide.api.SeriesGuideExtensionReceiver;

public class YouTubeExtensionReceiver extends SeriesGuideExtensionReceiver {
    @Override
    protected int getJobId() {
        return SgApp.JOB_ID_EXTENSION_YOUTUBE;
    }

    @Override
    protected Class<? extends SeriesGuideExtension> getExtensionClass() {
        return YouTubeExtension.class;
    }
}
