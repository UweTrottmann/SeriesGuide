
package com.battlelancer.seriesguide.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.SgApp;
import com.uwetrottmann.androidutils.AndroidUtils;
import timber.log.Timber;

public class AccountUtils {

    public static final int SYNC_FREQUENCY = 24 * 60 * 60; // 1 day (in seconds)

    private static final String ACCOUNT_NAME = "SeriesGuide Sync";

    private static final String ACCOUNT_TYPE = BuildConfig.APPLICATION_ID;

    public static void createAccount(Context context) {
        Timber.d("Setting up account...");

        // remove any existing accounts
        removeAccount(context);

        // try to create a new account
        AccountManager manager = AccountManager.get(context);
        Account account = new Account(ACCOUNT_NAME, ACCOUNT_TYPE);

        boolean isNewAccountAdded;
        try {
            isNewAccountAdded = manager != null
                    && manager.addAccountExplicitly(account, null, null);
        } catch (SecurityException e) {
            Timber.e(e, "Setting up account...FAILED Account could not be added");
            return;
        }
        if (isNewAccountAdded) {
            // Inform the system that this account supports sync
            ContentResolver.setIsSyncable(account, SgApp.CONTENT_AUTHORITY, 1);
            // Inform the system that this account is eligible for auto sync
            // when the network is up
            ContentResolver.setSyncAutomatically(account, SgApp.CONTENT_AUTHORITY,
                    true);
            // Recommend a schedule for automatic synchronization. The system
            // may modify this based
            // on other scheduled syncs and network utilization.
            ContentResolver.addPeriodicSync(account, SgApp.CONTENT_AUTHORITY,
                    new Bundle(), SYNC_FREQUENCY);
        }

        Timber.d("Setting up account...DONE");
    }

    private static void removeAccount(Context context) {
        Timber.d("Removing existing accounts...");

        AccountManager manager = AccountManager.get(context);
        Account[] accounts = manager.getAccountsByType(ACCOUNT_TYPE);
        for (Account account : accounts) {
            if (AndroidUtils.isLollipopMR1OrHigher()) {
                manager.removeAccount(account, null, null, null);
            } else {
                manager.removeAccount(account, null, null);
            }
        }

        Timber.d("Removing existing accounts...DONE");
    }

    public static boolean isAccountExists(Context context) {
        AccountManager manager = AccountManager.get(context);
        Account[] accounts = manager.getAccountsByType(ACCOUNT_TYPE);
        return accounts.length > 0;
    }

    public static Account getAccount(Context context) {
        AccountManager manager = AccountManager.get(context);
        Account[] accounts = manager.getAccountsByType(ACCOUNT_TYPE);

        // return first available account
        if (accounts.length > 0) {
            return accounts[0];
        }

        return null;
    }
}
