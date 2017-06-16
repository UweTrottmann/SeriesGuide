package com.battlelancer.seriesguide.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.sync.SyncProgress;
import com.battlelancer.seriesguide.traktapi.TraktAuthActivity;
import com.battlelancer.seriesguide.widgets.FeatureStatusView;
import com.battlelancer.seriesguide.widgets.SyncStatusView;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Interface to show trakt account features and connect or disconnect trakt.
 */
public class ConnectTraktCredentialsFragment extends Fragment {

    private static final String STATE_IS_CONNECTING = "is_connecting";

    @BindView(R.id.textViewTraktAbout) TextView textViewAbout;
    @BindView(R.id.buttonTraktConnect) Button buttonConnect;
    @BindView(R.id.textViewTraktUser) TextView textViewUsername;
    @BindView(R.id.syncStatusTrakt) SyncStatusView syncStatusView;
    @BindView(R.id.featureStatusTraktCheckIn) FeatureStatusView featureStatusCheckIn;
    @BindView(R.id.featureStatusTraktSync) FeatureStatusView featureStatusSync;
    @BindView(R.id.featureStatusTraktSyncShows) FeatureStatusView featureStatusSyncShows;
    @BindView(R.id.featureStatusTraktSyncMovies) FeatureStatusView featureStatusSyncMovies;
    @BindView(R.id.textViewConnectTraktHexagonWarning) TextView textViewHexagonWarning;
    private Unbinder unbinder;

    private boolean isConnecting;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            isConnecting = savedInstanceState.getBoolean(STATE_IS_CONNECTING, false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_connect_trakt_credentials, container, false);
        unbinder = ButterKnife.bind(this, view);

        // make learn more link clickable
        textViewAbout.setMovementMethod(LinkMovementMethod.getInstance());

        boolean hexagonEnabled = HexagonSettings.isEnabled(getContext());
        featureStatusCheckIn.setFeatureEnabled(!hexagonEnabled);
        featureStatusSync.setFeatureEnabled(!hexagonEnabled);
        featureStatusSyncShows.setFeatureEnabled(!hexagonEnabled);
        featureStatusSyncMovies.setFeatureEnabled(!hexagonEnabled);
        textViewHexagonWarning.setVisibility(hexagonEnabled ? View.VISIBLE : View.GONE);

        syncStatusView.setVisibility(View.GONE);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        updateViews();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_IS_CONNECTING, isConnecting);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEvent(SyncProgress.SyncEvent event) {
        syncStatusView.setProgress(event);
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
                setConnectButtonState(false);
                setIsConnecting(false);
            }
        } else {
            isConnecting = false;
            textViewUsername.setText(null);
            setConnectButtonState(true);
            setIsConnecting(false);
        }
    }

    private void connect() {
        setIsConnecting(true);
        startActivity(new Intent(getActivity(), TraktAuthActivity.class));
    }

    private void disconnect() {
        TraktCredentials.get(getActivity()).removeCredentials();
        updateViews();
    }

    private void setConnectButtonState(boolean connectEnabled) {
        buttonConnect.setText(connectEnabled ? R.string.connect : R.string.disconnect);
        if (connectEnabled) {
            buttonConnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    connect();
                }
            });
        } else {
            buttonConnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    disconnect();
                }
            });
        }
    }

    private void setIsConnecting(boolean isConnecting) {
        this.isConnecting = isConnecting;
        buttonConnect.setEnabled(!isConnecting);
    }
}
