
package com.battlelancer.seriesguide.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

import com.battlelancer.seriesguide.SeriesGuideApplication;

public class SyncUtils {

    private static final int SYNC_FREQUENCY = 24 * 60 * 60; // 1 day (in
                                                            // seconds)

    public static void createSyncAccount(Context context) {
        // Create account, if it's missing. (Either first run, or user has
        // deleted account.)
        AccountManager manager = AccountManager.get(context);
        final Account account = SgAccountAuthenticator.getSyncAccount(context);
        if (manager.addAccountExplicitly(account, null, null)) {
            // Inform the system that this account supports sync
            ContentResolver.setIsSyncable(account, SeriesGuideApplication.CONTENT_AUTHORITY, 1);
            // Inform the system that this account is eligible for auto sync
            // when the network is up
            ContentResolver.setSyncAutomatically(account, SeriesGuideApplication.CONTENT_AUTHORITY,
                    true);
            // Recommend a schedule for automatic synchronization. The system
            // may modify this based
            // on other scheduled syncs and network utilization.
            ContentResolver.addPeriodicSync(account, SeriesGuideApplication.CONTENT_AUTHORITY,
                    new Bundle(), SYNC_FREQUENCY);
        }
    }

}
