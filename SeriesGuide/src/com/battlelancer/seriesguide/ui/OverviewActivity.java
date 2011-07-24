
package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class OverviewActivity extends BaseActivity {

    private Fragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.overview_multipane);

        getActivityHelper().setupActionBar(getString(R.string.description_overview));

        if (savedInstanceState == null) {
            mFragment = new OverviewFragment();
            mFragment.setArguments(getIntent().getExtras());

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_overview, mFragment).commit();
        }
    }
}
