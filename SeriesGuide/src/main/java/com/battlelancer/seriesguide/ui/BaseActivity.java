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

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.getglueapi.GetGlueCheckin;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.R;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.format.DateUtils;
import android.view.KeyEvent;

/**
 * Provides some common functionality across all activities like setting the theme, navigation
 * shortcuts and triggering AutoUpdates and AutoBackups.
 */
public abstract class BaseActivity extends SherlockFragmentActivity {

    @Override
    protected void onCreate(Bundle arg0) {
        setCustomTheme();
        super.onCreate(arg0);
    }

    protected void setCustomTheme() {
        // set a theme based on user preference
        setTheme(SeriesGuidePreferences.THEME);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // make sync interfering with backup task less likely
        if (!onAutoBackup()) {
            SgSyncAdapter.requestSyncIfTime(this);
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

    public void onEvent(GetGlueCheckin.GetGlueCheckInTask.GetGlueCheckInCompleteEvent event) {
        // display status toast about GetGlue check-in
        event.handle(this);
    }

    public void onEvent(TraktTask.TraktActionCompleteEvent event) {
        // display status toast about trakt action
        event.handle(this);
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

}
