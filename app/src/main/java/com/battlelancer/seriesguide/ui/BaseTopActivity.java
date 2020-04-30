
package com.battlelancer.seriesguide.ui;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.annotation.IdRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.backend.CloudSetupActivity;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.dataliberation.BackupSettings;
import com.battlelancer.seriesguide.dataliberation.DataLiberationActivity;
import com.battlelancer.seriesguide.sync.AccountUtils;
import com.battlelancer.seriesguide.ui.preferences.MoreOptionsActivity;
import com.battlelancer.seriesguide.ui.stats.StatsActivity;
import com.battlelancer.seriesguide.util.Utils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import timber.log.Timber;

/**
 * Activities at the top of the navigation hierarchy, displaying a bottom navigation bar.
 * Implementers must set it up with {@link #setupBottomNavigation(int)}.
 * <p>
 * They should also override {@link #getSnackbarParentView()} and supply a CoordinatorLayout.
 * It is used to show snack bars for important warnings (e.g. auto backup failed, Cloud signed out).
 * <p>
 * Also provides support for an optional sync progress bar (see {@link
 * #setupSyncProgressBar(int)}).
 */
public abstract class BaseTopActivity extends BaseMessageActivity {

    private View syncProgressBar;
    private Object syncObserverHandle;
    private Snackbar snackbar;

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(false);
        }
    }

    public void setupBottomNavigation(@IdRes int selectedItemId) {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(selectedItemId);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            onNavItemClick(item.getItemId());
            return false; // Do not change selected item.
        });
    }

    private void onNavItemClick(int itemId) {
        Intent launchIntent = null;

        switch (itemId) {
            case R.id.navigation_item_shows:
                if (this instanceof ShowsActivity) {
                    onSelectedCurrentNavItem();
                    return;
                }
                launchIntent = new Intent(this, ShowsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                break;
            case R.id.navigation_item_lists:
                if (this instanceof ListsActivity) {
                    onSelectedCurrentNavItem();
                    return;
                }
                launchIntent = new Intent(this, ListsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                break;
            case R.id.navigation_item_movies:
                if (this instanceof MoviesActivity) {
                    onSelectedCurrentNavItem();
                    return;
                }
                launchIntent = new Intent(this, MoviesActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                break;
            case R.id.navigation_item_stats:
                if (this instanceof StatsActivity) {
                    onSelectedCurrentNavItem();
                    return;
                }
                launchIntent = new Intent(this, StatsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                break;
            case R.id.navigation_item_more:
                if (this instanceof MoreOptionsActivity) {
                    onSelectedCurrentNavItem();
                    return;
                }
                launchIntent = new Intent(this, MoreOptionsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                break;
        }

        if (launchIntent != null) {
            startActivity(launchIntent);
            overridePendingTransition(R.anim.activity_fade_enter_sg, R.anim.activity_fade_exit_sg);
        }
    }

    /**
     * Called if the currently active nav item was clicked.
     * Implementing activities might want to use this to scroll contents to the top.
     */
    protected void onSelectedCurrentNavItem() {
        // Do nothing by default.
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
    protected void onStart() {
        super.onStart();

        if (Utils.hasAccessToX(this) && HexagonSettings.shouldValidateAccount(this)) {
            onShowCloudAccountWarning();
        }
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
    protected void onStop() {
        super.onStop();
        // dismiss any snackbar to avoid it getting restored
        // if condition that led to its display is no longer true
        if (snackbar != null && snackbar.isShown()) {
            snackbar.dismiss();
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

    private static final int OPTIONS_SWITCH_THEME_ID = 0;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (BuildConfig.DEBUG) {
            menu.add(0, 0, 0, "[Debug] Switch theme");
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == OPTIONS_SWITCH_THEME_ID) {
            boolean isNightMode =
                    AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
            AppCompatDelegate.setDefaultNightMode(isNightMode
                    ? AppCompatDelegate.MODE_NIGHT_NO
                    : AppCompatDelegate.MODE_NIGHT_YES);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onLastAutoBackupFailed() {
        if (snackbar != null && snackbar.isShown()) {
            Timber.d("NOT showing auto backup failed message: existing snackbar.");
            return;
        }

        Snackbar newSnackbar = Snackbar.make(
                getSnackbarParentView(),
                R.string.autobackup_failed,
                Snackbar.LENGTH_INDEFINITE
        );

        // Manually increase max lines.
        TextView textView = newSnackbar.getView()
                .findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setMaxLines(5);

        newSnackbar
                .addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        if (event == Snackbar.Callback.DISMISS_EVENT_ACTION
                                || event == Snackbar.Callback.DISMISS_EVENT_SWIPE) {
                            Timber.i("Has seen last auto backup failed.");
                            BackupSettings.setHasSeenLastAutoBackupFailed(BaseTopActivity.this);
                        }
                    }
                })
                .setAction(R.string.preferences, v ->
                        startActivity(DataLiberationActivity.intentToShowAutoBackup(this)))
                .show();

        snackbar = newSnackbar;
    }

    @Override
    protected void onAutoBackupMissingFiles() {
        if (snackbar != null && snackbar.isShown()) {
            Timber.d("NOT showing backup files warning: existing snackbar.");
            return;
        }

        Snackbar newSnackbar = Snackbar.make(
                getSnackbarParentView(),
                R.string.autobackup_files_missing,
                Snackbar.LENGTH_INDEFINITE
        );

        // Manually increase max lines.
        TextView textView = newSnackbar.getView()
                .findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setMaxLines(5);

        newSnackbar.setAction(R.string.preferences, v ->
                startActivity(DataLiberationActivity.intentToShowAutoBackup(this)));
        newSnackbar.show();

        snackbar = newSnackbar;
    }

    protected void onShowCloudAccountWarning() {
        if (snackbar != null && snackbar.isShown()) {
            Timber.d("NOT showing Cloud account warning: existing snackbar.");
            return;
        }

        Snackbar newSnackbar = Snackbar
                .make(getSnackbarParentView(), R.string.hexagon_signed_out,
                        Snackbar.LENGTH_INDEFINITE);
        newSnackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                if (event == Snackbar.Callback.DISMISS_EVENT_SWIPE) {
                    // user has dismissed warning, so disable Cloud
                    HexagonTools hexagonTools = SgApp.getServicesComponent(BaseTopActivity.this)
                            .hexagonTools();
                    hexagonTools.setDisabled();
                }
            }
        }).setAction(R.string.hexagon_signin, v -> {
            // forward to cloud setup which can help fix the account issue
            startActivity(new Intent(BaseTopActivity.this, CloudSetupActivity.class));
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
