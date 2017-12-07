package com.battlelancer.seriesguide.util.tasks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.enums.NetworkResult;
import org.greenrobot.eventbus.EventBus;

public class RemoveShowTask extends AsyncTask<Integer, Void, Integer> {

    public static void execute(Context context, int showTvdbId) {
        // database op, so execute on serial executor to minimize conflicts
        new RemoveShowTask(context, showTvdbId).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    @SuppressLint("StaticFieldLeak") // using application context
    private final Context context;
    private final int showTvdbId;

    private RemoveShowTask(Context context, int showTvdbId) {
        this.context = context.getApplicationContext();
        this.showTvdbId = showTvdbId;
    }

    @Override
    protected void onPreExecute() {
        EventBus.getDefault().post(new OnRemovingShowEvent(showTvdbId));
    }

    @Override
    protected Integer doInBackground(Integer... params) {
        return SgApp.getServicesComponent(context).showTools().removeShow(showTvdbId);
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (result == NetworkResult.OFFLINE) {
            Toast.makeText(context, R.string.offline, Toast.LENGTH_LONG).show();
        } else if (result == NetworkResult.ERROR) {
            Toast.makeText(context, R.string.delete_error, Toast.LENGTH_LONG).show();
        }

        EventBus.getDefault().post(new OnShowRemovedEvent(showTvdbId, result));
    }

    /**
     * Posted if a show is about to get removed.
     */
    public static class OnRemovingShowEvent {
        public final int showTvdbId;

        public OnRemovingShowEvent(int showTvdbId) {
            this.showTvdbId = showTvdbId;
        }
    }

    /**
     * Posted if show was just removed (or failure).
     */
    public static class OnShowRemovedEvent {
        public final int showTvdbId;
        /** One of {@link NetworkResult}. */
        public final int resultCode;

        public OnShowRemovedEvent(int showTvdbId, int resultCode) {
            this.showTvdbId = showTvdbId;
            this.resultCode = resultCode;
        }
    }
}
