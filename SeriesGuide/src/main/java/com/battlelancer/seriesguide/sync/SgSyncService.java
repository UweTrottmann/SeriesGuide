
package com.battlelancer.seriesguide.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.battlelancer.seriesguide.SgApp;
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
                sSyncAdapter = new SgSyncAdapter((SgApp) getApplication(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Timber.d("Binding sync adapter");
        return sSyncAdapter.getSyncAdapterBinder();
    }

}
