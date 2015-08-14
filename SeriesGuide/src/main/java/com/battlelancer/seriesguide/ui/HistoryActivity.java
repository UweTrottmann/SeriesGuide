/*
 * Copyright 2015 Uwe Trottmann
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
import android.support.annotation.IntDef;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.ui.dialogs.AddShowDialogFragment;
import com.battlelancer.seriesguide.ui.streams.UserEpisodeStreamFragment;
import com.battlelancer.seriesguide.ui.streams.UserMovieStreamFragment;
import com.battlelancer.seriesguide.util.TaskManager;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Displays history of watched episodes or movies.
 */
public class HistoryActivity extends BaseActivity implements
        AddShowDialogFragment.OnAddShowListener {

    public static final int EPISODES_LOADER_ID = 100;
    public static final int MOVIES_LOADER_ID = 101;

    public static final int DISPLAY_EPISODE_HISTORY = 0;
    public static final int DISPLAY_MOVIE_HISTORY = 1;

    public interface InitBundle {
        String HISTORY_TYPE = BuildConfig.APPLICATION_ID + ".historytype";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlepane);
        setupActionBar();

        if (savedInstanceState == null) {
            int historyType = getIntent().getIntExtra(InitBundle.HISTORY_TYPE, -1);
            Fragment f;
            if (historyType == DISPLAY_EPISODE_HISTORY) {
                f = new UserEpisodeStreamFragment();
            } else if (historyType == DISPLAY_MOVIE_HISTORY) {
                f = new UserMovieStreamFragment();
            } else {
                throw new IllegalArgumentException(
                        "Did not specify a valid HistoryType in the launch intent.");
            }
            f.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.content_frame, f)
                    .commit();
        }
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.user_stream);
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

    /**
     * Called if the user adds a show from a trakt stream fragment.
     */
    @Override
    public void onAddShow(SearchResult show) {
        TaskManager.getInstance(this).performAddTask(show);
    }
}
