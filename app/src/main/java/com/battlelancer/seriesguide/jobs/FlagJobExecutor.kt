package com.battlelancer.seriesguide.jobs

import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.ui.BaseMessageActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.greenrobot.eventbus.EventBus

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
                EventBus.getDefault().postSticky(
                    BaseMessageActivity.ServiceActiveEvent(false, false)
                )

                // update local database and possibly prepare network job
                val isSuccessful = job.applyLocalChanges(appContext, requiresNetworkJob)

                EventBus.getDefault().removeStickyEvent(
                    BaseMessageActivity.ServiceActiveEvent::class.java
                )

                val message = if (isSuccessful) {
                    job.getConfirmationText(appContext)
                } else {
                    appContext.getString(R.string.database_error)
                }
                EventBus.getDefault().post(
                    BaseMessageActivity.ServiceCompletedEvent(message, isSuccessful, job)
                )

                if (requiresNetworkJob) {
                    SgSyncAdapter.requestSyncJobsImmediate(appContext)
                }
            }
        }
    }

}