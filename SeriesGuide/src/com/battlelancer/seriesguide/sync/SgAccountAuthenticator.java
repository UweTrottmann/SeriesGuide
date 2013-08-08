
package com.battlelancer.seriesguide.sync;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.battlelancer.seriesguide.SeriesGuideApplication;
import com.uwetrottmann.seriesguide.R;

public class SgAccountAuthenticator extends AbstractAccountAuthenticator {

    public static final String ACCOUNT_NAME = "SeriesGuide Sync";
    private static final String TAG = "SgAccountAuthenticator";
    private Context mContext;

    public static Account getSyncAccount(Context context) {
        return new Account(ACCOUNT_NAME, context.getString(R.string.package_name));
    }

    public SgAccountAuthenticator(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
            String authTokenType, String[] requiredFeatures, Bundle options)
            throws NetworkErrorException {
        Log.d(TAG, "Setting up sync account");

        // set up a dummy account for syncing
        AccountManager manager = AccountManager.get(mContext);
        final Account account = getSyncAccount(mContext);
        manager.addAccountExplicitly(account, null, null);
        ContentResolver.setIsSyncable(account, SeriesGuideApplication.CONTENT_AUTHORITY, 1);
        ContentResolver.setSyncAutomatically(account, SeriesGuideApplication.CONTENT_AUTHORITY,
                true);

        Log.d(TAG, "Finished setting up sync account");

        return null;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
            Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
            String[] features) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response, Account account)
            throws NetworkErrorException {
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
        return result;
    }

}
