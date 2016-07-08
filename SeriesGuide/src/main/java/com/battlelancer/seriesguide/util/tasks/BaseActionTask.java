package com.battlelancer.seriesguide.util.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.uwetrottmann.androidutils.AndroidUtils;

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

        // show toast to which service we send
        if (isSendingToHexagon()) {
            Toast.makeText(context, R.string.hexagon_api_queued, Toast.LENGTH_SHORT).show();
        }
        if (isSendingToTrakt()) {
            Toast.makeText(context, R.string.trakt_submitqueued, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (result == SUCCESS && getSuccessTextResId() != 0) {
            // success!
            Toast.makeText(context, getSuccessTextResId(), Toast.LENGTH_SHORT).show();
            return;
        }

        // handle errors
        Integer errorResId = null;
        switch (result) {
            case ERROR_NETWORK:
                errorResId = R.string.offline;
                break;
            case ERROR_DATABASE:
                errorResId = R.string.database_error;
                break;
            case ERROR_TRAKT_AUTH:
                errorResId = R.string.trakt_error_credentials;
                break;
            case ERROR_TRAKT_API:
                errorResId = R.string.trakt_error_general;
                break;
            case ERROR_TRAKT_API_NOT_FOUND:
                errorResId = R.string.trakt_error_not_exists;
                break;
            case ERROR_HEXAGON_API:
                errorResId = R.string.hexagon_api_error;
                break;
        }
        if (errorResId != null) {
            Toast.makeText(context, errorResId, Toast.LENGTH_LONG).show();
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
