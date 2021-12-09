package com.battlelancer.seriesguide.extensions

import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.api.SeriesGuideExtension
import com.battlelancer.seriesguide.api.SeriesGuideExtensionReceiver

class GooglePlayExtensionReceiver : SeriesGuideExtensionReceiver() {
    override fun getJobId(): Int = SgApp.JOB_ID_EXTENSION_GOOGLE_PLAY

    override fun getExtensionClass(): Class<out SeriesGuideExtension> =
        GooglePlayExtension::class.java
}