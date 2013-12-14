/*
 * Copyright 2013 Uwe Trottmann
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
 *
 */

package com.battlelancer.seriesguide.backend;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import com.actionbarsherlock.app.SherlockFragment;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.R;
import com.uwetrottmann.seriesguide.shows.model.Show;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;

/**
 * Helps connecting a device to Hexagon: sign in via Google account, initial uploading of shows.
 */
public class CloudSetupFragment extends SherlockFragment {

    public static final String TAG = "Hexagon";

    private Button mButtonAction;

    private TextView mTextViewDescription;

    private ProgressBar mProgressBar;

    private RadioGroup mRadioGroupPriority;

    private GoogleAccountCredential mCredential;

    private HexagonSetupTask mHexagonSetupTask;

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

        mButtonAction = (Button) v.findViewById(R.id.buttonRegisterAction);
        mTextViewDescription = (TextView) v.findViewById(R.id.textViewRegisterDescription);
        mProgressBar = (ProgressBar) v.findViewById(R.id.progressBarRegister);
        mRadioGroupPriority = (RadioGroup) v.findViewById(R.id.radioGroupRegisterPriority);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mCredential = GoogleAccountCredential
                .usingAudience(getActivity(), HexagonSettings.AUDIENCE);
        setAccountName(HexagonSettings.getAccountName(getActivity()));

        updateViewsStates(false);

        // lock down UI if task is still running
        if (mHexagonSetupTask != null
                && mHexagonSetupTask.getStatus() != AsyncTask.Status.FINISHED) {
            setProgressLock(true); // prevent duplicate tasks
        }
    }

    private void updateViewsStates(boolean enableVersionDecision) {
        // setup not in progress
        mProgressBar.setVisibility(View.GONE);

        // display user decision form?
        if (enableVersionDecision) {
            // require decision before upload: show options
            mRadioGroupPriority.setVisibility(View.VISIBLE);
            mRadioGroupPriority.check(R.id.radioButtonRegisterPriorityCloud); // default to cloud
            mTextViewDescription.setText(R.string.hexagon_priority_choice_required);
            mButtonAction.setText(R.string.hexagon_priority_select);
            mButtonAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // create new setup task, give it user decision on duplicates
                    boolean isCloudOverwrites = mRadioGroupPriority.getCheckedRadioButtonId()
                            == R.id.radioButtonRegisterPriorityCloud;

                    setProgressLock(true);  // prevent duplicate tasks
                    mHexagonSetupTask = new HexagonSetupTask(getActivity(), mSetupFinishedListener,
                            isCloudOverwrites);
                    mHexagonSetupTask.execute();
                }
            });
            return;
        }

        // don't display user decision form
        mRadioGroupPriority.setVisibility(View.GONE);

        // not signed in?
        if (!isSignedIn()) {
            mTextViewDescription.setText(R.string.hexagon_description);
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
            return;
        }

        // setup complete?
        if (HexagonSettings.hasCompletedSetup(getActivity())) {
            // enable sign-out
            mTextViewDescription.setText(R.string.hexagon_signed_in);
            mButtonAction.setText(R.string.hexagon_signout);
            mButtonAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    signOut();
                }
            });
            return;
        }

        // setup failed, offer action to re-try
        mTextViewDescription.setText(R.string.hexagon_setup_incomplete);
        mButtonAction.setText(R.string.hexagon_setup_complete);
        mButtonAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupHexagon();
            }
        });
    }

    /**
     * Disable the action button and show a progress bar.
     */
    private void setProgressLock(boolean isLocked) {
        mProgressBar.setVisibility(isLocked ? View.VISIBLE : View.GONE);
        mButtonAction.setEnabled(!isLocked);
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
                        storeAccountName(accountName);
                        setAccountName(accountName);
                        if (isSignedIn()) {
                            setupHexagon();
                        } else {
                            updateViewsStates(false);
                        }
                    }
                }
                break;
            }
        }
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

    private boolean isSignedIn() {
        return mCredential.getSelectedAccountName() != null;
    }

    private void signIn() {
        // launch account picker
        startActivityForResult(mCredential.newChooseAccountIntent(),
                CloudSetupActivity.REQUEST_ACCOUNT_PICKER);
    }

    private void signOut() {
        // remove account name from settings
        storeAccountName(null);
        setAccountName(null);

        updateViewsStates(false);
    }

    private void setAccountName(String accountName) {
        mCredential.setSelectedAccountName(accountName);
    }

    private void storeAccountName(String accountName) {
        // store account name in settings
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .edit()
                .putString(HexagonSettings.KEY_ACCOUNT_NAME, accountName)
                .commit();

        ShowTools.get(getActivity()).setShowsServiceAccountName(accountName);
    }

    private void setupHexagon() {
        setProgressLock(true);  // prevent duplicate tasks

        mHexagonSetupTask = new HexagonSetupTask(getActivity(), mSetupFinishedListener);
        mHexagonSetupTask.execute();
    }

    private static class HexagonSetupTask extends AsyncTask<Void, Void, Integer> {

        public static final int USER_ACTION_REQUIRED = 2;

        public static final int SYNC_REQUIRED = 1;

        public static final int SUCCESS = 0;

        public static final int FAILURE = -1;

        public interface OnSetupFinishedListener {

            public void onSetupFinished(int resultCode);

        }

        private final Context mContext;

        private OnSetupFinishedListener mOnSetupFinishedListener;

        private boolean mIsDecidingOnDuplicates;

        private boolean mIsCloudOverwrites;

        /**
         * Checks for local and remote shows and uploads shows accordingly. If there are some shows
         * in the local database as well as on hexagon, will abort with result code indicating
         * required user intervention.
         */
        public HexagonSetupTask(Context context, OnSetupFinishedListener listener) {
            mContext = context.getApplicationContext();
            mOnSetupFinishedListener = listener;
            mIsDecidingOnDuplicates = false;
        }

        /**
         * Same as regular task, but if there are some shows in the local database as well as on
         * hexagon, does make a decision on uploading only missing or all shows without user
         * intervention based on the given flag.
         */
        public HexagonSetupTask(Context context, OnSetupFinishedListener listener,
                boolean isCloudOverwrites) {
            mContext = context.getApplicationContext();
            mOnSetupFinishedListener = listener;
            mIsDecidingOnDuplicates = true;
            mIsCloudOverwrites = isCloudOverwrites;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Integer doInBackground(Void... params) {
            // set setup incomplete flag
            HexagonSettings.setSetupIncomplete(mContext);

            // are there local shows?
            HashSet<Integer> showsLocal = ShowTools.getShowTvdbIdsAsSet(mContext);
            if (showsLocal == null || isCancelled()) {
                // that did go wrong
                return FAILURE;
            }
            if (showsLocal.size() == 0) {
                // no local shows, download any from Hexagon
                return SYNC_REQUIRED;
            }

            // are there shows on Hexagon?
            List<Show> showsRemote = ShowTools.Download.getRemoteShows(mContext);
            if (showsRemote == null || isCancelled()) {
                // that did go wrong
                return FAILURE;
            }
            if (showsRemote.size() == 0) {
                // no shows on Hexagon
                // upload all local shows
                return uploadAllShows();
            }

            // are any of the local shows already on Hexagon?
            int showsLocalCount = showsLocal.size();
            pruneShowsAlreadyOnHexagon(showsLocal, showsRemote);
            if (showsLocal.size() == showsLocalCount) {
                // none of the local shows are on Hexagon, upload all
                if (ShowTools.Upload.showsAll(mContext) == ShowTools.Upload.FAILURE) {
                    // that did go wrong
                    return FAILURE;
                } else {
                    // good, now download the other shows from hexagon
                    return SYNC_REQUIRED;
                }
            }

            // user intervention required on duplicates?
            if (mIsDecidingOnDuplicates) {
                // no, was instructed with decision
                if (mIsCloudOverwrites) {
                    if (showsLocal.size() == 0) {
                        // all shows are already on hexagon, done!
                        return SUCCESS;
                    }
                    // uploading only shows missing from the cloud
                    List<Show> showsMissing = ShowTools.Upload
                            .getSelectedLocalShowsAsList(mContext, showsLocal);
                    if (ShowTools.Upload.shows(mContext, showsMissing)
                            == ShowTools.Upload.FAILURE) {
                        // that did go wrong
                        return FAILURE;
                    } else {
                        // good, now sync the other shows from hexagon
                        return SYNC_REQUIRED;
                    }
                } else {
                    // upload all, overwriting duplicates in the cloud
                    return uploadAllShows();
                }
            }

            // user intervention required: upload only missing or overwrite all shows?
            return USER_ACTION_REQUIRED;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (mOnSetupFinishedListener != null) {
                mOnSetupFinishedListener.onSetupFinished(result);
            }
        }

        private void pruneShowsAlreadyOnHexagon(HashSet<Integer> showsLocal,
                List<Show> showsRemote) {
            for (Show show : showsRemote) {
                // try removing the id
                showsLocal.remove(show.getTvdbId());
            }
        }

        private Integer uploadAllShows() {
            if (ShowTools.Upload.showsAll(mContext) == ShowTools.Upload.FAILURE) {
                // that did go wrong
                return FAILURE;
            } else {
                // all good!
                return SUCCESS;
            }
        }
    }

    private HexagonSetupTask.OnSetupFinishedListener mSetupFinishedListener
            = new HexagonSetupTask.OnSetupFinishedListener() {
        @Override
        public void onSetupFinished(int resultCode) {
            switch (resultCode) {
                case HexagonSetupTask.USER_ACTION_REQUIRED: {
                    // task user to select which version of shows to keep
                    updateViewsStates(true);
                    break;
                }
                case HexagonSetupTask.SYNC_REQUIRED: {
                    // schedule full sync
                    SgSyncAdapter.requestSync(getActivity(), -1);
                    HexagonSettings.setSetupCompleted(getActivity());
                    updateViewsStates(false);
                    break;
                }
                case HexagonSetupTask.FAILURE: {
                    // TODO error toast
                    // show setup incomplete message
                    updateViewsStates(false);
                    break;
                }
                case HexagonSetupTask.SUCCESS:
                    // nothing further to do!
                    HexagonSettings.setSetupCompleted(getActivity());
                    updateViewsStates(false);
                    break;
            }

            setProgressLock(false); // allow new task
        }
    };

}
