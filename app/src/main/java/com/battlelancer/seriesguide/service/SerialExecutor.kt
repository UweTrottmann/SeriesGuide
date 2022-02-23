package com.battlelancer.seriesguide.service

import android.os.AsyncTask
import java.util.ArrayDeque
import java.util.concurrent.Executor

/**
 * A serial executor that uses the [AsyncTask.THREAD_POOL_EXECUTOR] to execute tasks on.
 *
 * Adapted from [AsyncTask.SERIAL_EXECUTOR], Copyright (C) 2008 The Android Open Source
 * Project, Licensed under the Apache License, Version 2.0.
 */
class SerialExecutor : Executor {
    private val tasks = ArrayDeque<Runnable>()
    private var active: Runnable? = null

    @Synchronized
    override fun execute(r: Runnable) {
        tasks.offer(Runnable {
            try {
                r.run()
            } finally {
                scheduleNext()
            }
        })
        if (active == null) {
            scheduleNext()
        }
    }

    @Synchronized
    private fun scheduleNext() {
        if (tasks.poll().also { active = it } != null) {
            AsyncTask.THREAD_POOL_EXECUTOR.execute(active)
        }
    }
}