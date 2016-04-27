/*
 * Copyright 2016 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.backend;

import android.Manifest;
import android.accounts.AccountManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.ui.dialogs.RemoveCloudAccountDialogFragment;
import com.battlelancer.seriesguide.util.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * Helps connecting a device to Hexagon: sign in via Google account, initial uploading of shows.
 */
public class CloudSetupFragment extends Fragment {

    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;
    private static final int REQUEST_CODE_SIGN_IN = 1;
    private static final int REQUEST_ACCOUNT_PICKER = 2;

    private Button buttonAction;
    private TextView textViewDescription;
    private TextView textViewUsername;
    private ProgressBar progressBar;
    private Button buttonRemoveAccount;
    private TextView textViewWarning;

    private HexagonSetupTask hexagonSetupTask;

    private boolean isProgressLocked;

    private boolean isGooglePlayMissingLocked;
    private Snackbar snackbar;

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

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        updateViewStates();

        // lock down UI if task is still running
        if (hexagonSetupTask != null
                && hexagonSetupTask.getStatus() != AsyncTask.Status.FINISHED) {
            setProgressLock(true); // prevent duplicate tasks
        }
    }

    private void updateViewStates() {
        // setup not in progress
        progressBar.setVisibility(View.GONE);
        // warn about changes in behavior with trakt
        String warning = getString(R.string.hexagon_warning_lists);
        if (TraktCredentials.get(getActivity()).hasCredentials()) {
            warning += "\n" + getString(R.string.hexagon_warning_trakt);
        }
        textViewWarning.setText(warning);

        // not signed in?
        if (!HexagonTools.isSignedIn(getActivity())) {
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
                        trySignIn();
                    } else {
                        Utils.advertiseSubscription(getActivity());
                    }
                }
            });
            // disable account removal
            buttonRemoveAccount.setVisibility(View.GONE);
            return;
        }

        // signed in!
        textViewUsername.setText(HexagonSettings.getAccountName(getActivity()));
        textViewUsername.setVisibility(View.VISIBLE);

        // enable sign-out
        textViewDescription.setText(R.string.hexagon_signed_in);
        buttonAction.setText(R.string.hexagon_signout);
        buttonAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });
        // enable account removal
        buttonRemoveAccount.setVisibility(View.VISIBLE);
    }

    /**
     * Disables the action button and shows a progress bar.
     */
    private void setProgressLock(boolean isLocked) {
        isProgressLocked = isLocked;
        progressBar.setVisibility(isLocked ? View.VISIBLE : View.GONE);

        // always disable if no Google Play services available
        if (isGooglePlayMissingLocked) {
            buttonAction.setEnabled(false);
            return;
        }
        buttonAction.setEnabled(!isLocked);
        buttonRemoveAccount.setEnabled(!isLocked);
    }

    /**
     * Disables the action button.
     */
    private void setLock(boolean isLocked) {
        isGooglePlayMissingLocked = isLocked;

        // always disable if ongoing progress
        if (isProgressLocked) {
            buttonAction.setEnabled(false);
            buttonRemoveAccount.setEnabled(false);
            return;
        }
        buttonAction.setEnabled(!isLocked);
        buttonRemoveAccount.setEnabled(!isLocked);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ACCOUNT_PICKER: {
                if (data != null && data.getExtras() != null) {
                    String accountName = data.getExtras().getString(
                            AccountManager.KEY_ACCOUNT_NAME);
                    if (!TextUtils.isEmpty(accountName)) {
                        setupHexagon(accountName);
                    }
                }
                break;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // disable UI if no Google Play services available
        checkGooglePlayServicesAvailable();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        if (hexagonSetupTask != null
                && hexagonSetupTask.getStatus() != AsyncTask.Status.FINISHED) {
            hexagonSetupTask.cancel(true);
        }
        hexagonSetupTask = null;

        super.onDestroy();
    }

    public void onEventMainThread(
            RemoveCloudAccountDialogFragment.RemoveHexagonAccountTask.HexagonAccountRemovedEvent event) {
        event.handle(getActivity());
        updateViewStates();
    }

    /**
     * Ensure Google Play Services is up to date, if not help the user update it.
     */
    private void checkGooglePlayServicesAvailable() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int connectionStatusCode = googleApiAvailability
                .isGooglePlayServicesAvailable(getActivity());
        if (googleApiAvailability.isUserResolvableError(connectionStatusCode)) {
            setLock(true);
            googleApiAvailability.getErrorDialog(getActivity(), connectionStatusCode,
                    REQUEST_GOOGLE_PLAY_SERVICES, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            showGooglePlayServicesWarning();
                        }
                    }).show();
        } else if (connectionStatusCode != ConnectionResult.SUCCESS) {
            setLock(true);
            showGooglePlayServicesWarning();
            Timber.i("This device is not supported. Code %s", connectionStatusCode);
        } else {
            setLock(false);
        }
    }

    private void showGooglePlayServicesWarning() {
        if (getView() == null) {
            return;
        }
        if (snackbar != null) {
            snackbar.dismiss();
        }
        snackbar = Snackbar.make(getView(), R.string.hexagon_google_play_missing,
                Snackbar.LENGTH_INDEFINITE);
        snackbar.show();
    }

    private void trySignIn() {
        // make sure we have the required permissions
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            // don't have it? request it, do task if granted
            requestPermissions(new String[] { Manifest.permission.GET_ACCOUNTS },
                    REQUEST_CODE_SIGN_IN);
            return;
        }

        doSignIn();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_SIGN_IN) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doSignIn();
            } else {
                if (getView() != null) {
                    Snackbar.make(getView(), R.string.hexagon_permission_missing,
                            Snackbar.LENGTH_LONG).show();
                }
            }
        }
    }

    private void doSignIn() {
        // launch account picker
        startActivityForResult(
                HexagonTools.getAccountCredential(getActivity()).newChooseAccountIntent(),
                REQUEST_ACCOUNT_PICKER);
    }

    private void signOut() {
        // remove account name from settings
        HexagonTools.storeAccountName(getActivity(), null);

        updateViewStates();
    }

    private void setupHexagon(String accountName) {
        setProgressLock(true);  // prevent duplicate tasks

        hexagonSetupTask = new HexagonSetupTask(getActivity(), mSetupFinishedListener);
        hexagonSetupTask.execute(accountName);
    }

    private static class HexagonSetupTask extends AsyncTask<String, Void, Integer> {

        public static final int SYNC_REQUIRED = 1;

        public static final int FAILURE = -1;

        public static final int FAILURE_AUTH = -2;

        public interface OnSetupFinishedListener {

            void onSetupFinished(int resultCode);
        }

        private final Context mContext;

        private OnSetupFinishedListener mOnSetupFinishedListener;

        /**
         * Checks for local and remote shows and uploads shows accordingly. If there are some shows
         * in the local database as well as on hexagon, will download and merge data first, then
         * upload.
         */
        public HexagonSetupTask(Context context, OnSetupFinishedListener listener) {
            mContext = context.getApplicationContext();
            mOnSetupFinishedListener = listener;
        }

        @Override
        protected Integer doInBackground(String... params) {
            // set setup incomplete flag
            Timber.i("Setting up Hexagon...");
            HexagonSettings.setSetupIncomplete(mContext);

            // validate auth data
            String accountName = params[0];
            if (TextUtils.isEmpty(accountName)
                    || !HexagonTools.validateAccount(mContext, accountName)) {
                return FAILURE_AUTH;
            }

            // set all shows as not merged with Hexagon
            ContentValues values = new ContentValues();
            values.put(SeriesGuideContract.Shows.HEXAGON_MERGE_COMPLETE, false);
            mContext.getContentResolver().update(SeriesGuideContract.Shows.CONTENT_URI, values,
                    null, null);

            // reset sync related properties
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
                    mContext).edit();
            editor.putBoolean(HexagonSettings.KEY_MERGED_SHOWS, false);
            editor.putBoolean(HexagonSettings.KEY_MERGED_MOVIES, false);
            editor.remove(HexagonSettings.KEY_LAST_SYNC_EPISODES);
            editor.remove(HexagonSettings.KEY_LAST_SYNC_SHOWS);
            editor.remove(HexagonSettings.KEY_LAST_SYNC_MOVIES);
            if (!editor.commit()) {
                return FAILURE;
            }

            // at last store the new credentials (enables SG hexagon integration)
            HexagonTools.storeAccountName(mContext, accountName);
            if (!HexagonTools.isSignedIn(mContext)) {
                return FAILURE_AUTH;
            }

            return SYNC_REQUIRED;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (mOnSetupFinishedListener != null) {
                mOnSetupFinishedListener.onSetupFinished(result);
            }
        }
    }

    private HexagonSetupTask.OnSetupFinishedListener mSetupFinishedListener
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

            updateViewStates();
            setProgressLock(false); // allow new task
        }
    };
}
