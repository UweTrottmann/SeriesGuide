// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.jobs

import android.content.Context

interface NetworkJob {

    fun execute(context: Context): NetworkJobResult

}