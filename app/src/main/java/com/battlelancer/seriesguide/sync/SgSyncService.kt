package com.battlelancer.seriesguide.sync

import android.app.Service
import android.content.Intent
import android.os.IBinder
import timber.log.Timber

/**
 * A [Service] that returns an IBinder [SgSyncAdapter], allowing the sync adapter
 * framework to call onPerformSync().
 */
class SgSyncService : Service() {

    override fun onCreate() {
        Timber.d("Creating sync service")
        synchronized(syncAdapterLock) {
            if (syncAdapter == null) {
                syncAdapter = SgSyncAdapter(applicationContext)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        Timber.d("Binding sync adapter")
        return syncAdapter?.syncAdapterBinder
    }

    companion object {
        private val syncAdapterLock = Any()
        private var syncAdapter: SgSyncAdapter? = null
    }
}