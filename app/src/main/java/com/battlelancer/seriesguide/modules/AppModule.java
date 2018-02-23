package com.battlelancer.seriesguide.modules;

import android.content.Context;
import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {

    private final Context context;

    public AppModule(Context context) {
        this.context = context.getApplicationContext();
    }

    @Provides
    @ApplicationContext
    Context provideApplicationContext() {
        return context;
    }
}
