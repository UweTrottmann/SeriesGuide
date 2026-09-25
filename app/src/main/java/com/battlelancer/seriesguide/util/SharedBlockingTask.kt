// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.util

import timber.log.Timber
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import kotlin.concurrent.thread

/**
 * Runs [block] on its own thread, so interrupting a calling thread does not affect it.
 * If a run is active, callers wait for its result instead of starting a new one.
 *
 * Waits for the result even if the calling thread is interrupted, then restores the
 * interrupted state.
 *
 * This is safe to call from multiple threads.
 */
class SharedBlockingTask<T>(
    private val threadName: String,
    private val block: () -> T
) {

    // Lock to synchronize starting a run
    private val lock = Any()

    // Guarded by lock
    private var activeTask: FutureTask<T>? = null

    /**
     * Runs [block] on a new thread, or if a run is active, waits for its result instead.
     *
     * @throws ExecutionException if the run throws.
     */
    fun runOrAwait(): T {
        val task = synchronized(lock) {
            activeTask
                ?.takeIf { !it.isDone }
                ?.also { Timber.d("%s already running, wait", threadName) }
                ?: FutureTask { block() }.also { newTask ->
                    activeTask = newTask
                    thread(name = threadName) { newTask.run() }
                }
        }
        return getUninterruptibly(task)
    }

    private fun getUninterruptibly(task: FutureTask<T>): T {
        var interrupted = false
        try {
            while (true) {
                try {
                    return task.get()
                } catch (_: InterruptedException) {
                    interrupted = true
                }
            }
        } finally {
            if (interrupted) Thread.currentThread().interrupt()
        }
    }
}
