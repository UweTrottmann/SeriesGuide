
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

    @BindView(R.id.textViewConnectTraktFinished) TextView syncMessageTextView;
    @BindView(R.id.buttonShowLibrary) Button libraryButton;
    @BindView(R.id.buttonPositive) Button closeButton;
    @BindView(R.id.buttonNegative) Button hiddenButton;
    private Unbinder unbinder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_connect_trakt_finished, container, false);
        unbinder = ButterKnife.bind(this, v);

        // hide sync message if hexagon is connected (so trakt sync is disabled)
        if (HexagonTools.isSignedIn(getActivity())) {
            syncMessageTextView.setVisibility(View.GONE);
        }

        // library button
        libraryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // open library tab
                startActivity(new Intent(getActivity(), SearchActivity.class).putExtra(
                        SearchActivity.EXTRA_DEFAULT_TAB, SearchActivity.WATCHED_TAB_POSITION));
                getActivity().finish();
            }
        });

        // close button
        closeButton.setText(R.string.dismiss);
        closeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });
        hiddenButton.setVisibility(View.GONE);

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
    }
}
