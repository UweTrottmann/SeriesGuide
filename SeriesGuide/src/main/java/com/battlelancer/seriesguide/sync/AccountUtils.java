
package com.battlelancer.seriesguide.sync;

import com.battlelancer.seriesguide.SeriesGuideApplication;
import com.uwetrottmann.seriesguide.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

public class AccountUtils {

    private static final String TAG = "AccountUtils";

    private static final String ACCOUNT_NAME = "SeriesGuide Sync";

    private static final int SYNC_FREQUENCY = 24 * 60 * 60; // 1 day (in seconds)

    public static void createAccount(Context context) {
        Log.d(TAG, "Setting up account...");

        // remove any existing accounts
        removeAccount(context);

        // create a new account
        AccountManager manager = AccountManager.get(context);
        Account account = new Account(ACCOUNT_NAME, context.getString(R.string.package_name));
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

        Log.d(TAG, "Setting up account...DONE");
    }

    private static void removeAccount(Context context) {
        Log.d(TAG, "Removing existing accounts...");

        AccountManager manager = AccountManager.get(context);
        Account[] accounts = manager.getAccountsByType(context.getString(R.string.package_name));
        for (Account account : accounts) {
            manager.removeAccount(account, null, null);
        }

        Log.d(TAG, "Removing existing accounts...DONE");
    }

    public static boolean isAccountExists(Context context) {
        AccountManager manager = AccountManager.get(context);
        Account[] accounts = manager.getAccountsByType(context.getString(R.string.package_name));
        return accounts.length > 0;
    }

    public static Account getAccount(Context context) {
        AccountManager manager = AccountManager.get(context);
        Account[] accounts = manager.getAccountsByType(context.getString(R.string.package_name));

        // return first available account
        if (accounts.length > 0) {
            return accounts[0];
        }

        return null;
    }

}
