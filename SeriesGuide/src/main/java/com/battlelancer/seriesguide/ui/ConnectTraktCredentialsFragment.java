package com.battlelancer.seriesguide.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.traktapi.TraktAuthActivity;

/**
 * Provides a user interface to connect or create a trakt account.
 */
public class ConnectTraktCredentialsFragment extends Fragment {

    private boolean isConnecting;

    private Unbinder unbinder;
    @BindView(R.id.buttonPositive) Button buttonConnect;
    @BindView(R.id.buttonNegative) Button buttonDisconnect;
    @BindView(R.id.textViewConnectTraktUsernameLabel) View textViewUsernameLabel;
    @BindView(R.id.textViewConnectTraktUsername) TextView textViewUsername;
    @BindView(R.id.textViewConnectTraktHexagonWarning) TextView textViewHexagonWarning;
    @BindView(R.id.progressBarConnectTrakt) View progressBar;
    @BindView(R.id.textViewConnectTraktStatus) TextView textViewStatus;

    public static ConnectTraktCredentialsFragment newInstance() {
        return new ConnectTraktCredentialsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_connect_trakt_credentials, container, false);
        unbinder = ButterKnife.bind(this, view);

        // connect button
        buttonConnect.setText(R.string.connect);
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
            }
        });

        // disconnect button
        buttonDisconnect.setText(R.string.disconnect);
        buttonDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });

        return view;
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
                textViewUsername.setText(username);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
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
        buttonConnect.setEnabled(connectEnabled);
        buttonDisconnect.setEnabled(disconnectEnabled);
    }

    /**
     * @param progressVisible If {@code true}, will show progress bar.
     * @param statusTextResourceId If {@code -1} will hide the status display, otherwise show the
     * given text ressource.
     */
    private void setStatus(boolean progressVisible, int statusTextResourceId) {
        isConnecting = progressVisible;
        progressBar.setVisibility(progressVisible ? View.VISIBLE : View.INVISIBLE);
        if (statusTextResourceId == -1) {
            textViewStatus.setVisibility(View.INVISIBLE);
        } else {
            textViewStatus.setText(statusTextResourceId);
            textViewStatus.setVisibility(View.VISIBLE);
        }
    }

    private void setUsernameViewsStates(boolean visible) {
        textViewUsername.setVisibility(visible ? View.VISIBLE : View.GONE);
        textViewUsernameLabel.setVisibility(visible ? View.VISIBLE : View.GONE);
        textViewHexagonWarning.setVisibility(
                visible && HexagonTools.isSignedIn(getContext()) ? View.VISIBLE : View.GONE);
    }
}
