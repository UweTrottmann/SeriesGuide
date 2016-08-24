package com.battlelancer.seriesguide.dataliberation;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.BaseActivity;

public class DataLiberationActivity extends BaseActivity {

    public interface InitBundle {
        String EXTRA_SHOW_AUTOBACKUP = "showAutoBackup";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlepane);
        setupActionBar();

        if (savedInstanceState == null) {
            boolean showAutoBackup = getIntent().getBooleanExtra(InitBundle.EXTRA_SHOW_AUTOBACKUP,
                    false);
            Fragment f;
            if (showAutoBackup) {
                f = new AutoBackupFragment();
            } else {
                f = new DataLiberationFragment();
            }
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
        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return false;
    }

}
