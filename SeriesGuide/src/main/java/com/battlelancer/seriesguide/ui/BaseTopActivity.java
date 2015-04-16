
/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.ui;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.SyncStatusObserver;
import android.support.annotation.IdRes;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SeriesGuideApplication;
import com.battlelancer.seriesguide.sync.AccountUtils;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

/**
 * Activities at the top of the navigation hierarchy, display the nav drawer upon pressing the
 * up/home action bar button.
 *
 * <p>Also provides support for an optional sync progress bar (see {@link
 * #setupSyncProgressBar(int)}).
 */
public abstract class BaseTopActivity extends BaseNavDrawerActivity {

    private SmoothProgressBar syncProgressBar;
    private Object syncObserverHandle;

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
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
        syncProgressBar = (SmoothProgressBar) findViewById(progressBarId);
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
        if (toggleDrawer(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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
                            account, SeriesGuideApplication.CONTENT_AUTHORITY);
                    setSyncProgressVisibility(syncActive);
                }
            });
        }
    };

    /**
     * Google Analytics helper method for easy sending of click events.
     */
    protected abstract void fireTrackerEvent(String label);
}
