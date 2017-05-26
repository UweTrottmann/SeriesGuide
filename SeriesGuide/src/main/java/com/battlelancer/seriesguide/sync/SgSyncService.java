
package com.battlelancer.seriesguide.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import timber.log.Timber;

/**
 * A {@link Service} that returns an IBinder {@link SgSyncAdapter}, allowing the sync adapter
 * framework to call onPerformSync().
 */
public class SgSyncService extends Service {

    private static final Object syncAdapterLock = new Object();
    private static SgSyncAdapter syncAdapter = null;

    @Override
    public void onCreate() {
        Timber.d("Creating sync service");
        synchronized (syncAdapterLock) {
            if (syncAdapter == null) {
                syncAdapter = new SgSyncAdapter(getApplicationContext());
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Timber.d("Binding sync adapter");
        return syncAdapter.getSyncAdapterBinder();
    }
}
