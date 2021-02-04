package com.battlelancer.seriesguide.jobs.movies

import android.content.Context
import com.battlelancer.seriesguide.jobs.BaseJob
import com.battlelancer.seriesguide.jobs.FlagJob
import com.battlelancer.seriesguide.jobs.SgJobInfo
import com.battlelancer.seriesguide.jobs.episodes.JobAction
import com.battlelancer.seriesguide.ui.movies.MovieTools.MovieChangedEvent
import com.google.flatbuffers.FlatBufferBuilder
import org.greenrobot.eventbus.EventBus

abstract class MovieJob(
    action: JobAction,
    private val movieTmdbId: Int,
    private val plays: Int
) : BaseJob(action), FlagJob {

    override fun supportsHexagon(): Boolean {
        return true
    }

    override fun supportsTrakt(): Boolean {
        return true
    }

    override fun applyLocalChanges(context: Context, requiresNetworkJob: Boolean): Boolean {
        // prepare network job
        var networkJobInfo: ByteArray? = null
        if (requiresNetworkJob) {
            networkJobInfo = prepareNetworkJob()
            if (networkJobInfo == null) {
                return false
            }
        }

        if (!applyDatabaseUpdate(context, movieTmdbId)) {
            return false
        }

        // persist network job after successful local updates
        if (requiresNetworkJob) {
            if (!persistNetworkJob(context, networkJobInfo!!)) {
                return false
            }
        }

        // post event to update button states
        EventBus.getDefault().post(MovieChangedEvent(movieTmdbId))

        return true
    }

    protected abstract fun applyDatabaseUpdate(context: Context, movieTmdbId: Int): Boolean

    private fun prepareNetworkJob(): ByteArray? {
        val builder = FlatBufferBuilder(0)

        val jobInfo = SgJobInfo.createSgJobInfo(builder, 0, 0, movieTmdbId, plays, 0)

        builder.finish(jobInfo)
        return builder.sizedByteArray()
    }
}