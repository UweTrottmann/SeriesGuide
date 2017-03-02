
package com.battlelancer.seriesguide.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.HexagonTools;

/**
 * Tells about successful connection, allows to continue adding shows from users trakt library.
 */
public class ConnectTraktFinishedFragment extends Fragment {

    private Unbinder unbinder;
    @BindView(R.id.buttonPositive) Button buttonClose;
    @BindView(R.id.buttonNegative) Button buttonHidden;
    @BindView(R.id.textViewConnectTraktFinished) TextView textViewSyncMessage;
    @BindView(R.id.buttonShowLibrary) Button buttonShowLibrary;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_connect_trakt_finished, container, false);
        unbinder = ButterKnife.bind(this, v);

        // hide sync message if hexagon is connected (so trakt sync is disabled)
        if (HexagonTools.isConfigured(getActivity())) {
            textViewSyncMessage.setVisibility(View.GONE);
        }

        // library button
        buttonShowLibrary.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // open library tab
                startActivity(new Intent(getActivity(), SearchActivity.class).putExtra(
                        SearchActivity.EXTRA_DEFAULT_TAB, SearchActivity.TAB_POSITION_WATCHED));
                getActivity().finish();
            }
        });

        // close button
        buttonClose.setText(R.string.dismiss);
        buttonClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });
        buttonHidden.setVisibility(View.GONE);

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
    }
}
