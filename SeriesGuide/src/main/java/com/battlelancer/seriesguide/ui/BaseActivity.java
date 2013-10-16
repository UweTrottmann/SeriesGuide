/*
 * Copyright 2013 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.text.format.DateUtils;
import android.view.KeyEvent;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.ui.dialogs.TraktCancelCheckinDialogFragment;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.TraktTask.InitBundle;
import com.battlelancer.seriesguide.util.TraktTask.OnTraktActionCompleteListener;
import com.uwetrottmann.seriesguide.R;

/**
 * Provides some common functionality across all activities like setting the
 * theme, navigation shortcuts and triggering AutoUpdates and AutoBackups.
 */
public abstract class BaseActivity extends SherlockFragmentActivity implements
        OnTraktActionCompleteListener {

    @Override
    protected void onCreate(Bundle arg0) {
        // set a theme based on user preference
        setTheme(SeriesGuidePreferences.THEME);
        super.onCreate(arg0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // make sync interfering with backup task less likely
        if (!onAutoBackup()) {
            SgSyncAdapter.requestSync(this);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        // always navigate back to the home activity
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            NavUtils.navigateUpTo(this,
                    new Intent(Intent.ACTION_MAIN).setClass(this, ShowsActivity.class));
            overridePendingTransition(R.anim.shrink_enter, R.anim.shrink_exit);
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                overridePendingTransition(R.anim.shrink_enter, R.anim.shrink_exit);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTraktActionComplete(Bundle traktTaskArgs, boolean wasSuccessfull) {
        dismissProgressDialog(traktTaskArgs);
    }

    @Override
    public void onCheckinBlocked(Bundle traktTaskArgs, int wait) {
        dismissProgressDialog(traktTaskArgs);
        // Guard against the system reclaiming our resources
        if (!isFinishing()) {
            TraktCancelCheckinDialogFragment newFragment = TraktCancelCheckinDialogFragment
                    .newInstance(traktTaskArgs, wait);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            newFragment.show(ft, "cancel-checkin-dialog");
        }
    }

    private void dismissProgressDialog(Bundle traktTaskArgs) {
        TraktAction action = TraktAction.values()[traktTaskArgs.getInt(InitBundle.TRAKTACTION)];
        // dismiss a potential progress dialog
        if (action == TraktAction.CHECKIN_EPISODE || action ==
                TraktAction.CHECKIN_MOVIE) {
            Fragment prev = getSupportFragmentManager().findFragmentByTag("progress-dialog");
            if (prev != null) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.remove(prev);
                ft.commit();
            }
        }
    }

    /**
     * Periodically do an automatic backup of the show database.
     */
    private boolean onAutoBackup() {
        if (!AdvancedSettings.isAutoBackupEnabled(this)) {
            return false;
        }

        long now = System.currentTimeMillis();
        long previousBackupTime = AdvancedSettings.getLastAutoBackupTime(this);
        final boolean isTime = (now - previousBackupTime) > 7 * DateUtils.DAY_IN_MILLIS;

        if (isTime) {
            TaskManager.getInstance(this).tryBackupTask();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Converts an intent into a {@link Bundle} suitable for use as fragment
     * arguments.
     */
    public static Bundle intentToFragmentArguments(Intent intent) {
        Bundle arguments = new Bundle();
        if (intent == null) {
            return arguments;
        }

        final Uri data = intent.getData();
        if (data != null) {
            arguments.putParcelable("_uri", data);
        }

        final Bundle extras = intent.getExtras();
        if (extras != null) {
            arguments.putAll(intent.getExtras());
        }

        return arguments;
    }

    /**
     * Converts a fragment arguments bundle into an intent.
     */
    public static Intent fragmentArgumentsToIntent(Bundle arguments) {
        Intent intent = new Intent();
        if (arguments == null) {
            return intent;
        }

        final Uri data = arguments.getParcelable("_uri");
        if (data != null) {
            intent.setData(data);
        }

        intent.putExtras(arguments);
        intent.removeExtra("_uri");
        return intent;
    }
}
