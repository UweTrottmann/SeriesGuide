
package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.uwetrottmann.seriesguide.R;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

/**
 * Shows a {@link ConnectTraktFragment} or if already connected to trakt a
 * {@link ConnectTraktCredentialsFragment}.
 */
public class ConnectTraktActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupActionBar();

        if (savedInstanceState == null) {
            if (TraktSettings.hasTraktCredentials(this)) {
                // immediately show credentials to allow disconnecting
                ConnectTraktCredentialsFragment f = ConnectTraktCredentialsFragment.newInstance();
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.add(android.R.id.content, f);
                ft.commit();
            } else {
                // display trakt introduction
                ConnectTraktFragment f = new ConnectTraktFragment();
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.add(android.R.id.content, f);
                ft.commit();
            }
        }
    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.connect_trakt);
        actionBar.setDisplayHomeAsUpEnabled(true);
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
