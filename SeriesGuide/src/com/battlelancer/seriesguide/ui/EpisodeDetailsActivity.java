
package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class EpisodeDetailsActivity extends BaseActivity {
    protected static final String TAG = "EpisodeDetailsActivity";

    private Fragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlepane_empty);

        getActivityHelper().setupActionBar(getString(R.string.episode));

        if (savedInstanceState == null) {
            mFragment = new EpisodeDetailsFragment();
            mFragment.setArguments(getIntent().getExtras());

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.root_container, mFragment, "fragmentDetails").commit();
        }
    }
}
