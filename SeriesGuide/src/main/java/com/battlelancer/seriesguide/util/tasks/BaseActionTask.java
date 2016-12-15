package com.battlelancer.seriesguide.util.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.BaseNavDrawerActivity;
import com.uwetrottmann.androidutils.AndroidUtils;
import org.greenrobot.eventbus.EventBus;

public abstract class BaseActionTask extends AsyncTask<Void, Void, Integer> {

    public static final int SUCCESS = 0;
    public static final int ERROR_NETWORK = -1;
    public static final int ERROR_DATABASE = -2;
    public static final int ERROR_TRAKT_AUTH = -3;
    public static final int ERROR_TRAKT_API = -4;
    public static final int ERROR_TRAKT_API_NOT_FOUND = -5;
    public static final int ERROR_HEXAGON_API = -6;

    private final Context context;
    private boolean isSendingToHexagon;
    private boolean isSendingToTrakt;

    public BaseActionTask(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    protected void onPreExecute() {
        isSendingToHexagon = HexagonTools.isSignedIn(context);
        isSendingToTrakt = TraktCredentials.get(context).hasCredentials();

        // if sending to services and there is no network, cancel right away
        if (isSendingToHexagon() || isSendingToTrakt()) {
            if (!AndroidUtils.isNetworkConnected(getContext())) {
                cancel(true);
                Toast.makeText(context, R.string.offline, Toast.LENGTH_LONG).show();
                return;
            }
        }

        // show message to which service we send
        EventBus.getDefault().postSticky(new BaseNavDrawerActivity.ServiceActiveEvent(
                isSendingToHexagon(), isSendingToTrakt()));
    }

    @Override
    protected void onPostExecute(Integer result) {
        EventBus.getDefault().removeStickyEvent(BaseNavDrawerActivity.ServiceActiveEvent.class);
        EventBus.getDefault().post(new BaseNavDrawerActivity.ServiceCompletedEvent());

        if (result == SUCCESS && getSuccessTextResId() != 0) {
            // success!
            Toast.makeText(context, getSuccessTextResId(), Toast.LENGTH_SHORT).show();
            return;
        }

        // handle errors
        String error = null;
        switch (result) {
            case ERROR_NETWORK:
                error = context.getString(R.string.offline);
                break;
            case ERROR_DATABASE:
                error = context.getString(R.string.database_error);
                break;
            case ERROR_TRAKT_AUTH:
                error = context.getString(R.string.trakt_error_credentials);
                break;
            case ERROR_TRAKT_API:
                error = context.getString(R.string.api_error_generic,
                        context.getString(R.string.trakt));
                break;
            case ERROR_TRAKT_API_NOT_FOUND:
                error = context.getString(R.string.trakt_error_not_exists);
                break;
            case ERROR_HEXAGON_API:
                error = context.getString(R.string.api_error_generic,
                        context.getString(R.string.hexagon));
                break;
        }
        if (error != null) {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show();
        }
    }

    protected Context getContext() {
        return context;
    }

    protected abstract int getSuccessTextResId();

    /**
     * Will be true if signed in with hexagon. Override and return {@code false} to not send to
     * hexagon.
     */
    protected boolean isSendingToHexagon() {
        return isSendingToHexagon;
    }

    /**
     * Will be true if signed in with trakt.
     */
    protected boolean isSendingToTrakt() {
        return isSendingToTrakt;
    }
}
