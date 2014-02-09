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

package com.battlelancer.seriesguide.ui;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.actionbarsherlock.app.SherlockFragment;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.NetworkResult;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.util.ConnectTraktTask;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;

/**
 * Provides a user interface to connect or create a trakt account.
 */
public class ConnectTraktCredentialsFragment extends SherlockFragment implements
        ConnectTraktTask.OnTaskFinishedListener {

    private boolean mIsForwardingGivenTask;

    private ConnectTraktTask mTask;

    @InjectView(R.id.connectbutton) Button mButtonConnect;

    @InjectView(R.id.disconnectbutton) Button mButtonDisconnect;

    @InjectView(R.id.username) EditText mEditTextUsername;

    @InjectView(R.id.password) EditText mEditTextPassword;

    @InjectView(R.id.mailviews) View mViewsNewAccount;

    @InjectView(R.id.checkNewAccount) CheckBox mCheckBoxNewAccount;

    @InjectView(R.id.email) EditText mEditTextEmail;

    @InjectView(R.id.status) TextView mTextViewStatus;

    @InjectView(R.id.progressbar) View mProgressBar;

    @InjectView(R.id.progress) View mStatusView;

    public static ConnectTraktCredentialsFragment newInstance(Bundle traktData) {
        ConnectTraktCredentialsFragment f = new ConnectTraktCredentialsFragment();
        f.setArguments(traktData);
        f.mIsForwardingGivenTask = true;
        return f;
    }

    public static ConnectTraktCredentialsFragment newInstance() {
        ConnectTraktCredentialsFragment f = new ConnectTraktCredentialsFragment();
        f.mIsForwardingGivenTask = false;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Try to keep the fragment around on config changes so the credentials task
         * does not have to be finished.
         */
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.trakt_credentials_dialog, container, false);
        ButterKnife.inject(this, v);

        // status strip
        mStatusView.setVisibility(View.GONE);

        // new account toggle
        mViewsNewAccount.setVisibility(View.GONE);
        mCheckBoxNewAccount.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mViewsNewAccount.setVisibility(View.VISIBLE);
                } else {
                    mViewsNewAccount.setVisibility(View.GONE);
                }
            }
        });

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setupViews();

        // unfinished task around?
        if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED) {
            // disable buttons, show status message
            setButtonStates(false, false);
            setStatus(true, true, R.string.waitplease);
        }

        // connect button
        mButtonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // disable buttons, show status message
                setButtonStates(false, false);
                setStatus(true, true, R.string.waitplease);

                // get username and password
                Editable editableUsername = mEditTextUsername.getText();
                String username = editableUsername != null ?
                        editableUsername.toString().trim() : null;
                Editable editablePassword = mEditTextPassword.getText();
                String password = editablePassword != null ?
                        editablePassword.toString().trim() : null;

                // get email
                String email = null;
                if (mCheckBoxNewAccount.isChecked()) {
                    Editable editableEmail = mEditTextEmail.getText();
                    email = editableEmail != null ? editableEmail.toString().trim() : null;
                }

                mTask = new ConnectTraktTask(getActivity().getApplicationContext(),
                        ConnectTraktCredentialsFragment.this);
                mTask.execute(username, password, email);
            }
        });

        // disconnect button
        mButtonDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO do this async
                TraktCredentials.get(getActivity()).removeCredentials();
                setupViews();
            }
        });

    }

    private void setupViews() {
        boolean hasCredentials = TraktCredentials.get(getActivity()).hasCredentials();

        // buttons
        if (hasCredentials) {
            setButtonStates(false, true);
            mEditTextUsername.setText(TraktCredentials.get(getActivity()).getUsername());
        } else {
            setButtonStates(true, false);
        }

        // username and password
        mEditTextUsername.setEnabled(!hasCredentials);
        mEditTextPassword.setEnabled(!hasCredentials);
        mEditTextPassword.setText(hasCredentials ? "********" : null); // fake password

        // new account check box
        mCheckBoxNewAccount.setEnabled(!hasCredentials);
    }

    @Override
    public void onStart() {
        super.onStart();
        Utils.trackView(getActivity(), "Connect Trakt Credentials");
    }

    @Override public void onDestroyView() {
        super.onDestroyView();

        ButterKnife.reset(this);
    }

    private void setButtonStates(boolean connectEnabled, boolean disconnectEnabled) {
        mButtonConnect.setEnabled(connectEnabled);
        mButtonDisconnect.setEnabled(disconnectEnabled);
    }

    private void setStatus(boolean visible, boolean inProgress, int statusTextResourceId) {
        if (!visible) {
            mStatusView.setVisibility(View.GONE);
            return;
        }
        mStatusView.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(inProgress ? View.VISIBLE : View.GONE);
        mTextViewStatus.setText(statusTextResourceId);
    }

    @Override
    public void onTaskFinished(int resultCode) {
        mTask = null;

        if (resultCode == NetworkResult.OFFLINE) {
            setStatus(true, false, R.string.offline);
            setButtonStates(true, false);
            return;
        }

        if (resultCode == NetworkResult.ERROR) {
            setStatus(true, false, R.string.trakt_error_credentials);
            setButtonStates(true, false);
            return;
        }

        // if we got here, looks like credentials were stored successfully
        if (mIsForwardingGivenTask) {
            // relaunch the trakt task which called us
            final Bundle args = getArguments();
            AndroidUtils.executeAsyncTask(new TraktTask(getActivity(), args));
            getActivity().finish();
        } else {
            // show download/upload options after successful connection
            ConnectTraktFinishedFragment f = new ConnectTraktFinishedFragment();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(android.R.id.content, f);
            ft.commit();
        }
    }

}
