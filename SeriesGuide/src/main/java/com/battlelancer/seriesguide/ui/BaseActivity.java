package com.battlelancer.seriesguide.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.MenuItem;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.settings.BackupSettings;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.util.AddShowTask;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.TraktTask;
import com.google.android.gms.analytics.GoogleAnalytics;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

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
     * to use the action bar. <p>If setting a title, might also want to supply a title to the
     * activity ({@link #setTitle(CharSequence)}) for better accessibility.
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
     * Override this to avoid unregistering from {@link EventBus#getDefault()}
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
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    // This activity is NOT part of this app's task, so create a new task
                    // when navigating up, with a synthesized back stack.
                    TaskStackBuilder.create(this)
                            // Add all of this activity's parents to the back stack
                            .addNextIntentWithParentStack(upIntent)
                            // Navigate up to the closest parent
                            .startActivities();
                } else {
                    // This activity is part of this app's task, so simply
                    // navigate up to the logical parent activity.
                    NavUtils.navigateUpTo(this, upIntent);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Subscribe
    public void onEvent(AddShowTask.OnShowAddedEvent event) {
        // display status toast about adding shows
        event.handle(this);
    }

    @Subscribe
    public void onEvent(TraktTask.TraktActionCompleteEvent event) {
        // display status toast about trakt action
        event.handle(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DBUtils.DatabaseErrorEvent event) {
        event.handle(this);
    }

    /**
     * Periodically do an automatic backup of the show database.
     */
    private boolean onAutoBackup() {
        if (!AdvancedSettings.isAutoBackupEnabled(this)) {
            return false;
        }

        // only continue if we are allowed to write to external storage
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // only show warning if the user is done with first run
            if (FirstRunFragment.hasSeenFirstRunFragment(this)) {
                onShowAutoBackupPermissionWarning();
            }
            return false;
        }

        long now = System.currentTimeMillis();
        long previousBackupTime = AdvancedSettings.getLastAutoBackupTime(this);
        final boolean isTime = (now - previousBackupTime) > 7 * DateUtils.DAY_IN_MILLIS;

        if (isTime) {
            // if custom files are enabled, make sure they are configured
            // note: backup task clears backup file setting if there was an issue with the file
            if (!BackupSettings.isUseAutoBackupDefaultFiles(this)
                    && BackupSettings.isMissingAutoBackupFile(this)) {
                onShowAutoBackupMissingFilesWarning();
                return false;
            }

            TaskManager.getInstance(this).tryBackupTask();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Implementers may choose to show a warning that auto backup can not complete because not all
     * custom backup files are configured.
     */
    protected void onShowAutoBackupMissingFilesWarning() {
        // do nothing
    }

    /**
     * Implementers may choose to show a warning that auto backup can not complete because of
     * missing permissions.
     */
    protected void onShowAutoBackupPermissionWarning() {
        // do nothing
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
