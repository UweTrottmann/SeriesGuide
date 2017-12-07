
package com.battlelancer.seriesguide.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import timber.log.Timber;

public class SgAuthenticationService extends Service {

    SgAccountAuthenticator authenticator;

    @Override
    public void onCreate() {
        Timber.d("Creating authenticator service");
        authenticator = new SgAccountAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Timber.d("Binding authenticator");
        return authenticator.getIBinder();
    }

}
