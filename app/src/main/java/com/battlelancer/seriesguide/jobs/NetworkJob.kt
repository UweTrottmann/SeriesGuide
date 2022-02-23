package com.battlelancer.seriesguide.jobs

import android.content.Context
import com.battlelancer.seriesguide.sync.NetworkJobProcessor.JobResult

interface NetworkJob {

    fun execute(context: Context): JobResult

    companion object {
        const val SUCCESS = 0

        /** Issue connecting or reading a response, should retry.  */
        const val ERROR_CONNECTION = -1
        const val ERROR_TRAKT_AUTH = -2

        /** Issue with request, do not retry.  */
        const val ERROR_TRAKT_CLIENT = -3

        /** Issue with connection or server, do retry.  */
        const val ERROR_TRAKT_SERVER = -4

        /** Show, season or episode not found, do not retry, but notify.  */
        const val ERROR_TRAKT_NOT_FOUND = -5

        /** Issue with the request, do not retry.  */
        const val ERROR_HEXAGON_CLIENT = -6

        /** Issue with connection or server, should retry.  */
        const val ERROR_HEXAGON_SERVER = -7
        const val ERROR_HEXAGON_AUTH = -8
    }
}