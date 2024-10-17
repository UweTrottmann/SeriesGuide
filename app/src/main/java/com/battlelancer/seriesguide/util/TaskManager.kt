// SPDX-License-Identifier: Apache-2.0
// Copyright 2011-2024 Uwe Trottmann

package com.battlelancer.seriesguide.util

import android.content.Context
import android.os.AsyncTask
import android.widget.Toast
import androidx.annotation.MainThread
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.dataliberation.JsonExportTask
import com.battlelancer.seriesguide.shows.tools.AddShowTask
import com.battlelancer.seriesguide.shows.tools.LatestEpisodeUpdateTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Holds on to task instances while they are running to ensure only one is executing at a time.
 */
object TaskManager {

    /**
     * Ensures that only one task that
     * - adds shows,
     * - runs a backup
     * - runs an import
     * runs at a time.
     */
    val addShowOrBackupSemaphore = Semaphore(1)
    private var hasBackupTask: Boolean = false
    private var nextEpisodeUpdateTask: LatestEpisodeUpdateTask? = null

    @MainThread
    @Synchronized
    fun performAddTask(context: Context, show: AddShowTask.Show) {
        performAddTask(context, listOf(show), isSilentMode = false, isMergingShows = false)
    }

    /**
     * Schedule shows to be added to the database.
     *
     * @param isSilentMode   Whether to display status toasts if a show could not be added.
     * @param isMergingShows Whether to set the Hexagon show merged flag to true if all shows were
     */
    @JvmStatic
    @MainThread
    @Synchronized
    fun performAddTask(
        context: Context,
        shows: List<AddShowTask.Show>,
        isSilentMode: Boolean,
        isMergingShows: Boolean
    ) {
        if (!isSilentMode) {
            // notify user here already
            if (shows.size == 1) {
                // say title of show
                val show = shows[0]
                Toast.makeText(
                    context, context.getString(R.string.add_started, show.title),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // generic adding multiple message
                Toast.makeText(context, R.string.add_multiple, Toast.LENGTH_SHORT).show()
            }
        }

        // Queue another add task
        SgApp.coroutineScope.launch(Dispatchers.IO) {
            addShowOrBackupSemaphore.withPermit {
                AddShowTask(context, shows, isSilentMode, isMergingShows).run()
            }
        }
    }

    /**
     * If no [AddShowTask] or [JsonExportTask] created by this [TaskManager] is running a
     * [JsonExportTask] is scheduled in silent mode.
     */
    @MainThread
    @Synchronized
    fun tryBackupTask(context: Context): Boolean {
        if (hasBackupTask) {
            return false
        }
        hasBackupTask = true

        // Queue backup task
        SgApp.coroutineScope.launch(Dispatchers.IO) {
            addShowOrBackupSemaphore.withPermit {
                try {
                    JsonExportTask(
                        context, null,
                        isFullDump = false,
                        isAutoBackupMode = true,
                        type = null
                    ).run()
                } finally {
                    // If backup task gets cancelled for any reason, ensure flag is reset
                    hasBackupTask = false
                }
            }
        }
        return true
    }

    /**
     * Schedules a [LatestEpisodeUpdateTask] for all shows
     * if no other one of this type is currently running.
     */
    @MainThread
    @Synchronized
    fun tryNextEpisodeUpdateTask(context: Context) {
        val nextEpisodeUpdateTask = nextEpisodeUpdateTask
        if (nextEpisodeUpdateTask == null
            || nextEpisodeUpdateTask.status == AsyncTask.Status.FINISHED) {
            LatestEpisodeUpdateTask(context)
                .also { this.nextEpisodeUpdateTask = it }
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    @JvmStatic
    @Synchronized
    fun releaseNextEpisodeUpdateTaskRef() {
        nextEpisodeUpdateTask = null // clear reference to avoid holding on to task context
    }

}
