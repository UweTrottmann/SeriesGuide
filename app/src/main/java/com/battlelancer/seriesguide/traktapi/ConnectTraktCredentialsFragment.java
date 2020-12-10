package com.battlelancer.seriesguide.traktapi;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.sync.SyncProgress;
import com.battlelancer.seriesguide.ui.SearchActivity;
import com.battlelancer.seriesguide.widgets.FeatureStatusView;
import com.battlelancer.seriesguide.widgets.SyncStatusView;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Interface to show trakt account features and connect or disconnect trakt.
 */
public class ConnectTraktCredentialsFragment extends Fragment {

    @BindView(R.id.textViewTraktAbout) TextView textViewAbout;
    @BindView(R.id.buttonTraktConnect) Button buttonAccount;
    @BindView(R.id.textViewTraktUser) TextView textViewUsername;
    @BindView(R.id.syncStatusTrakt) SyncStatusView syncStatusView;
    @BindView(R.id.featureStatusTraktCheckIn) FeatureStatusView featureStatusCheckIn;
    @BindView(R.id.featureStatusTraktSync) FeatureStatusView featureStatusSync;
    @BindView(R.id.featureStatusTraktSyncShows) FeatureStatusView featureStatusSyncShows;
    @BindView(R.id.featureStatusTraktSyncMovies) FeatureStatusView featureStatusSyncMovies;
    @BindView(R.id.buttonTraktLibrary) Button buttonLibrary;
    private Unbinder unbinder;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
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

        // library button
        buttonLibrary.setOnClickListener(v -> {
            // open search tab, will now have links to trakt lists
            startActivity(new Intent(getActivity(), SearchActivity.class).putExtra(
                    SearchActivity.EXTRA_DEFAULT_TAB, SearchActivity.TAB_POSITION_SEARCH));
        });

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

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEvent(SyncProgress.SyncEvent event) {
        syncStatusView.setProgress(event);
    }

    private void updateViews() {
        TraktCredentials traktCredentials = TraktCredentials.get(getActivity());
        boolean hasCredentials = traktCredentials.hasCredentials();
        if (hasCredentials) {
            String username = traktCredentials.getUsername();
            String displayName = traktCredentials.getDisplayName();
            if (!TextUtils.isEmpty(displayName)) {
                username += " (" + displayName + ")";
            }
            textViewUsername.setText(username);
            setAccountButtonState(false);
            buttonLibrary.setVisibility(View.VISIBLE);
        } else {
            textViewUsername.setText(null);
            setAccountButtonState(true);
            buttonLibrary.setVisibility(View.GONE);
        }
    }

    private void connect() {
        buttonAccount.setEnabled(false);
        startActivity(new Intent(getActivity(), TraktAuthActivity.class));
    }

    private void disconnect() {
        TraktCredentials.get(getActivity()).removeCredentials();
        updateViews();
    }

    private void setAccountButtonState(boolean connectEnabled) {
        buttonAccount.setEnabled(true);
        buttonAccount.setText(connectEnabled ? R.string.connect : R.string.disconnect);
        if (connectEnabled) {
            buttonAccount.setOnClickListener(v -> connect());
        } else {
            buttonAccount.setOnClickListener(v -> disconnect());
        }
    }
}
