
package com.battlelancer.seriesguide.sync;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;


public class SgAuthenticationService extends Service {
    SgAccountAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        mAuthenticator = new SgAccountAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }

}
