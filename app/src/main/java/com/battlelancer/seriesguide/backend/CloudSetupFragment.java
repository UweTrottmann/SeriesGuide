package com.battlelancer.seriesguide.backend;

import android.accounts.Account;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.sync.SyncProgress;
import com.battlelancer.seriesguide.traktapi.ConnectTraktActivity;
import com.battlelancer.seriesguide.traktapi.TraktCredentials;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.widgets.SyncStatusView;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import timber.log.Timber;

/**
 * Helps connecting a device to Hexagon: sign in via Google account, initial uploading of shows.
 */
public class CloudSetupFragment extends Fragment {

    private static final int REQUEST_SIGN_IN = 1;
    private static final String ACTION_SIGN_IN = "sign-in";

    @BindView(R.id.buttonCloudAction) Button buttonAction;
    @BindView(R.id.textViewCloudDescription) TextView textViewDescription;
    @BindView(R.id.textViewCloudUser) TextView textViewUsername;
    @BindView(R.id.progressBarCloudAccount) ProgressBar progressBarAccount;
    @BindView(R.id.syncStatusCloud) SyncStatusView syncStatusView;
    @BindView(R.id.buttonCloudRemoveAccount) Button buttonRemoveAccount;
    @BindView(R.id.textViewCloudWarnings) TextView textViewWarning;
    private Unbinder unbinder;

    private Snackbar snackbar;

    private GoogleApiClient googleApiClient;
    @Nullable private GoogleSignInAccount signInAccount;
    private HexagonTools hexagonTools;
    private HexagonSetupTask hexagonSetupTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Try to keep the fragment around on config changes so the setup task
         * does not have to be finished.
         */
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_cloud_setup, container, false);
        unbinder = ButterKnife.bind(this, v);

        textViewWarning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // link to trakt account activity which has details about disabled features
                startActivity(new Intent(getContext(), ConnectTraktActivity.class));
            }
        });

        buttonRemoveAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setProgressVisible(true);
                DialogFragment f = new RemoveCloudAccountDialogFragment();
                f.show(getFragmentManager(), "remove-cloud-account");
            }
        });

        updateViews();
        setProgressVisible(true);
        syncStatusView.setVisibility(View.GONE);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        hexagonTools = SgApp.getServicesComponent(getContext()).hexagonTools();
        googleApiClient = new GoogleApiClient.Builder(getContext())
                .enableAutoManage(getActivity(), onGoogleConnectionFailedListener)
                .addOnConnectionFailedListener(onGoogleConnectionFailedListener)
                .addApi(Auth.GOOGLE_SIGN_IN_API, HexagonTools.getGoogleSignInOptions())
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!isHexagonSetupRunning()) {
            // check if the user is still signed in
            OptionalPendingResult<GoogleSignInResult> pendingResult = Auth.GoogleSignInApi
                    .silentSignIn(googleApiClient);
            if (pendingResult.isDone()) {
                // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
                // and the GoogleSignInResult will be available instantly.
                Timber.d("Got cached sign-in");
                handleSignInResult(pendingResult.get());
            } else {
                // If the user has not previously signed in on this device or the sign-in has expired,
                // this asynchronous branch will attempt to sign in the user silently.  Cross-device
                // single sign-on will occur in this branch.
                Timber.d("Trying async sign-in");
                pendingResult.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                    @Override
                    public void onResult(@NonNull GoogleSignInResult googleSignInResult) {
                        handleSignInResult(googleSignInResult);
                    }
                });
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isHexagonSetupRunning()) {
            hexagonSetupTask.cancel(true);
        }
        hexagonSetupTask = null;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(RemoveCloudAccountDialogFragment.CanceledEvent event) {
        setProgressVisible(false);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(RemoveCloudAccountDialogFragment.AccountRemovedEvent event) {
        event.handle(getActivity());
        setProgressVisible(false);
        updateViews();
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEvent(SyncProgress.SyncEvent event) {
        syncStatusView.setProgress(event);
    }

    /**
     * On sign-in success, saves the signed in Google account and auto-starts setup if Cloud is not
     * enabled, yet. On sign-in failure disables Cloud.
     *
     * @param result May be null (here if coming from onActivityResult).
     */
    private void handleSignInResult(@Nullable GoogleSignInResult result) {
        boolean signedIn = result != null && result.isSuccess();
        if (signedIn) {
            Timber.i("Signed in with Google.");
            signInAccount = result.getSignInAccount();
        } else {
            // not or no longer signed in
            if (result != null) {
                hexagonTools.trackSignInFailure(ACTION_SIGN_IN, result.getStatus());
            } else {
                hexagonTools.trackSignInFailure(ACTION_SIGN_IN, "result is null");
            }
            signInAccount = null;
            hexagonTools.setDisabled();
        }

        setProgressVisible(false);
        updateViews();

        if (signedIn && Utils.hasAccessToX(getContext())
                && !HexagonSettings.isEnabled(getContext())) {
            // auto-start setup if sign in succeeded and Cloud can be, but is not enabled, yet
            Timber.i("Auto-start Cloud setup.");
            startHexagonSetup();
        }
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(signInIntent, REQUEST_SIGN_IN);
    }

    private void signOut() {
        setProgressVisible(true);
        Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        setProgressVisible(false);

                        if (status.isSuccess()) {
                            Timber.i("Signed out of Google.");
                            signInAccount = null;
                            hexagonTools.setDisabled();
                            updateViews();
                        } else {
                            hexagonTools.trackSignInFailure("sign-out", status);
                        }
                    }
                });
    }

    private void updateViews() {
        // warn about changes in behavior with trakt
        textViewWarning.setVisibility(TraktCredentials.get(getActivity()).hasCredentials()
                ? View.VISIBLE : View.GONE);

        // hexagon enabled and account looks fine?
        if (HexagonSettings.isEnabled(getContext())
                && !HexagonSettings.shouldValidateAccount(getContext())) {
            textViewUsername.setText(HexagonSettings.getAccountName(getActivity()));
            textViewDescription.setText(R.string.hexagon_description);

            // enable sign-out
            buttonAction.setText(R.string.hexagon_signout);
            buttonAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    signOut();
                }
            });
            // enable account removal
            buttonRemoveAccount.setVisibility(View.VISIBLE);
        } else {
            // did try to setup, but failed?
            if (!HexagonSettings.hasCompletedSetup(getActivity())) {
                // show error message
                textViewDescription.setText(R.string.hexagon_setup_incomplete);
            } else {
                textViewDescription.setText(R.string.hexagon_description);
            }
            textViewUsername.setText(null);

            // enable sign-in
            buttonAction.setText(R.string.hexagon_signin);
            buttonAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // restrict access to supporters
                    if (Utils.hasAccessToX(getActivity())) {
                        startHexagonSetup();
                    } else {
                        Utils.advertiseSubscription(getActivity());
                    }
                }
            });
            // disable account removal
            buttonRemoveAccount.setVisibility(View.GONE);
        }
    }

    /**
     * Disables buttons and shows a progress bar.
     */
    private void setProgressVisible(boolean isVisible) {
        progressBarAccount.setVisibility(isVisible ? View.VISIBLE : View.GONE);

        buttonAction.setEnabled(!isVisible);
        buttonRemoveAccount.setEnabled(!isVisible);
    }

    /**
     * Disables all buttons (use if signing in with Google seems not possible).
     */
    private void setDisabled() {
        buttonAction.setEnabled(false);
        buttonRemoveAccount.setEnabled(false);
    }

    private void startHexagonSetup() {
        setProgressVisible(true);

        if (signInAccount == null) {
            signIn();
        } else if (!isHexagonSetupRunning()) {
            HexagonSettings.setSetupIncomplete(getContext());
            hexagonSetupTask = new HexagonSetupTask(hexagonTools, signInAccount,
                    onHexagonSetupFinishedListener);
            hexagonSetupTask.execute();
        }
    }

    private boolean isHexagonSetupRunning() {
        return hexagonSetupTask != null
                && hexagonSetupTask.getStatus() != AsyncTask.Status.FINISHED;
    }

    private static class HexagonSetupTask extends AsyncTask<String, Void, Integer> {

        public static final int SUCCESS_SYNC_REQUIRED = 1;
        public static final int FAILURE = -1;
        public static final int FAILURE_AUTH = -2;

        public interface OnSetupFinishedListener {

            void onSetupFinished(int resultCode);
        }

        private final HexagonTools hexagonTools;
        @NonNull private final GoogleSignInAccount signInAccount;
        private OnSetupFinishedListener onSetupFinishedListener;

        /**
         * Checks for local and remote shows and uploads shows accordingly. If there are some shows
         * in the local database as well as on hexagon, will download and merge data first, then
         * upload.
         */
        public HexagonSetupTask(HexagonTools hexagonTools,
                @NonNull GoogleSignInAccount signInAccount, OnSetupFinishedListener listener) {
            this.hexagonTools = hexagonTools;
            this.signInAccount = signInAccount;
            onSetupFinishedListener = listener;
        }

        @Override
        protected Integer doInBackground(String... params) {
            // set setup incomplete flag
            Timber.i("Setting up Hexagon...");

            // validate account data
            Account account = signInAccount.getAccount();
            if (TextUtils.isEmpty(signInAccount.getEmail()) || account == null) {
                return FAILURE_AUTH;
            }

            // at last reset sync state, store the new credentials and enable hexagon integration
            if (!hexagonTools.setEnabled(signInAccount)) {
                return FAILURE;
            }

            return SUCCESS_SYNC_REQUIRED;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (onSetupFinishedListener != null) {
                onSetupFinishedListener.onSetupFinished(result);
            }
        }
    }

    private HexagonSetupTask.OnSetupFinishedListener onHexagonSetupFinishedListener
            = new HexagonSetupTask.OnSetupFinishedListener() {
        @Override
        public void onSetupFinished(int resultCode) {
            switch (resultCode) {
                case HexagonSetupTask.SUCCESS_SYNC_REQUIRED: {
                    // schedule full sync
                    Timber.d("Setting up Hexagon...SUCCESS_SYNC_REQUIRED");
                    SgSyncAdapter.requestSyncFullImmediate(getActivity(), false);
                    HexagonSettings.setSetupCompleted(getActivity());
                    break;
                }
                case HexagonSetupTask.FAILURE_AUTH: {
                    // show setup incomplete message + error toast
                    if (getView() != null) {
                        Snackbar.make(getView(), R.string.hexagon_setup_fail_auth,
                                Snackbar.LENGTH_LONG).show();
                    }
                    Timber.d("Setting up Hexagon...FAILURE_AUTH");
                    break;
                }
                case HexagonSetupTask.FAILURE: {
                    // show setup incomplete message
                    Timber.d("Setting up Hexagon...FAILURE");
                    break;
                }
            }

            if (getView() == null) {
                return;
            }
            setProgressVisible(false); // allow new task
            updateViews();
        }
    };

    private GoogleApiClient.OnConnectionFailedListener onGoogleConnectionFailedListener
            = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            // using auto managed connection so only called if unresolvable error
            hexagonTools.trackSignInFailure(ACTION_SIGN_IN, connectionResult);
            if (getView() == null) {
                return;
            }
            setProgressVisible(false);
            setDisabled();
            if (snackbar != null) {
                snackbar.dismiss();
            }
            snackbar = Snackbar.make(getView(), R.string.hexagon_google_play_missing,
                    Snackbar.LENGTH_INDEFINITE);
            snackbar.show();
        }
    };
}
