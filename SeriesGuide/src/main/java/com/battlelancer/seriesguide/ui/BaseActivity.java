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

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.MenuItem;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.util.AddShowTask;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.TraktTask;
import com.google.android.gms.analytics.GoogleAnalytics;
import de.greenrobot.event.EventBus;

/**
 * Provides some common functionality across all activities like setting the theme, navigation
 * shortcuts and triggering AutoUpdates and AutoBackups. <p> Also registers with {@link
 * de.greenrobot.event.EventBus#getDefault()} by default to handle various common events, see {@link
 * #registerEventBus()} and {@link #unregisterEventBus()} to prevent that.
 */
public abstract class BaseActivity extends AppCompatActivity {

    private Handler mHandler;
    private Runnable mUpdateShowRunnable;

    @Override
    protected void onCreate(Bundle arg0) {
        setCustomTheme();
        super.onCreate(arg0);
    }

    protected void setCustomTheme() {
        // set a theme based on user preference
        setTheme(SeriesGuidePreferences.THEME);
    }

    /**
     * Implementers must call this in {@link #onCreate} after {@link #setContentView} if they want
     * to use the action bar.
     */
    protected void setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.sgToolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // make sync interfering with backup task less likely
        if (!onAutoBackup()) {
            SgSyncAdapter.requestSyncIfTime(this);
        }
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
        registerEventBus();
    }

    /**
     * Override this to avoid registering with {@link de.greenrobot.event.EventBus#getDefault()} in
     * {@link #onStart()}.
     *
     * <p> See {@link #unregisterEventBus()} as well.
     */
    public void registerEventBus() {
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mHandler != null && mUpdateShowRunnable != null) {
            mHandler.removeCallbacks(mUpdateShowRunnable);
        }

        GoogleAnalytics.getInstance(this).reportActivityStop(this);
        unregisterEventBus();
    }

    /**
     * Override this to avoid unregistering from {@link de.greenrobot.event.EventBus#getDefault()}
     * in {@link #onStop()}.
     *
     * <p> See {@link #registerEventBus()} as well.
     */
    public void unregisterEventBus() {
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onEvent(AddShowTask.OnShowAddedEvent event) {
        // display status toast about adding shows
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

    /**
     * Schedule an update for the given show. Might not run if this show was just updated. Execution
     * is also delayed so it won't reduce UI setup performance (= you can run this in {@link
     * #onCreate(android.os.Bundle)}).
     *
     * <p> See {@link com.battlelancer.seriesguide.sync.SgSyncAdapter#requestSyncIfTime(android.content.Context,
     * int)}.
     */
    protected void updateShowDelayed(final int showTvdbId) {
        if (mHandler == null) {
            mHandler = new Handler();
        }

        // delay sync request to avoid slowing down UI
        final Context context = getApplicationContext();
        mUpdateShowRunnable = new Runnable() {
            @Override
            public void run() {
                SgSyncAdapter.requestSyncIfTime(context, showTvdbId);
            }
        };
        mHandler.postDelayed(mUpdateShowRunnable, DateUtils.SECOND_IN_MILLIS);
    }
}
