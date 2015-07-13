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

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.TraktResult;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.traktapi.TraktAuthActivity;
import com.battlelancer.seriesguide.util.ConnectTraktTask;
import com.battlelancer.seriesguide.util.Utils;

/**
 * Provides a user interface to connect or create a trakt account.
 */
public class ConnectTraktCredentialsFragment extends Fragment implements
        ConnectTraktTask.OnTaskFinishedListener {

    public static final String KEY_OAUTH_CODE = "auth-code";

    private ConnectTraktTask mTask;

    @Bind(R.id.buttonConnectTraktConnect) Button buttonConnect;
    @Bind(R.id.buttonConnectTraktDisconnect) Button buttonDisconnect;
    @Bind(R.id.textViewConnectTraktUsernameLabel) View usernameLabel;
    @Bind(R.id.textViewConnectTraktUsername) TextView username;
    @Bind(R.id.progressBarConnectTrakt) View progressBar;
    @Bind(R.id.textViewConnectTraktStatus) TextView status;

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
        ButterKnife.bind(this, v);

        // connect button
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
            }
        });

        // disconnect button
        buttonDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        updateViews();
        Utils.trackView(getActivity(), "Connect Trakt Credentials");
    }

    private void updateViews() {
        // unfinished task around?
        if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED) {
            // disable buttons, show status message
            setButtonStates(false, false);
            setStatus(true, R.string.waitplease);
            setUsernameViewsStates(false);
            return;
        }

        boolean hasCredentials = TraktCredentials.get(getActivity()).hasCredentials();
        if (hasCredentials) {
            username.setText(TraktCredentials.get(getActivity()).getUsername());
            setButtonStates(false, true);
            setUsernameViewsStates(true);
            setStatus(false, -1);
        } else {
            setButtonStates(true, false);
            setUsernameViewsStates(false);
            setStatus(false, R.string.trakt_connect_instructions);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != ConnectTraktActivity.OAUTH_CODE_REQUEST_CODE
                || resultCode != Activity.RESULT_OK
                || data == null
                || !data.hasExtra(KEY_OAUTH_CODE)) {
            return;
        }

        // fetch access token with given OAuth auth code
        String authCode = data.getStringExtra(KEY_OAUTH_CODE);
        mTask = new ConnectTraktTask(getActivity(), ConnectTraktCredentialsFragment.this);
        Utils.executeInOrder(mTask, authCode == null ? "" : authCode); // avoid method ambiguity

        updateViews();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        ButterKnife.unbind(this);
    }

    private void connect() {
        // disable buttons, show status message
        setButtonStates(false, false);
        setStatus(true, R.string.waitplease);

        // launch activity to authorize with trakt
        startActivityForResult(new Intent(getActivity(), TraktAuthActivity.class),
                ConnectTraktActivity.OAUTH_CODE_REQUEST_CODE);
    }

    private void disconnect() {
        TraktCredentials.get(getActivity()).removeCredentials();
        updateViews();
    }

    @Override
    public void onTaskFinished(int resultCode) {
        mTask = null;

        if (resultCode == TraktResult.SUCCESS) {
            // if we got here, looks like credentials were stored successfully

            // show further options after successful connection
            if (getFragmentManager() != null) {
                ConnectTraktFinishedFragment f = new ConnectTraktFinishedFragment();
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.content_frame, f);
                ft.commitAllowingStateLoss();
            }
            return;
        }

        // handle errors
        int errorResId;
        switch (resultCode) {
            case TraktResult.OFFLINE:
                errorResId = R.string.offline;
                break;
            case TraktResult.API_ERROR:
                errorResId = R.string.trakt_error_general;
                break;
            case TraktResult.AUTH_ERROR:
            case TraktResult.ERROR:
            default:
                errorResId = R.string.trakt_error_credentials;
                break;
        }
        setStatus(false, errorResId);
        setButtonStates(true, false);
    }

    private void setButtonStates(boolean connectEnabled, boolean disconnectEnabled) {
        // guard calls, as we might get called after the views were detached
        if (buttonConnect != null) {
            buttonConnect.setEnabled(connectEnabled);
        }
        if (buttonDisconnect != null) {
            buttonDisconnect.setEnabled(disconnectEnabled);
        }
    }

    /**
     * @param progressVisible If {@code true}, will show progress bar.
     * @param statusTextResourceId If {@code -1} will hide the status display, otherwise show the
     * given text ressource.
     */
    private void setStatus(boolean progressVisible, int statusTextResourceId) {
        // guard calls, as we might get called after the views were detached
        if (status == null || progressBar == null) {
            return;
        }
        progressBar.setVisibility(progressVisible ? View.VISIBLE : View.INVISIBLE);
        if (statusTextResourceId == -1) {
            status.setVisibility(View.INVISIBLE);
        } else {
            status.setText(statusTextResourceId);
            status.setVisibility(View.VISIBLE);
        }
    }

    private void setUsernameViewsStates(boolean visible) {
        if (username == null || usernameLabel == null) {
            return;
        }
        username.setVisibility(visible ? View.VISIBLE : View.GONE);
        usernameLabel.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
