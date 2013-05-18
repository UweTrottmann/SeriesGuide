
package com.battlelancer.seriesguide.billing;

import android.os.Bundle;
import android.util.Log;

import com.battlelancer.seriesguide.ui.BaseActivity;

public class BillingActivity extends BaseActivity {

    protected static final String TAG = "BillingActivity";
    private IabHelper mHelper;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);

        String base64EncodedPublicKey = null;
        mHelper = new IabHelper(this, base64EncodedPublicKey);

        Log.d(TAG, "Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    Log.d(TAG, "Problem setting up In-app Billing: " + result);
                }
                // Hooray, IAB is fully set up!
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mHelper != null) {
            mHelper.dispose();
        }
        mHelper = null;
    }

}
