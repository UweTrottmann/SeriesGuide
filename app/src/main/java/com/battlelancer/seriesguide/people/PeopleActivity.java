package com.battlelancer.seriesguide.people;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.databinding.ActivityPeopleBinding;
import com.battlelancer.seriesguide.ui.BaseActivity;

public class PeopleActivity extends BaseActivity implements PeopleFragment.OnShowPersonListener {

    private boolean isTwoPane;

    interface InitBundle {
        String MEDIA_TYPE = "media_title";
        String ITEM_TMDB_ID = "item_tmdb_id";
        String PEOPLE_TYPE = "people_type";
    }

    public enum MediaType {
        SHOW("SHOW"),
        MOVIE("MOVIE");

        private final String value;

        MediaType(String value) {
            this.value = value;
        }

        @NonNull
        @Override
        public String toString() {
            return value;
        }
    }

    enum PeopleType {
        CAST("CAST"),
        CREW("CREW");

        private final String value;

        PeopleType(String value) {
            this.value = value;
        }

        @NonNull
        @Override
        public String toString() {
            return value;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPeopleBinding binding = ActivityPeopleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupActionBar();

        // if there is a pane shadow, we are in two pane layout
        isTwoPane = binding.viewPeopleShadowStart != null;

        if (savedInstanceState == null) {
            // check if we should directly show a person
            int personTmdbId = getIntent().getIntExtra(PersonFragment.ARG_PERSON_TMDB_ID, -1);
            if (personTmdbId != -1) {
                showPerson(personTmdbId);

                // if this is not a dual pane layout, remove ourselves from back stack
                if (!isTwoPane) {
                    finish();
                    return;
                }
            }

            PeopleFragment f = new PeopleFragment();
            f.setArguments(getIntent().getExtras());

            // in two-pane mode, list items should be activated when touched
            if (isTwoPane) {
                f.setActivateOnItemClick();
            }

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.containerPeople, f, "people-list")
                    .commit();
        }
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        PeopleType peopleType = PeopleType.valueOf(
                getIntent().getStringExtra(InitBundle.PEOPLE_TYPE));
        setTitle(peopleType == PeopleType.CAST ? R.string.movie_cast : R.string.movie_crew);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_clear_24dp);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(
                    peopleType == PeopleType.CAST ? R.string.movie_cast : R.string.movie_crew);
        }
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
        if (isTwoPane) {
            // show inline
            PersonFragment f = PersonFragment.newInstance(tmdbId);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.containerPeoplePerson, f)
                    .commit();
        } else {
            // start new activity
            Intent i = new Intent(this, PersonActivity.class);
            i.putExtra(PersonFragment.ARG_PERSON_TMDB_ID, tmdbId);
            startActivity(i);
        }
    }
}
