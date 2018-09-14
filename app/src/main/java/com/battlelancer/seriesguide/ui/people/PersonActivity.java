package com.battlelancer.seriesguide.ui.people;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.view.MenuItem;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.BaseActivity;

/**
 * Hosts a {@link PersonFragment}, only used on handset devices. On
 * tablet-size devices a two-pane layout inside {@link PeopleActivity}
 * is used.
 */
public class PersonActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // instead of Window.FEATURE_ACTION_BAR_OVERLAY as indicated by AppCompatDelegate warning
        supportRequestWindowFeature(AppCompatDelegate.FEATURE_SUPPORT_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person);
        setupActionBar();

        if (savedInstanceState == null) {
            PersonFragment f = PersonFragment.newInstance(
                    getIntent().getIntExtra(PersonFragment.ARG_PERSON_TMDB_ID, 0));
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.containerPerson, f)
                    .commit();
        }
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setBackgroundDrawable(
                    ContextCompat.getDrawable(this, R.drawable.background_actionbar_gradient));
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
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
}
