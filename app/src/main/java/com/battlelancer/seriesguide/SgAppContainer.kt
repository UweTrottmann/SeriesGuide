// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide

import android.content.Context
import com.battlelancer.seriesguide.diagnostics.DebugLogBuffer

class SgAppContainer(context: Context) {

    val debugLogBuffer by lazy { DebugLogBuffer(context) }

}
