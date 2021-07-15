package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.dataliberation.JsonExportTask;
import com.battlelancer.seriesguide.ui.search.AddShowTask;
import com.battlelancer.seriesguide.ui.search.SearchResult;
import java.util.ArrayList;
import java.util.List;
import kotlinx.coroutines.Job;

/**
 * Hold some {@link AsyncTask} instances while running to ensure only one is executing at a time.
 */
public class TaskManager {

    private static TaskManager _instance;

    @Nullable private AddShowTask addShowTask;
    @Nullable private Job backupTask;
    @Nullable private LatestEpisodeUpdateTask nextEpisodeUpdateTask;

    private TaskManager() {
    }

    public static synchronized TaskManager getInstance() {
        if (_instance == null) {
            _instance = new TaskManager();
        }
        return _instance;
    }

    @MainThread
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
    @MainThread
    public synchronized void performAddTask(final Context context, final List<SearchResult> shows,
            final boolean isSilentMode, final boolean isMergingShows) {
        if (!isSilentMode) {
            // notify user here already
            if (shows.size() == 1) {
                // say title of show
                SearchResult show = shows.get(0);
                Toast.makeText(context, context.getString(R.string.add_started, show.getTitle()),
                        Toast.LENGTH_SHORT).show();
            } else {
                // generic adding multiple message
                Toast.makeText(context, R.string.add_multiple, Toast.LENGTH_SHORT).show();
            }
        }

        // add the show(s) to a running add task or create a new one
        //noinspection ConstantConditions: null check in isAddTaskRunning
        if (!isAddTaskRunning() || !addShowTask.addShows(shows, isSilentMode, isMergingShows)) {
            addShowTask = new AddShowTask(context, shows, isSilentMode, isMergingShows);
            addShowTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public synchronized void releaseAddTaskRef() {
        addShowTask = null; // clear reference to avoid holding on to task context
    }

    public boolean isAddTaskRunning() {
        return !(addShowTask == null || addShowTask.getStatus() == AsyncTask.Status.FINISHED);
    }

    /**
     * If no {@link AddShowTask} or {@link JsonExportTask} created by this {@link
     * com.battlelancer.seriesguide.util.TaskManager} is running a
     * {@link JsonExportTask} is scheduled in silent mode.
     */
    @MainThread
    public synchronized boolean tryBackupTask(Context context) {
        if (!isAddTaskRunning()
                && (backupTask == null || backupTask.isCompleted())) {
            JsonExportTask exportTask = new JsonExportTask(context, null, false, true, null);
            backupTask = exportTask.launch();
            return true;
        }
        return false;
    }

    public synchronized void releaseBackupTaskRef() {
        backupTask = null; // clear reference to avoid holding on to task context
    }

    /**
     * Schedules a {@link com.battlelancer.seriesguide.util.LatestEpisodeUpdateTask} for all shows
     * if no other one of this type is currently running.
     */
    @MainThread
    public synchronized void tryNextEpisodeUpdateTask(Context context) {
        if (nextEpisodeUpdateTask == null
                || nextEpisodeUpdateTask.getStatus() == AsyncTask.Status.FINISHED) {
            nextEpisodeUpdateTask = new LatestEpisodeUpdateTask(context);
            nextEpisodeUpdateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public synchronized void releaseNextEpisodeUpdateTaskRef() {
        nextEpisodeUpdateTask = null; // clear reference to avoid holding on to task context
    }
}
