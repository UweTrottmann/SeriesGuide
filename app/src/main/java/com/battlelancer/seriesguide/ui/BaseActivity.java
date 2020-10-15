package com.battlelancer.seriesguide.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.core.app.TaskStackBuilder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.dataliberation.BackupSettings;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.traktapi.TraktTask;
import com.battlelancer.seriesguide.ui.search.AddShowTask;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.TaskManager;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Provides some common functionality across all activities like setting the theme, navigation
 * shortcuts and triggering AutoUpdates and AutoBackups. <p> Also registers with {@link
 * EventBus#getDefault()} by default to handle various common events, see {@link
 * #registerEventBus()} and {@link #unregisterEventBus()} to prevent that.
 */
public abstract class BaseActivity extends AppCompatActivity {

    private Handler handler;
    private Runnable updateShowRunnable;

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
        Toolbar toolbar = findViewById(R.id.sgToolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // make sync interfering with backup task less likely
        if (!onAutoBackup()) {
            SgSyncAdapter.requestSyncIfTime(this);
        }
        registerEventBus();
    }

    /**
     * Override this to avoid registering with {@link EventBus#getDefault()} in {@link #onStart()}.
     *
     * <p> See {@link #unregisterEventBus()} as well.
     */
    public void registerEventBus() {
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (handler != null && updateShowRunnable != null) {
            handler.removeCallbacks(updateShowRunnable);
        }

        unregisterEventBus();
    }

    /**
     * Override this to avoid unregistering from {@link EventBus#getDefault()} in {@link
     * #onStop()}.
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
        if (!BackupSettings.isAutoBackupEnabled(this)) {
            return false;
        }

        // If last auto backup failed, show a warning, but run it (if it's time) anyhow.
        if (BackupSettings.isWarnLastAutoBackupFailed(this)) {
            onLastAutoBackupFailed();
        }
        // If copies should be made, but the specified files are invalid,
        // show a warning but run auto backup (if it's time) anyhow.
        else if (BackupSettings.isCreateCopyOfAutoBackup(this)
                && BackupSettings.isMissingAutoBackupFile(this)) {
            onAutoBackupMissingFiles();
        }

        if (!BackupSettings.isTimeForAutoBackup(this)) {
            return false;
        }

        TaskManager.getInstance().tryBackupTask(this);
        return true;
    }

    /**
     * Implementers may choose to show a warning that the last auto backup has failed.
     */
    protected void onLastAutoBackupFailed() {
        // Do nothing.
    }

    /**
     * Implementers may choose to show a warning that auto backup can not complete because not all
     * custom backup files are configured.
     */
    protected void onAutoBackupMissingFiles() {
        // Do nothing.
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
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }

        // delay sync request to avoid slowing down UI
        final Context context = getApplicationContext();
        updateShowRunnable = () -> SgSyncAdapter.requestSyncIfTime(context, showTvdbId);
        handler.postDelayed(updateShowRunnable, DateUtils.SECOND_IN_MILLIS);
    }
}
