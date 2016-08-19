package com.battlelancer.seriesguide.modules;

import android.app.Application;
import com.battlelancer.seriesguide.SgApp;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

@Module
public class AppModule {

    private final SgApp application;

    public AppModule(SgApp application) {
        this.application = application;
    }

    @Provides
    SgApp providesSgApp() {
        return application;
    }

    @Provides
    Application providesApplication() {
        return application;
    }
}
