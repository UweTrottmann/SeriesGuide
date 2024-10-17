// SPDX-License-Identifier: Apache-2.0
// Copyright 2023 Uwe Trottmann

package com.battlelancer.seriesguide.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import android.widget.Toast
import androidx.annotation.MainThread
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.dataliberation.JsonExportTask
import com.battlelancer.seriesguide.shows.search.discover.SearchResult
import com.battlelancer.seriesguide.shows.tools.AddShowTask
import com.battlelancer.seriesguide.shows.tools.LatestEpisodeUpdateTask
import kotlinx.coroutines.Job

/**
 * Holds on to task instances while they are running to ensure only one is executing at a time.
 */
object TaskManager {

    @SuppressLint("StaticFieldLeak") // AddShowTask holds an application context
    private var addShowTask: AddShowTask? = null
    private var backupTask: Job? = null
    private var nextEpisodeUpdateTask: LatestEpisodeUpdateTask? = null

    @MainThread
    @Synchronized
    fun performAddTask(context: Context, show: SearchResult) =
        performAddTask(context, listOf(show), isSilentMode = false, isMergingShows = false)

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
        shows: List<SearchResult>,
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

        // add the show(s) to a running add task or create a new one
        if (!isAddTaskRunning || !addShowTask!!.addShows(shows, isSilentMode, isMergingShows)) {
            AddShowTask(context, shows, isSilentMode, isMergingShows)
                .also { this.addShowTask = it }
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    @Synchronized
    fun releaseAddTaskRef() {
        addShowTask = null // clear reference to avoid holding on to task context
    }

    val isAddTaskRunning: Boolean
        get() = !(addShowTask == null || addShowTask!!.status == AsyncTask.Status.FINISHED)

    /**
     * If no [AddShowTask] or [JsonExportTask] created by this [TaskManager] is running a
     * [JsonExportTask] is scheduled in silent mode.
     */
    @MainThread
    @Synchronized
    fun tryBackupTask(context: Context): Boolean {
        val backupTask = backupTask
        if (!isAddTaskRunning
            && (backupTask == null || backupTask.isCompleted)) {
            val exportTask = JsonExportTask(context, null, false, true, null)
            this.backupTask = exportTask.launch()
            return true
        }
        return false
    }

    @Synchronized
    fun releaseBackupTaskRef() {
        backupTask = null // clear reference to avoid holding on to task context
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
