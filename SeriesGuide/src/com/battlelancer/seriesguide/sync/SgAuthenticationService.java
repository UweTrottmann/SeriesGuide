
package com.battlelancer.seriesguide.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class SgAuthenticationService extends Service {

    private static final String TAG = "SgAuthenticationService";
    SgAccountAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        Log.d(TAG, "Creating authenticator service");
        mAuthenticator = new SgAccountAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Binding authenticator");
        return mAuthenticator.getIBinder();
    }

}
