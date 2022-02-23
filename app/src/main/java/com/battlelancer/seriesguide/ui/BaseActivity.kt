package com.battlelancer.seriesguide.ui

import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import com.battlelancer.seriesguide.dataliberation.BackupSettings
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.sync.SgSyncAdapter.Companion.requestSyncIfTime
import com.battlelancer.seriesguide.traktapi.TraktTask.TraktActionCompleteEvent
import com.battlelancer.seriesguide.ui.search.AddShowTask.OnShowAddedEvent
import com.battlelancer.seriesguide.util.DBUtils.DatabaseErrorEvent
import com.battlelancer.seriesguide.util.TaskManager
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Provides some common functionality for triggering sync and auto backup and handling some events.
 *
 * Registers with [EventBus.getDefault] by default to handle various common events,
 * see [registerEventBus] and [unregisterEventBus] to prevent that.
 */
abstract class BaseActivity : BaseThemeActivity() {

    private var handler: Handler? = null
    private var updateShowRunnable: Runnable? = null

    override fun onStart() {
        super.onStart()
        // make sync interfering with backup task less likely
        if (!onAutoBackup()) {
            requestSyncIfTime(this)
        }
        registerEventBus()
    }

    /**
     * Override this to avoid registering with [EventBus.getDefault] in [onStart].
     *
     * See [unregisterEventBus] as well.
     */
    open fun registerEventBus() {
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        val handler = handler
        val updateShowRunnable = updateShowRunnable
        if (handler != null && updateShowRunnable != null) {
            handler.removeCallbacks(updateShowRunnable)
        }
        unregisterEventBus()
    }

    /**
     * Override this to avoid unregistering from [EventBus.getDefault] in [onStop].
     *
     * See [registerEventBus] as well.
     */
    open fun unregisterEventBus() {
        EventBus.getDefault().unregister(this)
    }

    @Subscribe
    fun onEvent(event: OnShowAddedEvent) {
        // display status toast about adding shows
        event.handle(this)
    }

    @Subscribe
    fun onEvent(event: TraktActionCompleteEvent) {
        // display status toast about trakt action
        event.handle(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: DatabaseErrorEvent) {
        event.handle(this)
    }

    /**
     * Periodically do an automatic backup of the show database.
     */
    private fun onAutoBackup(): Boolean {
        if (!BackupSettings.isAutoBackupEnabled(this)) {
            return false
        }

        // If last auto backup failed, show a warning, but run it (if it's time) anyhow.
        if (BackupSettings.isWarnLastAutoBackupFailed(this)) {
            onLastAutoBackupFailed()
        }
        // If copies should be made, but the specified files are invalid,
        // show a warning but run auto backup (if it's time) anyhow.
        else if (BackupSettings.isCreateCopyOfAutoBackup(this)
            && BackupSettings.isMissingAutoBackupFile(this)) {
            onAutoBackupMissingFiles()
        }
        if (!BackupSettings.isTimeForAutoBackup(this)) {
            return false
        }
        TaskManager.getInstance().tryBackupTask(this)
        return true
    }

    /**
     * Implementers may choose to show a warning that the last auto backup has failed.
     */
    protected open fun onLastAutoBackupFailed() {
        // Do nothing.
    }

    /**
     * Implementers may choose to show a warning that auto backup can not complete because not all
     * custom backup files are configured.
     */
    protected open fun onAutoBackupMissingFiles() {
        // Do nothing.
    }

    /**
     * Schedule an update for the given show. Might not run if this show was just updated. Execution
     * is also delayed so it won't reduce UI setup performance (may call this in [onCreate]).
     *
     * See [SgSyncAdapter.requestSyncIfTime].
     */
    protected fun updateShowDelayed(showId: Long) {
        val handler = handler ?: Handler(Looper.getMainLooper())
            .also { handler = it }

        // delay sync request to avoid slowing down UI
        val context = applicationContext
        val updateShowRunnable = Runnable { requestSyncIfTime(context, showId) }
            .also { updateShowRunnable = it }
        handler.postDelayed(updateShowRunnable, DateUtils.SECOND_IN_MILLIS)
    }
}