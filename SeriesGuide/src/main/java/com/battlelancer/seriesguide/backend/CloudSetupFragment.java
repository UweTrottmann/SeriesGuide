package com.battlelancer.seriesguide.backend;

import android.accounts.Account;
import android.content.Context;
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
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.ui.dialogs.RemoveCloudAccountDialogFragment;
import com.battlelancer.seriesguide.util.Utils;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
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

    private Button buttonAction;
    private TextView textViewDescription;
    private TextView textViewUsername;
    private ProgressBar progressBar;
    private Button buttonRemoveAccount;
    private TextView textViewWarning;

    private HexagonSetupTask hexagonSetupTask;
    private GoogleApiClient googleApiClient;
    private Snackbar snackbar;

    @Nullable private GoogleSignInAccount signInAccount;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_cloud_setup, container, false);

        textViewDescription = (TextView) v.findViewById(R.id.textViewCloudDescription);
        textViewUsername = ButterKnife.findById(v, R.id.textViewCloudUsername);
        textViewWarning = ButterKnife.findById(v, R.id.textViewCloudWarnings);
        progressBar = (ProgressBar) v.findViewById(R.id.progressBarCloud);
        buttonAction = (Button) v.findViewById(R.id.buttonCloudAction);
        buttonRemoveAccount = ButterKnife.findById(v, R.id.buttonCloudRemoveAccount);
        buttonRemoveAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment f = new RemoveCloudAccountDialogFragment();
                f.show(getFragmentManager(), "remove-cloud-account");
            }
        });

        updateViews();
        setProgressVisible(true);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        GoogleSignInOptions gso =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build();
        googleApiClient = new GoogleApiClient.Builder(getContext())
                .enableAutoManage(getActivity(), onGoogleConnectionFailedListener)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
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
            if (handleSignInResult(result)) {
                startHexagonSetup();
            }
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
    public void onDestroy() {
        super.onDestroy();
        if (isHexagonSetupRunning()) {
            hexagonSetupTask.cancel(true);
        }
        hexagonSetupTask = null;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(
            RemoveCloudAccountDialogFragment.RemoveHexagonAccountTask.HexagonAccountRemovedEvent event) {
        event.handle(getActivity());
        updateViews();
    }

    /**
     * @return Whether there is now a signed in Google account available.
     */
    private boolean handleSignInResult(GoogleSignInResult result) {
        boolean signedIn = result.isSuccess();
        if (signedIn) {
            Timber.i("Signed in with Google.");
            signInAccount = result.getSignInAccount();
        } else {
            // not or no longer signed in
            Timber.d("NOT signed in with Google: %s", GoogleSignInStatusCodes.getStatusCodeString(
                    result.getStatus().getStatusCode()));
            signInAccount = null;
            // remove any existing account
            HexagonTools.storeAccount(getContext(), null);
        }

        setProgressVisible(false);
        updateViews();

        return signedIn;
    }

    private void signInWithGoogle() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(signInIntent, REQUEST_SIGN_IN);
    }

    private void signOutAndDisableHexagon() {
        setProgressVisible(true);
        Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        setProgressVisible(false);

                        if (status.isSuccess()) {
                            Timber.i("Signed out of Google.");
                            signInAccount = null;
                            // remove cached account to disable hexagon
                            HexagonTools.storeAccount(getActivity(), null);
                            updateViews();
                        } else {
                            Timber.e("Signing out of Google failed: %s", status);
                        }
                    }
                });
    }

    private void updateViews() {
        // warn about changes in behavior with trakt
        textViewWarning.setVisibility(TraktCredentials.get(getActivity()).hasCredentials()
                ? View.VISIBLE : View.GONE);

        // hexagon not configured?
        if (!HexagonTools.isConfigured(getActivity())) {
            // did try to setup, but failed?
            if (!HexagonSettings.hasCompletedSetup(getActivity())) {
                // show error message
                textViewDescription.setText(R.string.hexagon_setup_incomplete);
            } else {
                textViewDescription.setText(R.string.hexagon_description);
            }
            textViewUsername.setVisibility(View.GONE);

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
        } else {
            // configured!
            textViewUsername.setText(HexagonSettings.getAccountName(getActivity()));
            textViewUsername.setVisibility(View.VISIBLE);
            textViewDescription.setText(R.string.hexagon_signed_in);

            // enable sign-out
            buttonAction.setText(R.string.hexagon_signout);
            buttonAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    signOutAndDisableHexagon();
                }
            });
            // enable account removal
            buttonRemoveAccount.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Disables buttons and shows a progress bar.
     */
    private void setProgressVisible(boolean isVisible) {
        progressBar.setVisibility(isVisible ? View.VISIBLE : View.GONE);

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
            signInWithGoogle();
        } else {
            hexagonSetupTask = new HexagonSetupTask(getActivity(), signInAccount,
                    onHexagonSetupFinishedListener);
            hexagonSetupTask.execute();
        }
    }

    private boolean isHexagonSetupRunning() {
        return hexagonSetupTask != null
                && hexagonSetupTask.getStatus() != AsyncTask.Status.FINISHED;
    }

    private static class HexagonSetupTask extends AsyncTask<String, Void, Integer> {

        public static final int SYNC_REQUIRED = 1;
        public static final int FAILURE = -1;
        public static final int FAILURE_AUTH = -2;

        public interface OnSetupFinishedListener {

            void onSetupFinished(int resultCode);
        }

        private final Context context;
        @NonNull private final GoogleSignInAccount signInAccount;
        private OnSetupFinishedListener onSetupFinishedListener;

        /**
         * Checks for local and remote shows and uploads shows accordingly. If there are some shows
         * in the local database as well as on hexagon, will download and merge data first, then
         * upload.
         */
        public HexagonSetupTask(Context context, @NonNull GoogleSignInAccount signInAccount,
                OnSetupFinishedListener listener) {
            this.context = context.getApplicationContext();
            this.signInAccount = signInAccount;
            onSetupFinishedListener = listener;
        }

        @Override
        protected Integer doInBackground(String... params) {
            // set setup incomplete flag
            Timber.i("Setting up Hexagon...");
            HexagonSettings.setSetupIncomplete(context);

            // validate account data
            Account account = signInAccount.getAccount();
            if (TextUtils.isEmpty(signInAccount.getEmail()) || account == null) {
                return FAILURE_AUTH;
            }

            if (!HexagonSettings.resetSyncState(context)) {
                return FAILURE;
            }

            // at last store the new credentials (enables SG hexagon integration)
            HexagonTools.storeAccount(context, signInAccount);
            if (!HexagonTools.isConfigured(context)) {
                return FAILURE_AUTH;
            }

            return SYNC_REQUIRED;
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
                case HexagonSetupTask.SYNC_REQUIRED: {
                    // schedule full sync
                    Timber.d("Setting up Hexagon...SYNC_REQUIRED");
                    SgSyncAdapter.requestSyncImmediate(getActivity(), SgSyncAdapter.SyncType.FULL,
                            0, false);
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
            Timber.e("GoogleApiClient connect failed: %s", connectionResult);
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
