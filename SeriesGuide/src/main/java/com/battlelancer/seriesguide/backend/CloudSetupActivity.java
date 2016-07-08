package com.battlelancer.seriesguide.backend;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.BaseActivity;

/**
 * Hosts {@link CloudSetupFragment} to setup hexagon.
 */
public class CloudSetupActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlepane);
        setupActionBar();

        if (savedInstanceState == null) {
            CloudSetupFragment f = new CloudSetupFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.content_frame, f).commit();
        }
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
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
