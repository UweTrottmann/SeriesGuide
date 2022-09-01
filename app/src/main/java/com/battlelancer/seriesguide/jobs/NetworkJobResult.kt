package com.battlelancer.seriesguide.jobs

import android.app.PendingIntent

data class NetworkJobResult(
    val successful: Boolean,
    val jobRemovable: Boolean,
    val action: String? = null,
    val error: String? = null,
    val item: String? = null,
    val contentIntent: PendingIntent? = null
)
