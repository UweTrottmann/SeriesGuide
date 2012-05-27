
package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.items.SearchResult;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

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
        // notify user here already
        Toast.makeText(mContext,
                "\"" + show.title + "\" " + mContext.getString(R.string.add_started),
                Toast.LENGTH_SHORT).show();

        // add the show to a running add task or create a new one
        if (mAddTask == null || mAddTask.getStatus() == AsyncTask.Status.FINISHED) {
            mAddTask = (AddShowTask) new AddShowTask(mContext, show).execute();
        } else {
            // addTask is still running, try to add another show to its queue
            boolean hasAddedShow = mAddTask.addShow(show);
            if (!hasAddedShow) {
                mAddTask = (AddShowTask) new AddShowTask(mContext, show).execute();
            }
        }
    }

    public synchronized void tryUpdateTask(UpdateTask task, int messageId) {
        if (!isUpdateTaskRunning(true)) {
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

}
