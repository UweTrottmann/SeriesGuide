package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.os.AsyncTaskCompat;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.dataliberation.JsonExportTask;
import com.battlelancer.seriesguide.items.SearchResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Hold some {@link AsyncTask} instances while running to ensure only one is executing at a time.
 */
public class TaskManager {

    private static TaskManager _instance;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    @Nullable private AddShowTask mAddTask;
    @Nullable private JsonExportTask mBackupTask;
    @Nullable private LatestEpisodeUpdateTask mNextEpisodeUpdateTask;

    private TaskManager() {
    }

    public static synchronized TaskManager getInstance() {
        if (_instance == null) {
            _instance = new TaskManager();
        }
        return _instance;
    }

    public synchronized void performAddTask(Context context, SearchResult show) {
        List<SearchResult> wrapper = new ArrayList<>();
        wrapper.add(show);
        performAddTask(context, wrapper, false, false);
    }

    /**
     * Schedule shows to be added to the database.
     *
     * @param isSilentMode   Whether to display status toasts if a show could not be added.
     * @param isMergingShows Whether to set the Hexagon show merged flag to true if all shows were
     */
    public synchronized void performAddTask(final Context context, final List<SearchResult> shows,
            final boolean isSilentMode, final boolean isMergingShows) {
        if (!isSilentMode) {
            // notify user here already
            if (shows.size() == 1) {
                // say title of show
                SearchResult show = shows.get(0);
                Toast.makeText(context, context.getString(R.string.add_started, show.title),
                        Toast.LENGTH_SHORT).show();
            } else {
                // generic adding multiple message
                Toast.makeText(context, R.string.add_multiple, Toast.LENGTH_SHORT).show();
            }
        }

        // add the show(s) to a running add task or create a new one
        //noinspection ConstantConditions: null check in isAddTaskRunning
        if (!isAddTaskRunning() || !mAddTask.addShows(shows, isSilentMode, isMergingShows)) {
            // ensure this is called on our main thread (AsyncTask needs access to it)
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mAddTask = (AddShowTask) Utils.executeInOrder(
                            new AddShowTask(context, shows, isSilentMode, isMergingShows));
                }
            });
        }
    }

    public synchronized void releaseAddTaskRef() {
        mAddTask = null; // clear reference to avoid holding on to task context
    }

    public boolean isAddTaskRunning() {
        return !(mAddTask == null || mAddTask.getStatus() == AsyncTask.Status.FINISHED);
    }

    /**
     * If no {@link AddShowTask} or {@link JsonExportTask} created by this {@link
     * com.battlelancer.seriesguide.util.TaskManager} is running a
     * {@link JsonExportTask} is scheduled in silent mode.
     */
    public synchronized void tryBackupTask(Context context) {
        if (!isAddTaskRunning()
                && (mBackupTask == null || mBackupTask.getStatus() == AsyncTask.Status.FINISHED)) {
            mBackupTask = new JsonExportTask(context, null, false, true);
            AsyncTaskCompat.executeParallel(mBackupTask);
        }
    }

    public synchronized void releaseBackupTaskRef() {
        mBackupTask = null; // clear reference to avoid holding on to task context
    }

    /**
     * Schedules a {@link com.battlelancer.seriesguide.util.LatestEpisodeUpdateTask} for all shows
     * if no other one of this type is currently running.
     */
    public synchronized void tryNextEpisodeUpdateTask(Context context) {
        if (mNextEpisodeUpdateTask == null
                || mNextEpisodeUpdateTask.getStatus() == AsyncTask.Status.FINISHED) {
            mNextEpisodeUpdateTask = new LatestEpisodeUpdateTask(context);
            AsyncTaskCompat.executeParallel(mNextEpisodeUpdateTask);
        }
    }

    public synchronized void releaseNextEpisodeUpdateTaskRef() {
        mNextEpisodeUpdateTask = null; // clear reference to avoid holding on to task context
    }
}
