
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

import com.actionbarsherlock.app.SherlockFragment;
import com.battlelancer.seriesguide.ui.AddActivity.AddPagerAdapter;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.R;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

/**
 * Tells about successful connection, allows to continue adding shows from users
 * trakt library or upload existing shows from SeriesGuide.
 */
public class ConnectTraktFinishedFragment extends SherlockFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.connect_trakt_finished_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // library button
        getView().findViewById(R.id.buttonShowLibrary).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // open library tab
                Intent i = new Intent(getActivity(), AddActivity.class);
                i.putExtra(AddActivity.InitBundle.DEFAULT_TAB, AddPagerAdapter.LIBRARY_TAB_POSITION);
                startActivity(i);
            }
        });

        // upload button
        getView().findViewById(R.id.buttonUploadShows).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), TraktSyncActivity.class);
                startActivity(i);
            }
        });

        // close button
        getView().findViewById(R.id.buttonClose).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });
    }
    
    @Override
    public void onStart() {
        super.onStart();
        Utils.trackView(getActivity(), "Connect Trakt Finished");
    }
}
