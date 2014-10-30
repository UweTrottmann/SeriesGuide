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
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import com.battlelancer.seriesguide.R;

public class TraktShoutsActivity extends BaseActivity {

    public static final int LOADER_ID_COMMENTS = 100;

    public static Bundle createInitBundleEpisode(int showTvdbid, int seasonNumber,
            int episodeNumber, String title) {
        Bundle extras = new Bundle();
        extras.putInt(TraktShoutsFragment.InitBundle.SHOW_TVDB_ID, showTvdbid);
        extras.putInt(TraktShoutsFragment.InitBundle.SEASON_NUMBER, seasonNumber);
        extras.putInt(TraktShoutsFragment.InitBundle.EPISODE_NUMBER, episodeNumber);
        extras.putString(InitBundle.TITLE, title);
        return extras;
    }

    public static Bundle createInitBundleShow(String title, int tvdbId) {
        Bundle extras = new Bundle();
        extras.putInt(TraktShoutsFragment.InitBundle.SHOW_TVDB_ID, tvdbId);
        extras.putString(InitBundle.TITLE, title);
        return extras;
    }

    public static Bundle createInitBundleMovie(String title, int tmdbId) {
        Bundle extras = new Bundle();
        extras.putInt(TraktShoutsFragment.InitBundle.MOVIE_TMDB_ID, tmdbId);
        extras.putString(InitBundle.TITLE, title);
        return extras;
    }

    private interface InitBundle {
        String TITLE = "title";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlepane);
        setupActionBar();

        if (savedInstanceState == null) {
            // embed the shouts fragment dialog
            Fragment f;
            Bundle args = getIntent().getExtras();
            int showTvdbId = args.getInt(TraktShoutsFragment.InitBundle.SHOW_TVDB_ID);
            int episode = args.getInt(TraktShoutsFragment.InitBundle.EPISODE_NUMBER);
            if (showTvdbId == 0) {
                int tmdbId = args.getInt(TraktShoutsFragment.InitBundle.MOVIE_TMDB_ID);
                f = TraktShoutsFragment.newInstanceMovie(tmdbId);
            } else if (episode == 0) {
                f = TraktShoutsFragment.newInstanceShow(showTvdbId);
            } else {
                int season = args.getInt(TraktShoutsFragment.InitBundle.SEASON_NUMBER);
                f = TraktShoutsFragment
                        .newInstanceEpisode(showTvdbId, season, episode);
            }
            getSupportFragmentManager().beginTransaction().add(R.id.content_frame, f)
                    .commit();
        }
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.comments);
        actionBar.setSubtitle(getIntent().getExtras().getString(InitBundle.TITLE));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
