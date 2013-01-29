
package com.battlelancer.seriesguide.ui;

import android.content.Intent;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.uwetrottmann.seriesguide.R;

/**
 * Adds action items specific to top show activities.
 */
public abstract class BaseTopShowsActivity extends BaseTopActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.base_show_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_checkin) {
            startActivity(new Intent(this, CheckinActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
