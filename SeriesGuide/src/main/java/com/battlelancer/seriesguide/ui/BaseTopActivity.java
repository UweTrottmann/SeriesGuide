
package com.battlelancer.seriesguide.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.billing.BillingActivity;
import com.battlelancer.seriesguide.billing.IabHelper;
import com.battlelancer.seriesguide.billing.IabResult;
import com.battlelancer.seriesguide.billing.Inventory;
import com.battlelancer.seriesguide.migration.MigrationActivity;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.R;

/**
 * Activities at the top of the navigation hierarchy, show menu on going up.
 */
public abstract class BaseTopActivity extends BaseNavDrawerActivity {

    private static final String TAG = "BaseTopActivity";
    private IabHelper mHelper;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);

        setupActionBar();

        // setup nav drawer to show special indicator
        getMenu().setSlideDrawable(R.drawable.ic_drawer);
        getMenu().setDrawerIndicatorEnabled(true);

        // query in-app purchases (only if not already qualified)
        if (Utils.requiresPurchaseCheck(this)) {
            mHelper = new IabHelper(this, BillingActivity.getPublicKey(this));
            mHelper.enableDebugLogging(BillingActivity.DEBUG);

            Log.d(TAG, "Starting In-App Billing helper setup.");
            mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                public void onIabSetupFinished(IabResult result) {
                    Log.d(TAG, "Setup finished.");

                    if (mHelper == null) {
                        // activity has already been destroyed and helper has
                        // been disposed of
                        return;
                    }

                    if (!result.isSuccess()) {
                        // Oh noes, there was a problem. But do not go crazy.
                        disposeIabHelper();
                        return;
                    }

                    // Hooray, IAB is fully set up. Now, let's get an inventory
                    // of stuff we own.
                    Log.d(TAG, "Setup successful. Querying inventory.");
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                }
            });
        }
    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        disposeIabHelper();
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
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            toggleMenu();
            return true;
        }
        else if (itemId == R.id.menu_subscribe) {
            startActivity(new Intent(this, BillingActivity.class));

            fireTrackerEvent("Subscribe");
            return true;
        }
        else if (itemId == R.id.menu_migrate) {
            startActivity(new Intent(this, MigrationActivity.class));
            return true;
        }
        else if (itemId == R.id.menu_preferences) {
            startActivity(new Intent(this, SeriesGuidePreferences.class));

            fireTrackerEvent("Settings");
            return true;
        }
        else if (itemId == R.id.menu_help) {
            startActivity(new Intent(this, HelpActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

            fireTrackerEvent("Help");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Listener that's called when we finish querying the items and
    // subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");
            if (result.isFailure()) {
                // ignore failures (maybe not, requires testing)
                disposeIabHelper();
                return;
            }

            Log.d(TAG, "Query inventory was successful.");

            BillingActivity.checkForSubscription(BaseTopActivity.this, inventory);

            Log.d(TAG, "Inventory query finished.");
            disposeIabHelper();
        }
    };

    private void disposeIabHelper() {
        if (mHelper != null) {
            Log.d(TAG, "Disposing of IabHelper.");
            mHelper.dispose();
        }
        mHelper = null;
    }

    /**
     * Google Analytics helper method for easy sending of click events.
     */
    protected abstract void fireTrackerEvent(String label);
}
