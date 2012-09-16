/*
 * Copyright 2011 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.ui;

import android.os.Bundle;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.util.ShareUtils.ShareItems;

public class TraktShoutsActivity extends BaseActivity {

    public static Bundle createInitBundle(int showTvdbid, int seasonNumber, int episodeNumber,
            String title) {
        Bundle extras = new Bundle();
        extras.putInt(ShareItems.TVDBID, showTvdbid);
        extras.putInt(ShareItems.SEASON, seasonNumber);
        extras.putInt(ShareItems.EPISODE, episodeNumber);
        extras.putString(ShareItems.SHARESTRING, title);
        return extras;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlepane_empty);

        Bundle args = getIntent().getExtras();
        String title = args.getString(ShareItems.SHARESTRING);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        // embed the shouts fragment dialog
        SherlockDialogFragment newFragment;
        int tvdbId = args.getInt(ShareItems.TVDBID);
        int episode = args.getInt(ShareItems.EPISODE);
        if (episode == 0) {
            newFragment = TraktShoutsFragment.newInstance(title, tvdbId);
        } else {
            int season = args.getInt(ShareItems.SEASON);
            newFragment = TraktShoutsFragment.newInstance(title, tvdbId, season, episode);
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.root_container, newFragment)
                .commit();
    }
}
