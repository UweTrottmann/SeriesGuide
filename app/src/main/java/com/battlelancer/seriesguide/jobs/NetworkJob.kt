// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: GPL-3.0-or-later

package com.battlelancer.seriesguide.jobs

import android.content.Context

interface NetworkJob {

    fun execute(context: Context): NetworkJobResult

}