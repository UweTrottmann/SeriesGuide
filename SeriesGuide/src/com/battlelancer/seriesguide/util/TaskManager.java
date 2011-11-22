
package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.beta.R;

import android.content.Context;
import android.widget.Toast;

/**
 * Inspired by florianmski's traktoid TraktManager. This class is used to hold a
 * running update task, so it can execute independently from a running activity
 * (so the application can still be used while the update continues). A plain
 * AsyncTask could do this, too, but here we can also restrict it to one task
 * running at a time.
 */
public class TaskManager {

    private static TaskManager _instance;

    private UpdateTask mTask;

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

    public synchronized void tryUpdateTask(UpdateTask task, int messageId) {
        if (!isUpdateTaskRunning()) {
            mTask = task;
            Toast.makeText(mContext, messageId, Toast.LENGTH_SHORT).show();
            task.execute();
        }
    }

    public synchronized void onTaskCompleted() {
        mTask = null;
    }

    public boolean isUpdateTaskRunning() {
        if (mTask != null) {
            Toast.makeText(mContext, R.string.update_inprogress, Toast.LENGTH_LONG).show();
            return true;
        } else {
            return false;
        }
    }

}
