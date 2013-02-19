
package com.battlelancer.seriesguide.ui;

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.uwetrottmann.seriesguide.R;

/**
 * Activities at the top of the navigation hierarchy, show menu on going up.
 */
public abstract class BaseTopActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.base_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            toggle();
            return true;
        }
        else if (itemId == R.id.menu_preferences) {
            fireTrackerEvent("Settings");

            startActivity(new Intent(this, SeriesGuidePreferences.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            return true;
        }
        else if (itemId == R.id.menu_help) {
            fireTrackerEvent("Help");

            startActivity(new Intent(this, HelpActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Google Analytics helper method for easy sending of click events.
     */
    protected abstract void fireTrackerEvent(String label);
}
