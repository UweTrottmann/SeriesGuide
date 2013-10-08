
package com.battlelancer.seriesguide.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * {@link Service} which executes a {@link SgSyncAdapter} to sync the show
 * database.
 */
public class SgSyncService extends Service {

    private static final String TAG = "SgSyncService";
    private static final Object sSyncAdapterLock = new Object();
    private SgSyncAdapter sSyncAdapter;

    @Override
    public void onCreate() {
        Log.d(TAG, "Creating sync service");
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new SgSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Binding sync adapter");
        return sSyncAdapter.getSyncAdapterBinder();
    }

}
