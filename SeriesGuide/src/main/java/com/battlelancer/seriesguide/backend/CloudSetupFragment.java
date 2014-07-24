/*
 * Copyright 2014 Uwe Trottmann
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

import android.accounts.AccountManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.ui.dialogs.RemoveCloudAccountDialogFragment;
import com.battlelancer.seriesguide.util.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * Helps connecting a device to Hexagon: sign in via Google account, initial uploading of shows.
 */
public class CloudSetupFragment extends Fragment {

    private Button mButtonAction;
    private TextView mTextViewDescription;
    private ProgressBar mProgressBar;
    private Button mButtonRemoveAccount;
    private TextView mTextViewWarning;

    private HexagonSetupTask mHexagonSetupTask;

    private boolean mIsProgressLocked;

    private boolean mIsGooglePlayMissingLocked;

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

        mTextViewDescription = (TextView) v.findViewById(R.id.textViewCloudDescription);
        mTextViewWarning = ButterKnife.findById(v, R.id.textViewCloudWarnings);
        mProgressBar = (ProgressBar) v.findViewById(R.id.progressBarCloud);
        mButtonAction = (Button) v.findViewById(R.id.buttonCloudAction);
        mButtonRemoveAccount = ButterKnife.findById(v, R.id.buttonCloudRemoveAccount);
        mButtonRemoveAccount.setOnClickListener(new View.OnClickListener() {
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
        if (mHexagonSetupTask != null
                && mHexagonSetupTask.getStatus() != AsyncTask.Status.FINISHED) {
            setProgressLock(true); // prevent duplicate tasks
        }
    }

    private void updateViewStates() {
        // setup not in progress
        mProgressBar.setVisibility(View.GONE);
        // warn about changes in behavior with trakt
        String warning = getString(R.string.hexagon_warning_lists);
        if (TraktCredentials.get(getActivity()).hasCredentials()) {
            warning += "\n" + getString(R.string.hexagon_warning_trakt);
        }
        mTextViewWarning.setText(warning);

        // not signed in?
        if (!HexagonTools.isSignedIn(getActivity())) {
            // did try to setup, but failed?
            if (!HexagonSettings.hasCompletedSetup(getActivity())) {
                // show error message
                mTextViewDescription.setText(R.string.hexagon_setup_incomplete);
            } else {
                mTextViewDescription.setText(R.string.hexagon_description);
            }
            // enable sign-in
            mButtonAction.setText(R.string.hexagon_signin);
            mButtonAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // restrict access to supporters
                    if (Utils.hasAccessToX(getActivity())) {
                        signIn();
                    } else {
                        Utils.advertiseSubscription(getActivity());
                    }
                }
            });
            // disable account removal
            mButtonRemoveAccount.setVisibility(View.GONE);
            return;
        }

        // signed in!

        // enable sign-out
        mTextViewDescription.setText(R.string.hexagon_signed_in);
        mButtonAction.setText(R.string.hexagon_signout);
        mButtonAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });
        // enable account removal
        mButtonRemoveAccount.setVisibility(View.VISIBLE);
    }

    /**
     * Disables the action button and shows a progress bar.
     */
    private void setProgressLock(boolean isLocked) {
        mIsProgressLocked = isLocked;
        mProgressBar.setVisibility(isLocked ? View.VISIBLE : View.GONE);

        // always disable if no Google Play services available
        if (mIsGooglePlayMissingLocked) {
            mButtonAction.setEnabled(false);
            return;
        }
        mButtonAction.setEnabled(!isLocked);
        mButtonRemoveAccount.setEnabled(!isLocked);
    }

    /**
     * Disables the action button.
     */
    private void setLock(boolean isLocked) {
        mIsGooglePlayMissingLocked = isLocked;

        // always disable if ongoing progress
        if (mIsProgressLocked) {
            mButtonAction.setEnabled(false);
            mButtonRemoveAccount.setEnabled(false);
            return;
        }
        mButtonAction.setEnabled(!isLocked);
        mButtonRemoveAccount.setEnabled(!isLocked);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CloudSetupActivity.REQUEST_ACCOUNT_PICKER: {
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
        if (mHexagonSetupTask != null
                && mHexagonSetupTask.getStatus() != AsyncTask.Status.FINISHED) {
            mHexagonSetupTask.cancel(true);
        }
        mHexagonSetupTask = null;

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
        final int connectionStatusCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(getActivity());
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            GooglePlayServicesUtil
                    .getErrorDialog(connectionStatusCode, getActivity(),
                            CloudSetupActivity.REQUEST_GOOGLE_PLAY_SERVICES).show();
            setLock(true);
            return;
        }
        if (connectionStatusCode != ConnectionResult.SUCCESS) {
            Timber.i("This device is not supported.");
            Toast.makeText(getActivity(), "This device is not supported.", Toast.LENGTH_LONG)
                    .show();
            setLock(true);
            return;
        }
        setLock(false);
    }

    private void signIn() {
        // launch account picker
        startActivityForResult(
                HexagonTools.getAccountCredential(getActivity()).newChooseAccountIntent(),
                CloudSetupActivity.REQUEST_ACCOUNT_PICKER);
    }

    private void signOut() {
        // remove account name from settings
        HexagonTools.storeAccountName(getActivity(), null);

        updateViewStates();
    }

    private void setupHexagon(String accountName) {
        setProgressLock(true);  // prevent duplicate tasks

        mHexagonSetupTask = new HexagonSetupTask(getActivity(), mSetupFinishedListener);
        mHexagonSetupTask.execute(accountName);
    }

    private static class HexagonSetupTask extends AsyncTask<String, Void, Integer> {

        public static final int SYNC_REQUIRED = 1;

        public static final int FAILURE = -1;

        public static final int FAILURE_AUTH = -2;

        public interface OnSetupFinishedListener {

            public void onSetupFinished(int resultCode);
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
                    Toast.makeText(getActivity(), R.string.hexagon_setup_fail_auth,
                            Toast.LENGTH_LONG).show();
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
