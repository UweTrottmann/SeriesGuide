package com.battlelancer.seriesguide.traktapi;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.databinding.FragmentConnectTraktCredentialsBinding;
import com.battlelancer.seriesguide.sync.SyncProgress;
import com.battlelancer.seriesguide.ui.SearchActivity;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Interface to show trakt account features and connect or disconnect trakt.
 */
public class ConnectTraktCredentialsFragment extends Fragment {

    private FragmentConnectTraktCredentialsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        binding = FragmentConnectTraktCredentialsBinding.inflate(inflater, container, false);

        // make learn more link clickable
        binding.textViewTraktAbout.setMovementMethod(LinkMovementMethod.getInstance());

        boolean hexagonEnabled = HexagonSettings.isEnabled(requireContext());
        binding.featureStatusTraktCheckIn.setFeatureEnabled(!hexagonEnabled);
        binding.featureStatusTraktSync.setFeatureEnabled(!hexagonEnabled);
        binding.featureStatusTraktSyncShows.setFeatureEnabled(!hexagonEnabled);
        binding.featureStatusTraktSyncMovies.setFeatureEnabled(!hexagonEnabled);

        // library button
        binding.buttonTraktLibrary.setOnClickListener(v -> {
            // open search tab, will now have links to trakt lists
            startActivity(SearchActivity.newIntent(requireContext()));
        });

        binding.syncStatusTrakt.setVisibility(View.GONE);

        return binding.getRoot();
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
        binding = null;
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEvent(SyncProgress.SyncEvent event) {
        binding.syncStatusTrakt.setProgress(event);
    }

    private void updateViews() {
        TraktCredentials traktCredentials = TraktCredentials.get(requireContext());
        boolean hasCredentials = traktCredentials.hasCredentials();
        if (hasCredentials) {
            String username = traktCredentials.getUsername();
            String displayName = traktCredentials.getDisplayName();
            if (!TextUtils.isEmpty(displayName)) {
                username += " (" + displayName + ")";
            }
            binding.textViewTraktUser.setText(username);
            setAccountButtonState(false);
            binding.buttonTraktLibrary.setVisibility(View.VISIBLE);
        } else {
            binding.textViewTraktUser.setText(null);
            setAccountButtonState(true);
            binding.buttonTraktLibrary.setVisibility(View.GONE);
        }
    }

    private void connect() {
        binding.buttonTraktConnect.setEnabled(false);
        startActivity(new Intent(getActivity(), TraktAuthActivity.class));
    }

    private void disconnect() {
        TraktCredentials.get(requireContext()).removeCredentials();
        updateViews();
    }

    private void setAccountButtonState(boolean connectEnabled) {
        Button buttonAccount = binding.buttonTraktConnect;
        buttonAccount.setEnabled(true);
        buttonAccount.setText(connectEnabled ? R.string.connect : R.string.disconnect);
        if (connectEnabled) {
            buttonAccount.setOnClickListener(v -> connect());
        } else {
            buttonAccount.setOnClickListener(v -> disconnect());
        }
    }
}
