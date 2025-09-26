// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide.util

import android.os.Build

object AndroidTools {

    val isManufacturerHuawei = "Huawei".equals(Build.MANUFACTURER, ignoreCase = true)

}