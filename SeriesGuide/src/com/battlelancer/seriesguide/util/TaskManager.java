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

package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.battlelancer.seriesguide.items.SearchResult;
import com.uwetrottmann.seriesguide.R;

import java.util.List;

/**
 * Inspired by florianmski's traktoid TraktManager. This class is used to hold
 * running tasks, so it can execute independently from a running activity (so
 * the application can still be used while the update continues). A plain
 * AsyncTask could do this, too, but here we can also restrict it to one task
 * running at a time.
 */
public class TaskManager {

    private static TaskManager _instance;

    private AddShowTask mAddTask;

    private UpdateTask mUpdateTask;

    private BackupTask mBackupTask;

    private Context mContext;

    private TaskManager(Context context) {
        mContext = context.getApplicationContext();
    }

    public static synchronized TaskManager getInstance(Context context) {
        if (_instance == null) {
            _instance = new TaskManager(context);
        }
        return _instance;
    }

    public synchronized void performAddTask(SearchResult show) {
        List<SearchResult> wrapper = Lists.newArrayList();
        wrapper.add(show);
        performAddTask(wrapper);
    }

    public synchronized void performAddTask(List<SearchResult> shows) {
        // notify user here already
        if (shows.size() == 1) {
            // say title of show
            SearchResult show = shows.get(0);
            Toast.makeText(mContext,
                    "\"" + show.title + "\" " + mContext.getString(R.string.add_started),
                    Toast.LENGTH_SHORT).show();
        } else {
            // generic adding multiple message
            Toast.makeText(
                    mContext,
                    R.string.add_multiple,
                    Toast.LENGTH_SHORT).show();
        }

        // add the show(s) to a running add task or create a new one
        boolean isRequiringNewTask;
        if (mAddTask == null || mAddTask.getStatus() == AsyncTask.Status.FINISHED) {
            isRequiringNewTask = true;
        } else {
            // addTask is still running, try to add another show to its queue
            isRequiringNewTask = !mAddTask.addShows(shows);
        }
        if (isRequiringNewTask) {
            mAddTask = (AddShowTask) new AddShowTask(mContext, shows).execute();
        }
    }

    /**
     * Checks if an {@link UpdateTask} is already running. If yes, a warning can
     * be displayed by setting displayWarning. If not the given
     * {@link UpdateTask} is stored and executed. If messageId is not -1 this
     * string resource will be displayed as a toast after the {@link UpdateTask}
     * is started.
     * 
     * @param task
     * @param displayWarning
     * @param messageId
     */
    public synchronized void tryUpdateTask(UpdateTask task, boolean displayWarning, int messageId) {
        if (!isUpdateTaskRunning(displayWarning)) {
            mUpdateTask = task;
            task.execute();
            if (messageId != -1) {
                Toast.makeText(mContext, messageId, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public synchronized void onTaskCompleted() {
        mUpdateTask = null;
    }

    public boolean isUpdateTaskRunning(boolean displayWarning) {
        if (mUpdateTask != null) {
            if (displayWarning) {
                Toast.makeText(mContext, R.string.update_inprogress, Toast.LENGTH_LONG).show();
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean isAddTaskRunning() {
        if (mAddTask == null || mAddTask.getStatus() == AsyncTask.Status.FINISHED) {
            return false;
        } else {
            return true;
        }
    }

    public void tryBackupTask(String filePath) {
        if (!isUpdateTaskRunning(false) && !isAddTaskRunning()) {
            mBackupTask = new BackupTask(mContext);
            mBackupTask.execute(new String[] {
                    filePath
            });
        }
    }

    public boolean isBackupTaskRunning() {
        if (mBackupTask == null || mBackupTask.getStatus() == AsyncTask.Status.FINISHED) {
            return false;
        } else {
            return true;
        }
    }
}
