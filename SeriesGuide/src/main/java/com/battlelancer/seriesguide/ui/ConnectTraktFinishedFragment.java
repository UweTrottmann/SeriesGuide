
package com.battlelancer.seriesguide.ui;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.databinding.FragmentConnectTraktFinishedBinding;

/**
 * Tells about successful connection, allows to continue adding shows from users trakt library.
 */
public class ConnectTraktFinishedFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        FragmentConnectTraktFinishedBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.fragment_connect_trakt_finished, container, false);

        // hide sync message if hexagon is connected (so trakt sync is disabled)
        if (HexagonTools.isSignedIn(getActivity())) {
            binding.textViewConnectTraktFinished.setVisibility(View.GONE);
        }

        // library button
        binding.buttonShowLibrary.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // open library tab
                startActivity(new Intent(getActivity(), SearchActivity.class).putExtra(
                        SearchActivity.EXTRA_DEFAULT_TAB, SearchActivity.TAB_POSITION_WATCHED));
                getActivity().finish();
            }
        });

        // close button
        binding.buttons.buttonPositive.setText(R.string.dismiss);
        binding.buttons.buttonPositive.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });
        binding.buttons.buttonNegative.setVisibility(View.GONE);

        return binding.getRoot();
    }
}
