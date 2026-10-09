// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright 2020, 2023 Uwe Trottmann

package com.battlelancer.seriesguide.jobs

import android.app.Application
import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.getSgAppContainer
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.ui.ServiceActive
import com.battlelancer.seriesguide.ui.ServiceCompleted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

object FlagJobExecutor {

    val semaphore = Semaphore(1)

    /**
     * Executes one job at a time in the order they are submitted
     * (e.g. set watched + set not watched order matters).
     * Runs on IO dispatcher.
     */
    @JvmStatic
    fun execute(context: Context, job: FlagJob) {
        val appContext = context.applicationContext
        SgApp.coroutineScope.launch(Dispatchers.IO) {
            // Semaphore ensures waiting jobs receive permit in order of submission (FIFO).
            semaphore.withPermit {
                val shouldSendToHexagon = job.supportsHexagon()
                        && HexagonSettings.isEnabled(appContext)
                val shouldSendToTrakt = job.supportsTrakt()
                        && TraktCredentials.get(appContext).hasCredentials()
                val requiresNetworkJob = shouldSendToHexagon || shouldSendToTrakt

                // set send flags to false to avoid showing 'Sending to...' message
                val serviceTaskStatus = (appContext as Application).getSgAppContainer()
                    .serviceTaskStatus
                serviceTaskStatus.setActive(
                    ServiceActive(shouldSendToHexagon = false, shouldSendToTrakt = false)
                )

                // update local database and possibly prepare network job
                val isSuccessful = job.applyLocalChanges(appContext, requiresNetworkJob)

                // all actions execute immediately, no need to acknowledge them, so only show errors
                val errorMessageOrNull =
                    if (!isSuccessful) appContext.getString(R.string.database_error) else null
                serviceTaskStatus.setCompleted(
                    ServiceCompleted(errorMessageOrNull, isSuccessful, job)
                )

                if (requiresNetworkJob) {
                    SgSyncAdapter.requestSyncJobsImmediate(appContext)
                }
            }
        }
    }

}