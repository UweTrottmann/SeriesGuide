package com.battlelancer.seriesguide.modules;

import com.battlelancer.seriesguide.sync.TvdbSyncTest;
import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = {
        AppModule.class,
        HttpClientModule.class,
        TmdbModule.class,
        TraktModule.class,
        TvdbModule.class
})
public interface TestServicesComponent extends ServicesComponent {
    void inject(TvdbSyncTest tvdbSyncTest);
}
