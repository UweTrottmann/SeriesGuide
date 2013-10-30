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

import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.util.FlagTapeEntry;
import com.battlelancer.seriesguide.util.FlagTapeEntryQueue;
import com.battlelancer.seriesguide.util.FlagTapedTask;
import com.battlelancer.seriesguide.util.FlagTapedTask.Callback;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.services.ShowService;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;

public class TraktFlagService extends Service implements Callback {

    private static final String TAG = "TraktFlagService";

    private static final long MAX_RETRY_INTERVAL = 15 * DateUtils.MINUTE_IN_MILLIS;

    private FlagTapeEntryQueue mQueue;

    private boolean running;

    @Override
    public void onCreate() {
        super.onCreate();
        mQueue = FlagTapeEntryQueue.getInstance(getApplicationContext());
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

        FlagTapeEntry entry = mQueue.peek();
        if (entry != null) {
            running = true;

            // build a new FlagTapedTask and execute it
            Trakt manager = ServiceUtils.getTraktServiceManagerWithAuth(
                    getApplicationContext(), false);
            if (manager == null) {
                stop();
                return;
            }
            ShowService showService = manager.showService();

            new FlagTapedTask(getApplicationContext(), showService, entry.action, entry.showId,
                    entry.flags, entry.isFlag)
                    .execute(this);
        } else {
            stop();
        }
    }

    private void stop() {
        Log.i(TAG, "Stopping service.");
        stopSelf();
    }

    @Override
    public void onSuccess() {
        running = false;
        mQueue.remove();
        executeNext();
    }

    @Override
    public void onFailure(boolean isNotConnected) {
        // The user has disconnected from trakt in the meanwhile
        if (!ServiceUtils.hasTraktCredentials(getApplicationContext())) {
            // clear all remaining tasks
            while (mQueue.size() > 0) {
                mQueue.remove();
            }
        }

        /*
         * If the device is not connected to an allowed connection we rely on
         * BaseNavDrawerActivity.onAutoUpdate() to start this service again at most every
         * 15 minutes when the user opens the app.
         */

        // back off exponentially if something went wrong (and we are
        // not just offline)
        if (!isNotConnected) {
            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext());
            long interval = prefs.getLong(SeriesGuidePreferences.KEY_TAPE_INTERVAL,
                    DateUtils.MINUTE_IN_MILLIS);
            if ((interval *= 2) > MAX_RETRY_INTERVAL) {
                interval = MAX_RETRY_INTERVAL;
            }
            prefs.edit().putLong(SeriesGuidePreferences.KEY_TAPE_INTERVAL, interval).commit();

            long wakeUpTime = System.currentTimeMillis() + interval;

            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(this, OnTapeRestartReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
            am.set(AlarmManager.RTC, wakeUpTime, pi);
        }

        stop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
