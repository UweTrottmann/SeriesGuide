
/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SeriesGuideApplication;
import timber.log.Timber;

public class AccountUtils {

    public static final int SYNC_FREQUENCY = 24 * 60 * 60; // 1 day (in seconds)

    private static final String ACCOUNT_NAME = "SeriesGuide Sync";

    public static void createAccount(Context context) {
        Timber.d("Setting up account...");

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

        Timber.d("Setting up account...DONE");
    }

    private static void removeAccount(Context context) {
        Timber.d("Removing existing accounts...");

        AccountManager manager = AccountManager.get(context);
        Account[] accounts = manager.getAccountsByType(context.getString(R.string.package_name));
        for (Account account : accounts) {
            manager.removeAccount(account, null, null);
        }

        Timber.d("Removing existing accounts...DONE");
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
