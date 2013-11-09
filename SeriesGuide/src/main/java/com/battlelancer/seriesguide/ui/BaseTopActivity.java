
package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.billing.BillingActivity;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.R;

import android.content.Intent;
import android.os.Bundle;

/**
 * Activities at the top of the navigation hierarchy, show menu on going up.
 */
public abstract class BaseTopActivity extends BaseNavDrawerActivity {

    private static final String TAG = "BaseTopActivity";

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);

        setupActionBar();

    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void setupNavDrawer() {
        super.setupNavDrawer();

        // show a drawer indicator
        setDrawerIndicatorEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.base_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        // show subscribe button if not subscribed, yet
        menu.findItem(R.id.menu_subscribe).setVisible(!Utils.hasAccessToX(this));

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // check if we should toggle the navigation drawer (app icon was touched)
        if (toggleDrawer(item)) {
            return true;
        }

        int itemId = item.getItemId();
        if (itemId == R.id.menu_subscribe) {
            startActivity(new Intent(this, BillingActivity.class));

            fireTrackerEvent("Subscribe");
            return true;
        } else if (itemId == R.id.menu_preferences) {
            startActivity(new Intent(this, SeriesGuidePreferences.class));

            fireTrackerEvent("Settings");
            return true;
        } else if (itemId == R.id.menu_help) {
            startActivity(new Intent(this, HelpActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

            fireTrackerEvent("Help");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Google Analytics helper method for easy sending of click events.
     */
    protected abstract void fireTrackerEvent(String label);
}
