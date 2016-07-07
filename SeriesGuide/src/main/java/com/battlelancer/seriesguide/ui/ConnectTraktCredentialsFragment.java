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

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.databinding.FragmentConnectTraktCredentialsBinding;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.traktapi.TraktAuthActivity;

/**
 * Provides a user interface to connect or create a trakt account.
 */
public class ConnectTraktCredentialsFragment extends Fragment {

    private FragmentConnectTraktCredentialsBinding binding;

    private boolean isConnecting;

    public static ConnectTraktCredentialsFragment newInstance() {
        return new ConnectTraktCredentialsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater,
                R.layout.fragment_connect_trakt_credentials, container, false);

        // connect button
        binding.buttons.buttonPositive.setText(R.string.connect);
        binding.buttons.buttonPositive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
            }
        });

        // disconnect button
        binding.buttons.buttonNegative.setText(R.string.disconnect);
        binding.buttons.buttonNegative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();

        updateViews();
    }

    private void updateViews() {
        TraktCredentials traktCredentials = TraktCredentials.get(getActivity());
        boolean hasCredentials = traktCredentials.hasCredentials();
        if (hasCredentials) {
            if (isConnecting) {
                // show further options after successful connection
                ConnectTraktFinishedFragment f = new ConnectTraktFinishedFragment();
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.content_frame, f);
                ft.commitAllowingStateLoss();
            } else {
                String username = traktCredentials.getUsername();
                String displayName = traktCredentials.getDisplayName();
                if (!TextUtils.isEmpty(displayName)) {
                    username += " (" + displayName + ")";
                }
                binding.textViewConnectTraktUsername.setText(username);
                setButtonStates(false, true);
                setUsernameViewsStates(true);
                setStatus(false, -1);
            }
        } else {
            isConnecting = false;
            setButtonStates(true, false);
            setUsernameViewsStates(false);
            setStatus(false, R.string.trakt_connect_instructions);
        }
    }

    private void connect() {
        // disable buttons, show status message
        setButtonStates(false, false);
        setStatus(true, R.string.waitplease);

        // launch activity to authorize with trakt
        startActivity(new Intent(getActivity(), TraktAuthActivity.class));
    }

    private void disconnect() {
        TraktCredentials.get(getActivity()).removeCredentials();
        updateViews();
    }

    private void setButtonStates(boolean connectEnabled, boolean disconnectEnabled) {
        binding.buttons.buttonPositive.setEnabled(connectEnabled);
        binding.buttons.buttonNegative.setEnabled(disconnectEnabled);
    }

    /**
     * @param progressVisible If {@code true}, will show progress bar.
     * @param statusTextResourceId If {@code -1} will hide the status display, otherwise show the
     * given text ressource.
     */
    private void setStatus(boolean progressVisible, int statusTextResourceId) {
        isConnecting = progressVisible;
        binding.progressBarConnectTrakt.setVisibility(
                progressVisible ? View.VISIBLE : View.INVISIBLE);
        if (statusTextResourceId == -1) {
            binding.textViewConnectTraktStatus.setVisibility(View.INVISIBLE);
        } else {
            binding.textViewConnectTraktStatus.setText(statusTextResourceId);
            binding.textViewConnectTraktStatus.setVisibility(View.VISIBLE);
        }
    }

    private void setUsernameViewsStates(boolean visible) {
        binding.textViewConnectTraktUsername.setVisibility(visible ? View.VISIBLE : View.GONE);
        binding.textViewConnectTraktUsernameLabel.setVisibility(visible ? View.VISIBLE : View.GONE);
        binding.textViewConnectTraktHexagonWarning.setVisibility(
                visible && HexagonTools.isSignedIn(getContext()) ? View.VISIBLE : View.GONE);
    }
}
