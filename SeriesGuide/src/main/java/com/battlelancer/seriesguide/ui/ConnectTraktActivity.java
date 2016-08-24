
package com.battlelancer.seriesguide.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.TraktCredentials;

/**
 * Shows a {@link ConnectTraktFragment} or if already connected to trakt a
 * {@link ConnectTraktCredentialsFragment}.
 */
public class ConnectTraktActivity extends BaseActivity {

    static final int OAUTH_CODE_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlepane);
        setupActionBar();

        if (savedInstanceState == null) {
            if (TraktCredentials.get(this).hasCredentials()) {
                // immediately show credentials to allow disconnecting
                ConnectTraktCredentialsFragment f = ConnectTraktCredentialsFragment.newInstance();
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.add(R.id.content_frame, f);
                ft.commit();
            } else {
                // display trakt introduction
                ConnectTraktFragment f = new ConnectTraktFragment();
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.add(R.id.content_frame, f);
                ft.commit();
            }
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
