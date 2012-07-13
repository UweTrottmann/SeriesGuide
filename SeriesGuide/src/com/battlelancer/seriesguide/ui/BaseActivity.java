/*
 * Copyright 2012 Uwe Trottmann
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

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.UpdateTask;
import com.uwetrottmann.androidutils.AndroidUtils;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.text.format.DateUtils;
import android.view.KeyEvent;

/**
 * Provides some common functionality across all activities like setting the
 * theme and navigation shortcuts.
 */
public abstract class BaseActivity extends SherlockFragmentActivity {

    @Override
    protected void onCreate(Bundle arg0) {
        // set a theme based on user preference
        setTheme(SeriesGuidePreferences.THEME);
        super.onCreate(arg0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        onAutoUpdate();
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        // always navigate back to the home activity
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            NavUtils.navigateUpTo(this,
                    new Intent(Intent.ACTION_MAIN).setClass(this, ShowsActivity.class));
            overridePendingTransition(R.anim.fragment_slide_right_enter,
                    R.anim.fragment_slide_right_exit);
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                overridePendingTransition(R.anim.fragment_slide_right_enter,
                        R.anim.fragment_slide_right_exit);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Try to launch a delta-update task if certain conditions are met.
     */
    private void onAutoUpdate() {
        // try to run auto-update
        if (AndroidUtils.isNetworkConnected(this)) {
            final SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext());

            // check if auto-update is actually enabled
            final boolean isAutoUpdateEnabled = prefs.getBoolean(
                    SeriesGuidePreferences.KEY_AUTOUPDATE, true);
            if (isAutoUpdateEnabled) {

                // check if wifi is required, abort if necessary
                final boolean isWifiOnly = prefs.getBoolean(
                        SeriesGuidePreferences.KEY_AUTOUPDATEWLANONLY, true);
                if (!isWifiOnly || AndroidUtils.isWifiConnected(this)) {

                    // only update if at least 15mins have passed since last one
                    long now = System.currentTimeMillis();
                    final long previousUpdateTime = prefs.getLong(
                            SeriesGuidePreferences.KEY_LASTUPDATE, 0);
                    final boolean isTime = (now - previousUpdateTime) > 15 * DateUtils.MINUTE_IN_MILLIS;

                    if (isTime) {
                        TaskManager.getInstance(this).tryUpdateTask(new UpdateTask(false, this),
                                false, -1);
                    }
                }
            }
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

    /**
     * Navigate to the overview activity of the given show.
     * 
     * @param showId
     */
    protected void navigateToOverview(int showId) {
        final Intent intent = new Intent(this, OverviewActivity.class);
        intent.putExtra(OverviewFragment.InitBundle.SHOW_TVDBID, showId);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
