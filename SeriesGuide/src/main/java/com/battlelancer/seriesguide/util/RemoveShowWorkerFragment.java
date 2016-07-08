package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.NetworkResult;
import de.greenrobot.event.EventBus;

/**
 * Worker {@link android.support.v4.app.Fragment} hosting an {@link android.os.AsyncTask} that
 * removes a show from the database.
 */
public class RemoveShowWorkerFragment extends Fragment {

    public static final String TAG = "RemoveShowWorkerFragment";

    private static final String KEY_SHOW_TVDBID = "show_tvdb_id";

    public static RemoveShowWorkerFragment newInstance(int showTvdbId) {
        RemoveShowWorkerFragment f = new RemoveShowWorkerFragment();

        Bundle args = new Bundle();
        args.putInt(KEY_SHOW_TVDBID, showTvdbId);
        f.setArguments(args);

        return f;
    }

    /**
     * Posted if show is about to get removed.
     */
    public class OnRemovingShowEvent {
    }

    /**
     * Posted if show was just removed (or failure).
     */
    public class OnShowRemovedEvent {
        /** One of {@link com.battlelancer.seriesguide.enums.NetworkResult}. */
        public final int resultCode;

        public OnShowRemovedEvent(int resultCode) {
            this.resultCode = resultCode;
        }
    }

    private RemoveShowTask mTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // do not overwrite existing task
        if (mTask == null) {
            mTask = new RemoveShowTask(getActivity().getApplicationContext());
            Utils.executeInOrder(mTask, getArguments().getInt(KEY_SHOW_TVDBID));
        }
    }

    public boolean isTaskFinished() {
        return mTask == null || mTask.getStatus() == AsyncTask.Status.FINISHED;
    }

    private class RemoveShowTask extends AsyncTask<Integer, Void, Integer> {

        private final Context mContext;

        public RemoveShowTask(Context context) {
            mContext = context;
        }

        @Override
        protected void onPreExecute() {
            EventBus.getDefault().post(new OnRemovingShowEvent());
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            return ShowTools.get(mContext).removeShow(params[0]);
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result == NetworkResult.OFFLINE) {
                Toast.makeText(mContext, R.string.offline, Toast.LENGTH_LONG).show();
            } else if (result == NetworkResult.ERROR) {
                Toast.makeText(mContext, R.string.delete_error, Toast.LENGTH_LONG).show();
            }

            EventBus.getDefault().post(new OnShowRemovedEvent(result));
        }
    }
}
