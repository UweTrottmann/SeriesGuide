
package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.R;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

public class EpisodesActivity extends BaseActivity {

    private Fragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.episodes_multipane);

        getActivityHelper().setupActionBar(getTitle());

        final String customTitle = getIntent().getStringExtra(Intent.EXTRA_TITLE);
        getActivityHelper().setActionBarTitle(customTitle != null ? customTitle : getTitle());

        if (savedInstanceState == null) {
            mFragment = onCreatePane();
            mFragment.setArguments(intentToFragmentArguments(getIntent()));

            getSupportFragmentManager().beginTransaction().add(R.id.fragment_episodes, mFragment)
                    .commit();
        }
    }

    protected Fragment onCreatePane() {
        return new EpisodesFragment();
    }

}
