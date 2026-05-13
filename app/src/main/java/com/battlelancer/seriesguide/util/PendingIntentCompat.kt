// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright 2022-2025 Uwe Trottmann

package com.battlelancer.seriesguide.util

import android.app.PendingIntent
import android.os.Build

object PendingIntentCompat {

    val flagMutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.FLAG_MUTABLE
    } else {
        0
    }

}