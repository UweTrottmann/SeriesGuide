package com.battlelancer.seriesguide.traktapi;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.ViewModelProvider;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.util.Errors;
import com.battlelancer.seriesguide.util.Utils;
import java.math.BigInteger;
import java.security.SecureRandom;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import timber.log.Timber;

/**
 * Starts a trakt OAuth 2.0 authorization flow using the default browser or an embedded {@link
 * android.webkit.WebView} as a fallback.
 */
public class TraktAuthActivity extends BaseOAuthActivity {

    private static final String KEY_STATE = "state";
    private static final String ACTION_FETCHING_TOKENS = "fetching tokens";
    private static final String ERROR_DESCRIPTION_STATE_MISMATCH
            = "invalid_state, State is null or does not match.";

    private String state;
    private TraktAuthActivityModel model;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        model = new ViewModelProvider(this).get(TraktAuthActivityModel.class);

        if (savedInstanceState != null) {
            // restore state on recreation
            // (e.g. if launching external browser dropped us out of memory)
            state = savedInstanceState.getString(KEY_STATE);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_STATE, state);
    }

    @Override
    @Nullable
    protected String getAuthorizationUrl() {
        state = new BigInteger(130, new SecureRandom()).toString(32);
        return SgApp.getServicesComponent(this).trakt().buildAuthorizationUrl(state);
    }

    @Override
    protected String getAuthErrorMessage() {
        return getString(R.string.trakt_error_credentials);
    }

    @Override
    protected void fetchTokensAndFinish(@Nullable String authCode, @Nullable String state) {
        activateFallbackButtons();

        if (model.getConnectTask() != null
                && model.getConnectTask().getStatus() != AsyncTask.Status.FINISHED) {
            // connect task is still running
            setMessage(getString(R.string.waitplease), true);
            return;
        }

        // if state does not match what we sent, drop the auth code
        if (this.state == null || !this.state.equals(state)) {
            // log trakt OAuth failures
            Errors.logAndReportNoBend(ACTION_FETCHING_TOKENS,
                    new TraktOAuthError(ACTION_FETCHING_TOKENS, ERROR_DESCRIPTION_STATE_MISMATCH));

            setMessage(getAuthErrorMessage() + (this.state == null ?
                    "\n\n(State is null.)" :
                    "\n\n(State does not match. Cross-site request forgery detected.)"));
            return;
        }

        if (TextUtils.isEmpty(authCode)) {
            // no valid auth code, remain in activity and show fallback buttons
            Timber.e("Failed because no auth code returned.");
            setMessage(getAuthErrorMessage() + "\n\n(No auth code returned.)");
            return;
        }

        // fetch access token with given OAuth auth code
        setMessage(getString(R.string.waitplease), true);
        ConnectTraktTask task = new ConnectTraktTask(this);
        Utils.executeInOrder(task, authCode);
        model.setConnectTask(task);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ConnectTraktTask.TaskResult event) {
        model.setConnectTask(null);

        int resultCode = event.resultCode;
        if (resultCode == TraktResult.SUCCESS) {
            // if we got here, looks like credentials were stored successfully
            finish();
            return;
        }

        // handle errors
        CharSequence errorText;
        switch (resultCode) {
            case TraktResult.OFFLINE:
                errorText = getString(R.string.offline);
                break;
            case TraktResult.API_ERROR:
                errorText = getString(R.string.api_error_generic, getString(R.string.trakt));
                break;
            case TraktResult.ACCOUNT_LOCKED:
                errorText = getString(R.string.trakt_error_account_locked);
                break;
            case TraktResult.AUTH_ERROR:
            case TraktResult.ERROR:
            default:
                errorText = getString(R.string.trakt_error_credentials);
                break;
        }

        if (event.debugMessage != null) {
            errorText = HtmlCompat.fromHtml(
                    "<p>" + errorText + "</p><p><i>" + event.debugMessage + "</i></p>",
                    HtmlCompat.FROM_HTML_MODE_COMPACT
            );
        }

        setMessage(errorText);
        activateFallbackButtons();
    }
}
