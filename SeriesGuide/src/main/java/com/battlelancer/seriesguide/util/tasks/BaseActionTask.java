package com.battlelancer.seriesguide.util.tasks;

import android.os.AsyncTask;
import android.support.annotation.CallSuper;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
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

    private final SgApp app;
    private boolean isSendingToHexagon;
    private boolean isSendingToTrakt;

    public BaseActionTask(SgApp app) {
        this.app = app;
    }

    @Override
    protected void onPreExecute() {
        isSendingToHexagon = HexagonSettings.isEnabled(app);
        isSendingToTrakt = TraktCredentials.get(app).hasCredentials();

        // show message to which service we send
        EventBus.getDefault().postSticky(new BaseNavDrawerActivity.ServiceActiveEvent(
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
        EventBus.getDefault().removeStickyEvent(BaseNavDrawerActivity.ServiceActiveEvent.class);

        boolean displaySuccess;
        String confirmationText;
        if (result == SUCCESS) {
            // success!
            displaySuccess = true;
            confirmationText = getSuccessTextResId() != 0
                    ? app.getString(getSuccessTextResId())
                    : null;
        } else {
            // handle errors
            displaySuccess = false;
            switch (result) {
                case ERROR_NETWORK:
                    confirmationText = app.getString(R.string.offline);
                    break;
                case ERROR_DATABASE:
                    confirmationText = app.getString(R.string.database_error);
                    break;
                case ERROR_TRAKT_AUTH:
                    confirmationText = app.getString(R.string.trakt_error_credentials);
                    break;
                case ERROR_TRAKT_API:
                    confirmationText = app.getString(R.string.api_error_generic,
                            app.getString(R.string.trakt));
                    break;
                case ERROR_TRAKT_API_NOT_FOUND:
                    confirmationText = app.getString(R.string.trakt_error_not_exists);
                    break;
                case ERROR_HEXAGON_API:
                    confirmationText = app.getString(R.string.api_error_generic,
                            app.getString(R.string.hexagon));
                    break;
                default:
                    confirmationText = null;
                    break;
            }
        }
        EventBus.getDefault()
                .post(new BaseNavDrawerActivity.ServiceCompletedEvent(confirmationText,
                        displaySuccess));
    }

    protected SgApp getContext() {
        return app;
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
