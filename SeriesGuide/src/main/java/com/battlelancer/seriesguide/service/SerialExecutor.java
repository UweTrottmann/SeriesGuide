package com.battlelancer.seriesguide.service;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import java.util.ArrayDeque;
import java.util.concurrent.Executor;

/**
 * A serial executor that uses the {@link AsyncTask#THREAD_POOL_EXECUTOR} to execute tasks on.
 *
 * <p>Adapted from {@link AsyncTask#SERIAL_EXECUTOR}, Copyright (C) 2008 The Android Open Source
 * Project, Licensed under the Apache License, Version 2.0.
 */
class SerialExecutor implements Executor {
    private final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
    private Runnable active;

    public synchronized void execute(@NonNull final Runnable r) {
        tasks.offer(new Runnable() {
            public void run() {
                try {
                    r.run();
                } finally {
                    scheduleNext();
                }
            }
        });
        if (active == null) {
            scheduleNext();
        }
    }

    private synchronized void scheduleNext() {
        if ((active = tasks.poll()) != null) {
            AsyncTask.THREAD_POOL_EXECUTOR.execute(active);
        }
    }
}
