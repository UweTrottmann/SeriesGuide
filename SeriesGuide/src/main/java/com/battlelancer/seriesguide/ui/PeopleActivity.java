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

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.MenuItem;
import com.battlelancer.seriesguide.R;

public class PeopleActivity extends BaseActivity implements PeopleFragment.OnShowPersonListener {

    public interface InitBundle {
        String MEDIA_TYPE = "media_title";
        String TMDB_ID = "tmdb_id";
        String PEOPLE_TYPE = "people_type";
    }

    public enum MediaType {
        SHOW("SHOW"),
        MOVIE("MOVIE");

        private final String mValue;

        private MediaType(String value) {
            mValue = value;
        }

        @Override
        public String toString() {
            return mValue;
        }
    }

    public enum PeopleType {
        CAST("CAST"),
        CREW("CREW");

        private final String mValue;

        private PeopleType(String value) {
            mValue = value;
        }

        @Override
        public String toString() {
            return mValue;
        }
    }

    public static final int PEOPLE_LOADER_ID = 100;
    public static final int PERSON_LOADER_ID = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_people);

        setupActionBar();

        if (savedInstanceState == null) {
            Fragment f = new PeopleFragment();
            f.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, f)
                    .commit();
        }
    }

    private void setupActionBar() {
        final ActionBar actionBar = getActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        PeopleType peopleType = PeopleType.valueOf(
                getIntent().getStringExtra(InitBundle.PEOPLE_TYPE));
        actionBar.setTitle(
                peopleType == PeopleType.CAST ? R.string.movie_cast : R.string.movie_crew);
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

    @Override
    public void showPerson(int tmdbId) {
        PersonFragment f = PersonFragment.newInstance(tmdbId);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, f)
                .addToBackStack(null)
                .commit();
    }
}
