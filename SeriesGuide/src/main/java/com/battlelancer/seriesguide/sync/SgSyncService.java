
/*
 * Copyright 2014 Uwe Trottmann
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
 */

package com.battlelancer.seriesguide.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import timber.log.Timber;

/**
 * {@link Service} which executes a {@link SgSyncAdapter} to sync the show
 * database.
 */
public class SgSyncService extends Service {

    private static final Object sSyncAdapterLock = new Object();
    private SgSyncAdapter sSyncAdapter;

    @Override
    public void onCreate() {
        Timber.d("Creating sync service");
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new SgSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Timber.d("Binding sync adapter");
        return sSyncAdapter.getSyncAdapterBinder();
    }

}
