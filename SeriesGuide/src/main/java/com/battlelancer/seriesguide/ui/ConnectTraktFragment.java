
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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.method.LinkMovementMethod;
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
 * Tells about trakt and how it integrates with SeriesGuide, allows to proceed to entering
 * credentials step.
 */
public class ConnectTraktFragment extends Fragment {

    @BindView(R.id.textViewAbout) TextView aboutTextView;
    @BindView(R.id.textViewTraktInfoHexagonWarning) TextView hexagonWarningTextView;
    @BindView(R.id.buttonNegative) Button cancelButton;
    @BindView(R.id.buttonPositive) Button connectButton;
    private Unbinder unbinder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_connect_trakt_info, container, false);
        unbinder = ButterKnife.bind(this, v);

        connectButton.setText(R.string.connect_trakt);
        cancelButton.setText(android.R.string.cancel);

        // wire up buttons
        connectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                replaceWithCredentialsFragment();
            }
        });
        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        // make learn more link clickable
        aboutTextView.setMovementMethod(LinkMovementMethod.getInstance());

        // show hexagon + trakt conflict warning
        hexagonWarningTextView.setVisibility(
                HexagonTools.isSignedIn(getActivity()) ? View.VISIBLE : View.GONE);

        return v;
    }

    private void replaceWithCredentialsFragment() {
        ConnectTraktCredentialsFragment f = ConnectTraktCredentialsFragment.newInstance();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.content_frame, f);
        ft.commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
    }
}
