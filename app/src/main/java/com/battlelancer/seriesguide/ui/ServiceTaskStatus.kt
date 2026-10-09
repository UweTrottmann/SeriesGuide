// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.ui

import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.jobs.FlagJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Set while a service task is running.
 */
class ServiceActive(
    private val shouldSendToHexagon: Boolean,
    private val shouldSendToTrakt: Boolean
) {
    fun shouldDisplayMessage(): Boolean {
        return shouldSendToHexagon || shouldSendToTrakt
    }

    fun getStatusMessage(context: Context): String {
        val statusText = StringBuilder()
        if (shouldSendToHexagon) {
            statusText.append(context.getString(R.string.hexagon_api_queued))
        }
        if (shouldSendToTrakt) {
            if (statusText.isNotEmpty()) {
                statusText.append(" ")
            }
            statusText.append(context.getString(R.string.trakt_submitqueued))
        }
        return statusText.toString()
    }
}

/**
 * Emitted once a service task has completed. It may not have been successful.
 */
class ServiceCompleted(
    val confirmationText: String?,
    val isSuccessful: Boolean,
    val flagJob: FlagJob?
)

/**
 * Holds the status of service tasks (e.g. any Cloud or Trakt action).
 *
 * While a task is running, [active] is non-null. Once a task completes, [active] is reset and
 * [completed] emits.
 */
class ServiceTaskStatus {

    private val _active = MutableStateFlow<ServiceActive?>(null)

    /**
     * Non-null while a service task is running.
     */
    val active: StateFlow<ServiceActive?> = _active

    // Buffer so tryEmit never fails or suspends producers (FlagJobExecutor holds a semaphore while
    // calling this).
    private val _completed = MutableSharedFlow<ServiceCompleted>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Emits when a service task has completed. Not replayed to new collectors.
     */
    val completed: SharedFlow<ServiceCompleted> = _completed

    fun setActive(active: ServiceActive) {
        _active.value = active
    }

    /**
     * Resets [active] and emits to [completed]. Never suspends, so it is safe to call while
     * holding a semaphore.
     */
    fun setCompleted(completed: ServiceCompleted) {
        _active.value = null
        _completed.tryEmit(completed)
    }
}
