package com.battlelancer.seriesguide.extensions

import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.api.SeriesGuideExtension
import com.battlelancer.seriesguide.api.SeriesGuideExtensionReceiver

class WebSearchExtensionReceiver : SeriesGuideExtensionReceiver() {
    override fun getJobId(): Int = SgApp.JOB_ID_EXTENSION_WEBSEARCH

    override fun getExtensionClass(): Class<out SeriesGuideExtension> =
        WebSearchExtension::class.java
}