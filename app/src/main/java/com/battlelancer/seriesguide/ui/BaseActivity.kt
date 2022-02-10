package com.battlelancer.seriesguide.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder
import com.battlelancer.seriesguide.R
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
 * Provides some common functionality across all activities like setting the theme, navigation
 * shortcuts and triggering AutoUpdates and AutoBackups.
 *
 * Also registers with [EventBus.getDefault] by default to handle various common events,
 * see [registerEventBus] and [unregisterEventBus] to prevent that.
 */
abstract class BaseActivity : AppCompatActivity() {

    private var handler: Handler? = null
    private var updateShowRunnable: Runnable? = null

    override fun onCreate(arg0: Bundle?) {
        setCustomTheme()
        super.onCreate(arg0)
    }

    protected open fun setCustomTheme() {
        // set a theme based on user preference
        setTheme(SeriesGuidePreferences.THEME)
    }

    /**
     * Implementers must call this in [onCreate] after [setContentView] if they want
     * to use the action bar.
     *
     * If setting a title, might also want to supply a title to the
     * activity with [setTitle] for better accessibility.
     */
    protected open fun setupActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.sgToolbar)
        setSupportActionBar(toolbar)
    }

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                val upIntent = NavUtils.getParentActivityIntent(this)!!
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    // This activity is NOT part of this app's task, so create a new task
                    // when navigating up, with a synthesized back stack.
                    TaskStackBuilder.create(this)
                        // Add all of this activity's parents to the back stack
                        .addNextIntentWithParentStack(upIntent)
                        // Navigate up to the closest parent
                        .startActivities()
                } else {
                    // This activity is part of this app's task, so simply
                    // navigate up to the logical parent activity.
                    NavUtils.navigateUpTo(this, upIntent)
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
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