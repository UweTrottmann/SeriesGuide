
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
import com.battlelancer.seriesguide.billing.Purchase;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.R;

/**
 * Activities at the top of the navigation hierarchy, show menu on going up.
 */
public abstract class BaseTopActivity extends BaseActivity {

    private static final String TAG = "BaseTopActivity";
    private IabHelper mHelper;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // setup nav drawer to show special indicator
        getMenu().setSlideDrawable(R.drawable.ic_drawer);
        getMenu().setDrawerIndicatorEnabled(true);

        // query in-app purchases (only if not already X installed)
        if (!Utils.hasXinstalled(this)) {
            String key = getString(R.string.key_a) + getString(R.string.key_b)
                    + getString(R.string.key_c) + getString(R.string.key_d);
            mHelper = new IabHelper(this, key);
            mHelper.enableDebugLogging(BillingActivity.DEBUG);
            Log.d(TAG, "Starting In-App Billing helper setup.");
            mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                public void onIabSetupFinished(IabResult result) {
                    Log.d(TAG, "Setup finished.");

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
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            toggleMenu();
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

            // Do we have the premium upgrade?
            Purchase premiumPurchase = inventory.getPurchase(BillingActivity.SKU_X);
            boolean hasXUpgrade = (premiumPurchase != null && BillingActivity
                    .verifyDeveloperPayload(premiumPurchase));
            Log.d(TAG, "User has " + (hasXUpgrade ? "X UPGRADE" : "NOT X UPGRADE"));

            // Save current state until we query again
            AdvancedSettings.setLastUpgradeState(BaseTopActivity.this, hasXUpgrade);

            Log.d(TAG, "Inventory query finished.");
            disposeIabHelper();
        }
    };

    /**
     * Google Analytics helper method for easy sending of click events.
     */
    protected abstract void fireTrackerEvent(String label);

    private void disposeIabHelper() {
        if (mHelper != null) {
            Log.d(TAG, "Disposing of IabHelper.");
            mHelper.dispose();
        }
        mHelper = null;
    }
}
