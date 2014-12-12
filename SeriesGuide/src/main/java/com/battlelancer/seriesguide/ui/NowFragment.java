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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.NowAdapter;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersGridView;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows recently watched episodes, today's releases and recent episodes from friends (if connected
 * to trakt).
 */
public class NowFragment extends Fragment {

    @InjectView(R.id.gridViewNow) StickyGridHeadersGridView gridView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_now, container, false);
        ButterKnife.inject(this, v);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        List<NowAdapter.NowItem> nowItems = new ArrayList<>();
        nowItems.add(new NowAdapter.NowItem("Homeland", "", "", NowAdapter.NowType.RECENTLY_WATCHED));
        nowItems.add(new NowAdapter.NowItem("Homeland2", "", "", NowAdapter.NowType.RELEASED_TODAY));
        nowItems.add(new NowAdapter.NowItem("Homeland3", "", "", NowAdapter.NowType.RELEASED_TODAY));

        NowAdapter adapter = new NowAdapter(getActivity());
        adapter.addAll(nowItems);

        gridView.setAdapter(adapter);
        gridView.setAreHeadersSticky(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        ButterKnife.reset(this);
    }
}
