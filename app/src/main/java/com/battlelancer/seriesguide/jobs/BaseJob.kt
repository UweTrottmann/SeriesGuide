package com.battlelancer.seriesguide.jobs

import android.content.ContentValues
import android.content.Context
import com.battlelancer.seriesguide.jobs.episodes.JobAction
import com.battlelancer.seriesguide.provider.SeriesGuideContract

abstract class BaseJob(private val action: JobAction) {

    protected fun persistNetworkJob(context: Context, jobInfo: ByteArray): Boolean {
        val values = ContentValues()
        values.put(SeriesGuideContract.Jobs.TYPE, action.id)
        values.put(SeriesGuideContract.Jobs.CREATED_MS, System.currentTimeMillis())
        values.put(SeriesGuideContract.Jobs.EXTRAS, jobInfo)

        val insert = context.contentResolver.insert(SeriesGuideContract.Jobs.CONTENT_URI, values)

        return insert != null
    }

}