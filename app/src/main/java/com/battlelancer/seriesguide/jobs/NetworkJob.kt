package com.battlelancer.seriesguide.jobs

import android.content.Context

interface NetworkJob {

    fun execute(context: Context): JobResult

}