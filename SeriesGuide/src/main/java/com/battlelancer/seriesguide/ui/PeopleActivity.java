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
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import com.battlelancer.seriesguide.R;

public class PeopleActivity extends BaseActivity implements PeopleFragment.OnShowPersonListener {

    private boolean mTwoPane;

    public interface InitBundle {
        String MEDIA_TYPE = "media_title";
        String ITEM_TMDB_ID = "item_tmdb_id";
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

        if (findViewById(R.id.containerPeoplePerson) != null) {
            mTwoPane = true;
        }

        if (savedInstanceState == null) {
            // check if we should directly show a person
            int personTmdbId = getIntent().getIntExtra(PersonFragment.InitBundle.PERSON_TMDB_ID, -1);
            if (personTmdbId != -1) {
                showPerson(null, personTmdbId);

                // if this is not a dual pane layout, remove ourselves from back stack
                if (!mTwoPane) {
                    finish();
                    return;
                }
            }

            PeopleFragment f = new PeopleFragment();
            f.setArguments(getIntent().getExtras());

            // in two-pane mode, list items should be activated when touched
            if (mTwoPane) {
                f.setActivateOnItemClick(true);
            }

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.containerPeople, f, "people-list")
                    .commit();
        }
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
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
    public void showPerson(View view, int tmdbId) {
        if (mTwoPane) {
            // show inline
            PersonFragment f = PersonFragment.newInstance(tmdbId);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.containerPeoplePerson, f)
                    .commit();
        } else {
            // start new activity
            Intent i = new Intent(this, PersonActivity.class);
            i.putExtra(PersonFragment.InitBundle.PERSON_TMDB_ID, tmdbId);

            if (view != null) {
                ActivityCompat.startActivity(this, i,
                        ActivityOptionsCompat
                                .makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight())
                                .toBundle()
                );
            } else {
                startActivity(i);
            }
        }
    }
}
