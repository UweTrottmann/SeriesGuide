package com.battlelancer.seriesguide.ui

import android.content.ContentResolver
import android.content.Intent
import android.content.SyncStatusObserver
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatDelegate
import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.SgApp.Companion.getServicesComponent
import com.battlelancer.seriesguide.backend.CloudSetupActivity
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.dataliberation.BackupSettings
import com.battlelancer.seriesguide.dataliberation.DataLiberationActivity
import com.battlelancer.seriesguide.preferences.MoreOptionsActivity
import com.battlelancer.seriesguide.stats.StatsActivity
import com.battlelancer.seriesguide.sync.AccountUtils
import com.battlelancer.seriesguide.ui.ShowsActivity
import com.battlelancer.seriesguide.util.SupportTheDev
import com.battlelancer.seriesguide.util.SupportTheDev.buildSnackbar
import com.battlelancer.seriesguide.util.Utils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

/**
 * Activities at the top of the navigation hierarchy, displaying a bottom navigation bar.
 * Implementers must set it up with [setupBottomNavigation].
 *
 * They should also override [snackbarParentView] and supply a CoordinatorLayout.
 * It is used to show snack bars for important warnings (e.g. auto backup failed, Cloud signed out).
 *
 * Also provides support for an optional sync progress bar (see [setupSyncProgressBar]).
 */
abstract class BaseTopActivity : BaseMessageActivity() {

    private var syncProgressBar: View? = null
    private var syncObserverHandle: Any? = null
    private var snackbar: Snackbar? = null

    override fun setupActionBar() {
        super.setupActionBar()
        supportActionBar?.setHomeButtonEnabled(false)
    }

    fun setupBottomNavigation(@IdRes selectedItemId: Int) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = selectedItemId
        bottomNav.setOnNavigationItemSelectedListener { item: MenuItem ->
            onNavItemClick(item.itemId)
            false // Do not change selected item.
        }
    }

    private fun onNavItemClick(itemId: Int) {
        var launchIntent: Intent? = null
        when (itemId) {
            R.id.navigation_item_shows -> {
                if (this is ShowsActivity) {
                    onSelectedCurrentNavItem()
                    return
                }
                launchIntent = Intent(this, ShowsActivity::class.java)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                                or Intent.FLAG_ACTIVITY_NEW_TASK
                                or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
            }
            R.id.navigation_item_lists -> {
                if (this is ListsActivity) {
                    onSelectedCurrentNavItem()
                    return
                }
                launchIntent = Intent(this, ListsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            R.id.navigation_item_movies -> {
                if (this is MoviesActivity) {
                    onSelectedCurrentNavItem()
                    return
                }
                launchIntent = Intent(this, MoviesActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            R.id.navigation_item_stats -> {
                if (this is StatsActivity) {
                    onSelectedCurrentNavItem()
                    return
                }
                launchIntent = Intent(this, StatsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            R.id.navigation_item_more -> {
                if (this is MoreOptionsActivity) {
                    onSelectedCurrentNavItem()
                    return
                }
                launchIntent = Intent(this, MoreOptionsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        }
        if (launchIntent != null) {
            startActivity(launchIntent)
            overridePendingTransition(R.anim.activity_fade_enter_sg, R.anim.activity_fade_exit_sg)
        }
    }

    /**
     * Called if the currently active nav item was clicked.
     * Implementing activities might want to use this to scroll contents to the top.
     */
    protected open fun onSelectedCurrentNavItem() {
        // Do nothing by default.
    }

    /**
     * Implementing classes may call this in [onCreate] to set up a
     * progress bar which displays when syncing.
     */
    protected fun setupSyncProgressBar(@IdRes progressBarId: Int) {
        syncProgressBar = findViewById<View>(progressBarId)
            .also { it.visibility = View.GONE }
    }

    override fun onStart() {
        super.onStart()
        if (Utils.hasAccessToX(this) && HexagonSettings.shouldValidateAccount(this)) {
            onShowCloudAccountWarning()
        }
        if (SupportTheDev.shouldAsk(this)) {
            askForSupport()
        }
    }

    override fun onResume() {
        super.onResume()
        if (syncProgressBar != null) {
            // watch for sync state changes
            syncStatusObserver.onStatusChanged(0)
            val mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING or
                    ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE
            syncObserverHandle = ContentResolver.addStatusChangeListener(mask, syncStatusObserver)
        }
    }

    override fun onPause() {
        super.onPause()

        // stop listening to sync state changes
        if (syncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(syncObserverHandle)
            syncObserverHandle = null
        }
    }

    override fun onStop() {
        super.onStop()
        // dismiss any snackbar to avoid it getting restored
        // if condition that led to its display is no longer true
        val snackbar = snackbar
        if (snackbar != null && snackbar.isShown) {
            snackbar.dismiss()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()

        // use special animation when navigating away from a top activity
        // but not when exiting the app (use the default system animations)
        if (!isTaskRoot) {
            overridePendingTransition(R.anim.activity_fade_enter_sg, R.anim.activity_fade_exit_sg)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (BuildConfig.DEBUG) {
            menu.add(0, 0, 0, "[Debug] Switch theme")
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == OPTIONS_SWITCH_THEME_ID) {
            val isNightMode =
                AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
            AppCompatDelegate.setDefaultNightMode(
                if (isNightMode) {
                    AppCompatDelegate.MODE_NIGHT_NO
                } else {
                    AppCompatDelegate.MODE_NIGHT_YES
                }
            )
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onLastAutoBackupFailed() {
        val snackbar = snackbar
        if (snackbar != null && snackbar.isShown) {
            Timber.d("NOT showing auto backup failed message: existing snackbar.")
            return
        }
        val newSnackbar = Snackbar.make(
            snackbarParentView,
            R.string.autobackup_failed,
            Snackbar.LENGTH_INDEFINITE
        )

        // Manually increase max lines.
        val textView = newSnackbar.view
            .findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.maxLines = 5

        newSnackbar
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar, event: Int) {
                    if (event == DISMISS_EVENT_ACTION
                        || event == DISMISS_EVENT_SWIPE) {
                        Timber.i("Has seen last auto backup failed.")
                        BackupSettings.setHasSeenLastAutoBackupFailed(this@BaseTopActivity)
                    }
                }
            })
            .setAction(R.string.preferences) {
                startActivity(
                    DataLiberationActivity.intentToShowAutoBackup(
                        this
                    )
                )
            }
            .show()
        this.snackbar = newSnackbar
    }

    override fun onAutoBackupMissingFiles() {
        val snackbar = snackbar
        if (snackbar != null && snackbar.isShown) {
            Timber.d("NOT showing backup files warning: existing snackbar.")
            return
        }
        val newSnackbar = Snackbar.make(
            snackbarParentView,
            R.string.autobackup_files_missing,
            Snackbar.LENGTH_INDEFINITE
        )

        // Manually increase max lines.
        val textView = newSnackbar.view
            .findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.maxLines = 5

        newSnackbar.setAction(R.string.preferences) {
            startActivity(
                DataLiberationActivity.intentToShowAutoBackup(this)
            )
        }
        newSnackbar.show()
        this.snackbar = newSnackbar
    }

    protected fun onShowCloudAccountWarning() {
        val snackbar = snackbar
        if (snackbar != null && snackbar.isShown) {
            Timber.d("NOT showing Cloud account warning: existing snackbar.")
            return
        }
        val newSnackbar = Snackbar
            .make(
                snackbarParentView, R.string.hexagon_signed_out,
                Snackbar.LENGTH_INDEFINITE
            )
        newSnackbar.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(snackbar: Snackbar, event: Int) {
                if (event == DISMISS_EVENT_SWIPE) {
                    // user has dismissed warning, so disable Cloud
                    val hexagonTools = getServicesComponent(this@BaseTopActivity)
                        .hexagonTools()
                    hexagonTools.removeAccountAndSetDisabled()
                }
            }
        }).setAction(R.string.hexagon_signin) {
            // forward to cloud setup which can help fix the account issue
            startActivity(Intent(this@BaseTopActivity, CloudSetupActivity::class.java))
        }.show()
        this.snackbar = newSnackbar
    }

    private fun askForSupport() {
        val snackbar = snackbar
        if (snackbar != null && snackbar.isShown) {
            Timber.d("NOT asking for support: existing snackbar.")
            return
        }
        val newSnackbar = buildSnackbar(this, snackbarParentView)
        newSnackbar.show()
        this.snackbar = newSnackbar
    }

    /**
     * Shows or hides the indeterminate sync progress indicator inside this activity layout.
     */
    private fun setSyncProgressVisibility(isVisible: Boolean) {
        val syncProgressBar = syncProgressBar
        if (syncProgressBar == null ||
            syncProgressBar.visibility == (if (isVisible) View.VISIBLE else View.GONE)) {
            // not enabled or already in desired state, avoid replaying animation
            return
        }
        syncProgressBar.startAnimation(
            AnimationUtils.loadAnimation(
                syncProgressBar.context,
                if (isVisible) R.anim.fade_in else R.anim.fade_out
            )
        )
        syncProgressBar.visibility =
            if (isVisible) View.VISIBLE else View.GONE
    }

    /**
     * Create a new anonymous SyncStatusObserver. It's attached to the app's ContentResolver in
     * onResume(), and removed in onPause(). If a sync is active or pending, a progress bar is
     * shown.
     */
    private val syncStatusObserver = object : SyncStatusObserver {
        /** Callback invoked with the sync adapter status changes.  */
        override fun onStatusChanged(which: Int) {
            runOnUiThread(Runnable {
                /**
                 * The SyncAdapter runs on a background thread. To update the
                 * UI, onStatusChanged() runs on the UI thread.
                 */
                val account = AccountUtils.getAccount(this@BaseTopActivity)
                if (account == null) {
                    // no account setup
                    setSyncProgressVisibility(false)
                    return@Runnable
                }

                // Test the ContentResolver to see if the sync adapter is active.
                val syncActive = ContentResolver.isSyncActive(
                    account, SgApp.CONTENT_AUTHORITY
                )
                setSyncProgressVisibility(syncActive)
            })
        }
    }

    companion object {
        private const val OPTIONS_SWITCH_THEME_ID = 0
    }
}