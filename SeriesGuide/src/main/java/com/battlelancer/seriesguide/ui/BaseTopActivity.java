
package com.battlelancer.seriesguide.ui;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.backend.CloudSetupActivity;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.dataliberation.DataLiberationActivity;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.sync.AccountUtils;

/**
 * Activities at the top of the navigation hierarchy, display the nav drawer upon pressing the
 * up/home action bar button.
 *
 * <p>Also provides support for an optional sync progress bar (see {@link
 * #setupSyncProgressBar(int)}).
 */
public abstract class BaseTopActivity extends BaseNavDrawerActivity {

    private View syncProgressBar;
    private Object syncObserverHandle;
    private Snackbar snackbar;

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void setupNavDrawer() {
        super.setupNavDrawer();

        // show a drawer indicator
        setDrawerIndicatorEnabled();
    }

    /**
     * Implementing classes may call this in {@link #onCreate(android.os.Bundle)} to setup a
     * progress bar which displays when syncing.
     */
    protected void setupSyncProgressBar(@IdRes int progressBarId) {
        syncProgressBar = findViewById(progressBarId);
        syncProgressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (syncProgressBar != null) {
            // watch for sync state changes
            syncStatusObserver.onStatusChanged(0);
            final int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING |
                    ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
            syncObserverHandle = ContentResolver.addStatusChangeListener(mask, syncStatusObserver);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // stop listening to sync state changes
        if (syncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(syncObserverHandle);
            syncObserverHandle = null;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        // use special animation when navigating away from a top activity
        // but not when exiting the app (use the default system animations)
        if (!isTaskRoot()) {
            overridePendingTransition(R.anim.activity_fade_enter_sg, R.anim.activity_fade_exit_sg);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // check if we should toggle the navigation drawer (app icon was touched)
        return toggleDrawer(item) || super.onOptionsItemSelected(item);
    }

    @Override
    protected void onShowAutoBackupMissingFilesWarning() {
        if (snackbar != null && snackbar.isShown()) {
            // do not replace an existing snackbar
            return;
        }

        Snackbar newSnackbar = Snackbar
                .make(findViewById(android.R.id.content),
                        R.string.autobackup_files_missing, Snackbar.LENGTH_LONG);
        setUpAutoBackupSnackbar(newSnackbar);
        newSnackbar.show();

        snackbar = newSnackbar;
    }

    @Override
    protected void onShowAutoBackupPermissionWarning() {
        if (snackbar != null && snackbar.isShown()) {
            // do not replace an existing snackbar
            return;
        }

        Snackbar newSnackbar = Snackbar
                .make(findViewById(android.R.id.content),
                        R.string.autobackup_permission_missing, Snackbar.LENGTH_INDEFINITE);
        setUpAutoBackupSnackbar(newSnackbar);
        newSnackbar.show();

        snackbar = newSnackbar;
    }

    private void setUpAutoBackupSnackbar(Snackbar snackbar) {
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                if (event == Snackbar.Callback.DISMISS_EVENT_SWIPE) {
                    // user has acknowledged warning
                    // disable auto backup so warning is not shown again
                    disableAutoBackup();
                }
            }
        }).setAction(R.string.preferences, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // disable setting here (not in onDismissed)
                // so settings screen is correctly showing as disabled
                disableAutoBackup();
                startActivity(
                        new Intent(BaseTopActivity.this, DataLiberationActivity.class).putExtra(
                                DataLiberationActivity.InitBundle.EXTRA_SHOW_AUTOBACKUP, true));
            }
        });
    }

    private void disableAutoBackup() {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putBoolean(AdvancedSettings.KEY_AUTOBACKUP, false)
                .apply();
    }

    @Override
    protected void onShowCloudPermissionWarning() {
        if (snackbar != null && snackbar.isShown()) {
            // do not replace an existing snackbar
            return;
        }

        Snackbar newSnackbar = Snackbar
                .make(findViewById(android.R.id.content), R.string.hexagon_permission_missing,
                        Snackbar.LENGTH_INDEFINITE);
        newSnackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                if (event == Snackbar.Callback.DISMISS_EVENT_ACTION
                        || event == Snackbar.Callback.DISMISS_EVENT_SWIPE) {
                    // user has acknowledged warning
                    // so remove stored account name so this warning is not displayed again
                    HexagonTools.storeAccountName(BaseTopActivity.this, null);
                }
            }
        }).setAction(R.string.preferences, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(BaseTopActivity.this, CloudSetupActivity.class));
            }
        }).show();

        snackbar = newSnackbar;
    }

    /**
     * Shows or hides the indeterminate sync progress indicator inside this activity layout.
     */
    private void setSyncProgressVisibility(boolean isVisible) {
        if (syncProgressBar == null ||
                syncProgressBar.getVisibility() == (isVisible ? View.VISIBLE : View.GONE)) {
            // not enabled or already in desired state, avoid replaying animation
            return;
        }
        syncProgressBar.startAnimation(AnimationUtils.loadAnimation(syncProgressBar.getContext(),
                isVisible ? R.anim.fade_in : R.anim.fade_out));
        syncProgressBar.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    /**
     * Create a new anonymous SyncStatusObserver. It's attached to the app's ContentResolver in
     * onResume(), and removed in onPause(). If a sync is active or pending, a progress bar is
     * shown.
     */
    private SyncStatusObserver syncStatusObserver = new SyncStatusObserver() {
        /** Callback invoked with the sync adapter status changes. */
        @Override
        public void onStatusChanged(int which) {
            runOnUiThread(new Runnable() {
                /**
                 * The SyncAdapter runs on a background thread. To update the
                 * UI, onStatusChanged() runs on the UI thread.
                 */
                @Override
                public void run() {
                    Account account = AccountUtils.getAccount(BaseTopActivity.this);
                    if (account == null) {
                        // no account setup
                        setSyncProgressVisibility(false);
                        return;
                    }

                    // Test the ContentResolver to see if the sync adapter is active.
                    boolean syncActive = ContentResolver.isSyncActive(
                            account, SgApp.CONTENT_AUTHORITY);
                    setSyncProgressVisibility(syncActive);
                }
            });
        }
    };
}
