/*
 * Copyright 2013 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.battlelancer.seriesguide.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.battlelancer.seriesguide.util.FlagTapedTask;
import com.battlelancer.seriesguide.util.FlagTapedTask.Callback;
import com.battlelancer.seriesguide.util.FlagTapedTaskQueue;
import com.google.myjson.GsonBuilder;

public class TraktFlagService extends Service implements Callback {

    private static final String TAG = "TraktFlagService";

    private FlagTapedTaskQueue mQueue;

    private boolean running;

    @Override
    public void onCreate() {
        super.onCreate();
        mQueue = FlagTapedTaskQueue.create(getApplicationContext(), new GsonBuilder().create());
        Log.i(TAG, "Starting service.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        executeNext();
        return START_STICKY;
    }

    private void executeNext() {
        if (running) {
            return;
        }

        FlagTapedTask task = mQueue.peek();
        if (task != null) {
            running = true;
            task.execute(this);
        } else {
            Log.i(TAG, "Stopping service.");
            stopSelf();
        }
    }

    @Override
    public void onSuccess() {
        running = false;
        mQueue.remove();
        executeNext();
    }

    @Override
    public void onFailure() {
        // TODO Auto-generated method stub

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
