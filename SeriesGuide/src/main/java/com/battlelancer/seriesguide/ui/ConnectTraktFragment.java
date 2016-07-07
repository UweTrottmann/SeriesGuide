
/*
 * Copyright 2013 Uwe Trottmann
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

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.databinding.FragmentConnectTraktInfoBinding;

/**
 * Tells about trakt and how it integrates with SeriesGuide, allows to proceed to entering
 * credentials step.
 */
public class ConnectTraktFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        FragmentConnectTraktInfoBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.fragment_connect_trakt_info, container, false);

        binding.buttons.buttonPositive.setText(R.string.connect_trakt);
        binding.buttons.buttonNegative.setText(android.R.string.cancel);

        // wire up buttons
        binding.buttons.buttonPositive.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                replaceWithCredentialsFragment();
            }
        });
        binding.buttons.buttonNegative.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        // make learn more link clickable
        binding.textViewAbout.setMovementMethod(LinkMovementMethod.getInstance());

        // show hexagon + trakt conflict warning
        binding.textViewTraktInfoHexagonWarning.setVisibility(
                HexagonTools.isSignedIn(getActivity()) ? View.VISIBLE : View.GONE);

        return binding.getRoot();
    }

    private void replaceWithCredentialsFragment() {
        ConnectTraktCredentialsFragment f = ConnectTraktCredentialsFragment.newInstance();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.content_frame, f);
        ft.commit();
    }
}
