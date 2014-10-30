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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.NetworkResult;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.util.ConnectTraktTask;
import com.battlelancer.seriesguide.util.Utils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides a user interface to connect or create a trakt account.
 */
public class ConnectTraktCredentialsFragment extends Fragment implements
        ConnectTraktTask.OnTaskFinishedListener {

    private ConnectTraktTask mTask;

    @InjectView(R.id.connectbutton) Button mButtonConnect;

    @InjectView(R.id.disconnectbutton) Button mButtonDisconnect;

    @InjectView(R.id.username) EditText mEditTextUsername;

    @InjectView(R.id.password) EditText mEditTextPassword;

    @InjectView(R.id.mailviews) View mViewsNewAccount;

    @InjectView(R.id.checkNewAccount) CheckBox mCheckBoxNewAccount;

    @InjectView(R.id.email) AutoCompleteTextView mEmailAutoCompleteView;

    @InjectView(R.id.status) TextView mTextViewStatus;

    @InjectView(R.id.progressbar) View mProgressBar;

    @InjectView(R.id.progress) View mStatusView;

    public static ConnectTraktCredentialsFragment newInstance() {
        return new ConnectTraktCredentialsFragment();
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
        View v = inflater.inflate(R.layout.fragment_connect_trakt_credentials, container, false);
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

        // enable e-mail completion via device accounts
        final Account[] accounts = AccountManager.get(getActivity()).getAccounts();
        final Set<String> emailSet = new HashSet<>();
        for (Account account : accounts) {
            if (Patterns.EMAIL_ADDRESS.matcher(account.name).matches()) {
                emailSet.add(account.name);
            }
        }
        List<String> emails = new ArrayList<>(emailSet);
        mEmailAutoCompleteView.setAdapter(
                new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line,
                        emails));

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
                    Editable editableEmail = mEmailAutoCompleteView.getText();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        ButterKnife.reset(this);
    }

    private void setButtonStates(boolean connectEnabled, boolean disconnectEnabled) {
        // guard calls, as we might get called after the views were detached
        if (mButtonConnect != null) {
            mButtonConnect.setEnabled(connectEnabled);
        }
        if (mButtonDisconnect != null) {
            mButtonDisconnect.setEnabled(disconnectEnabled);
        }
    }

    private void setStatus(boolean visible, boolean inProgress, int statusTextResourceId) {
        // guard calls, as we might get called after the views were detached
        if (mStatusView != null) {
            if (!visible) {
                mStatusView.setVisibility(View.GONE);
                return;
            }
            mStatusView.setVisibility(View.VISIBLE);
        }
        if (mProgressBar != null) {
            mProgressBar.setVisibility(inProgress ? View.VISIBLE : View.GONE);
        }
        if (mTextViewStatus != null) {
            mTextViewStatus.setText(statusTextResourceId);
        }
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

        // show further options after successful connection
        if (getFragmentManager() != null) {
            ConnectTraktFinishedFragment f = new ConnectTraktFinishedFragment();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.content_frame, f);
            ft.commitAllowingStateLoss();
        }
    }
}
