// SPDX-License-Identifier: Apache-2.0
// Copyright 2012-2025 Uwe Trottmann

package com.battlelancer.seriesguide.ui

import android.content.ContentResolver
import android.content.Intent
import android.content.SyncStatusObserver
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.activity.addCallback
import androidx.annotation.IdRes
import androidx.core.view.ViewCompat
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.SgApp.Companion.getServicesComponent
import com.battlelancer.seriesguide.backend.CloudSetupActivity
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.billing.BillingTools
import com.battlelancer.seriesguide.dataliberation.BackupSettings
import com.battlelancer.seriesguide.dataliberation.DataLiberationActivity
import com.battlelancer.seriesguide.preferences.MoreOptionsActivity
import com.battlelancer.seriesguide.stats.StatsActivity
import com.battlelancer.seriesguide.sync.AccountUtils
import com.battlelancer.seriesguide.ui.ShowsActivity
import com.battlelancer.seriesguide.util.SupportTheDev
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

/**
 * Activities at the top of the navigation hierarchy, displaying a bottom navigation bar.
 * Implementers must set it up with [setupBottomNavigation].
 *
 * They should also override [snackbarParentView] and supply a CoordinatorLayout.
 * It is used to show snack bars for important warnings (e.g. auto backup failed, Cloud signed out).
 * They also should use [makeSnackbar] to create one to ensure it is properly shown above the
 * bottom navigation bar.
 *
 * Also provides support for an optional sync progress bar (see [setupSyncProgressBar]).
 */
abstract class BaseTopActivity : BaseMessageActivity() {

    private var syncProgressBar: View? = null
    private var syncObserverHandle: Any? = null
    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                R.anim.activity_fade_enter_sg,
                R.anim.activity_fade_exit_sg
            )
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                R.anim.activity_fade_enter_sg,
                R.anim.activity_fade_exit_sg
            )
        } else {
            onBackPressedDispatcher.addCallback {
                finish()
                // Use a custom animation when navigating away from a top activity
                // but not when exiting the app (use the default system animations).
                if (!isTaskRoot) {
                    @Suppress("DEPRECATION")
                    overridePendingTransition(
                        R.anim.activity_fade_enter_sg,
                        R.anim.activity_fade_exit_sg
                    )
                }
            }
        }
    }

    override fun setupActionBar() {
        super.setupActionBar()
        supportActionBar?.setHomeButtonEnabled(false)
    }

    fun setupBottomNavigation(@IdRes selectedItemId: Int) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = selectedItemId
        // Disable hideous bold font for active item.
        bottomNav.setItemTextAppearanceActiveBoldEnabled(false)
        bottomNav.setOnItemSelectedListener { item ->
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
            // For UPSIDE_DOWN_CAKE+ using overrideActivityTransition() in BaseTopActivity
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                overridePendingTransition(
                    R.anim.activity_fade_enter_sg,
                    R.anim.activity_fade_exit_sg
                )
            }
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
        if (BillingTools.hasAccessToPaidFeatures(this) && HexagonSettings.shouldValidateAccount(this)) {
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

    /**
     * Note: when using the [snackbarParentView] of this activity the Snackbar is not aligned to the
     * bottom of the screen (due to the bottom navigation bar). So using
     * [doNotInsetForNavigationBarOrIme] on it.
     */
    override fun makeSnackbar(message: String, length: Int): Snackbar {
        return super.makeSnackbar(message, length)
            .doNotInsetForNavigationBarOrIme()
    }

    /**
     * Prevent the Snackbar from adding bottom margin for the navigation bar (or an input method)
     * when it is not aligned to the bottom of the screen.
     *
     * https://github.com/material-components/material-components-android/issues/3446
     */
    private fun Snackbar.doNotInsetForNavigationBarOrIme(): Snackbar {
        // Note: do not consume insets as the bottom navigation bar needs them when using
        // button navigation.
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets -> insets }
        return this
    }

    override fun onLastAutoBackupFailed() {
        val snackbar = snackbar
        if (snackbar != null && snackbar.isShown) {
            Timber.d("NOT showing auto backup failed message: existing snackbar.")
            return
        }
        val newSnackbar = makeSnackbar(R.string.autobackup_failed, Snackbar.LENGTH_INDEFINITE)

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
        val newSnackbar = makeSnackbar(
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
        val newSnackbar = makeSnackbar(
            R.string.hexagon_signed_out,
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
        val newSnackbar = makeSnackbar(
            R.string.support_the_dev,
            SupportTheDev.SUPPORT_MESSAGE_DURATION_MILLISECONDS
        ).addCallback(object : Snackbar.Callback() {
            override fun onDismissed(snackbar: Snackbar, event: Int) {
                // Always do not show again after user has seen it once
                SupportTheDev.saveDismissedRightNow(snackbar.context)
            }
        }).setAction(R.string.billing_action_subscribe) {
            startActivity(BillingTools.getBillingActivityIntent(this))
        }
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
}