package com.battlelancer.seriesguide.extensions

import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.api.SeriesGuideExtension
import com.battlelancer.seriesguide.api.SeriesGuideExtensionReceiver

class YouTubeExtensionReceiver : SeriesGuideExtensionReceiver() {
    override fun getJobId(): Int = SgApp.JOB_ID_EXTENSION_YOUTUBE

    override fun getExtensionClass(): Class<out SeriesGuideExtension> =
        YouTubeExtension::class.java
}