
package com.battlelancer.seriesguide.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.seriesguide.R;

/**
 * Activities at the top of the navigation hierarchy, show menu on going up.
 */
public abstract class BaseTopActivity extends BaseActivity {

    protected static final String TAG = "BaseTopActivity";

    /**
     * Google Analytics helper method for easy event tracking.
     */
    public void fireTrackerEvent(String label) {
        EasyTracker.getTracker().trackEvent(TAG, "Click", label, (long) 0);
    }

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
            startActivity(new Intent(this, SeriesGuidePreferences.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            return true;
        }
        else if (itemId == R.id.menu_help) {
            Intent myIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(SeriesGuidePreferences.HELP_URL));
            startActivity(myIntent);
            
            fireTrackerEvent("Help");
            return true;
        }
        else if (itemId == R.id.menu_feedback) {
            final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
            intent.setType("plain/text");
            intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] {
                    SeriesGuidePreferences.SUPPORT_MAIL
            });
            intent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                    "SeriesGuide " + Utils.getVersion(this) + " Feedback");
            intent.putExtra(android.content.Intent.EXTRA_TEXT, "");
            startActivity(Intent.createChooser(intent, getString(R.string.feedback)));

            fireTrackerEvent("Feedback");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
