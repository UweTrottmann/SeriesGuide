package com.battlelancer.seriesguide.util

import android.app.PendingIntent
import android.os.Build

object PendingIntentCompat {

    val flagMutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.FLAG_MUTABLE
    } else {
        0
    }

    val flagImmutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_IMMUTABLE
    } else {
        0
    }

}