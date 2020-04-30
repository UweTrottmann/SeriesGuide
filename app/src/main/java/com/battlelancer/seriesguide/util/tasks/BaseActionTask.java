package com.battlelancer.seriesguide.util.tasks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import androidx.annotation.CallSuper;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.traktapi.TraktCredentials;
import com.battlelancer.seriesguide.ui.BaseMessageActivity;
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

    @SuppressLint("StaticFieldLeak") // using application context
    private final Context context;
    private boolean isSendingToHexagon;
    private boolean isSendingToTrakt;

    public BaseActionTask(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    protected void onPreExecute() {
        isSendingToHexagon = HexagonSettings.isEnabled(context);
        isSendingToTrakt = TraktCredentials.get(context).hasCredentials();

        // show message to which service we send
        EventBus.getDefault().postSticky(new BaseMessageActivity.ServiceActiveEvent(
                isSendingToHexagon(), isSendingToTrakt()));
    }

    @Override
    protected final Integer doInBackground(Void... params) {
        if (isCancelled()) {
            return null;
        }

        // if sending to service, check for connection
        if (isSendingToHexagon() || isSendingToTrakt()) {
            if (!AndroidUtils.isNetworkConnected(getContext())) {
                return ERROR_NETWORK;
            }
        }

        return doBackgroundAction(params);
    }

    protected abstract Integer doBackgroundAction(Void... params);

    @CallSuper
    @Override
    protected void onPostExecute(Integer result) {
        EventBus.getDefault().removeStickyEvent(BaseMessageActivity.ServiceActiveEvent.class);

        boolean displaySuccess;
        String confirmationText;
        if (result == SUCCESS) {
            // success!
            displaySuccess = true;
            confirmationText = getSuccessTextResId() != 0
                    ? context.getString(getSuccessTextResId())
                    : null;
        } else {
            // handle errors
            displaySuccess = false;
            switch (result) {
                case ERROR_NETWORK:
                    confirmationText = context.getString(R.string.offline);
                    break;
                case ERROR_DATABASE:
                    confirmationText = context.getString(R.string.database_error);
                    break;
                case ERROR_TRAKT_AUTH:
                    confirmationText = context.getString(R.string.trakt_error_credentials);
                    break;
                case ERROR_TRAKT_API:
                    confirmationText = context.getString(R.string.api_error_generic,
                            context.getString(R.string.trakt));
                    break;
                case ERROR_TRAKT_API_NOT_FOUND:
                    confirmationText = context.getString(R.string.trakt_error_not_exists);
                    break;
                case ERROR_HEXAGON_API:
                    confirmationText = context.getString(R.string.api_error_generic,
                            context.getString(R.string.hexagon));
                    break;
                default:
                    confirmationText = null;
                    break;
            }
        }
        EventBus.getDefault()
                .post(new BaseMessageActivity.ServiceCompletedEvent(confirmationText,
                        displaySuccess, null));
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
