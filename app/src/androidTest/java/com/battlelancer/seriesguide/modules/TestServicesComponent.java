package com.battlelancer.seriesguide.modules;

import com.battlelancer.seriesguide.sync.TmdbSyncTest;
import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = {
        AppModule.class,
        HttpClientModule.class,
        TmdbModule.class,
        TraktModule.class
})
public interface TestServicesComponent extends ServicesComponent {
    void inject(TmdbSyncTest tmdbSyncTest);
}
