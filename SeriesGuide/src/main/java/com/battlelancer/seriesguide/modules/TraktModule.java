package com.battlelancer.seriesguide.modules;

import android.app.Application;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.services.Recommendations;
import com.uwetrottmann.trakt5.services.Sync;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

@Module
public class TraktModule {

    @Singleton
    @Provides
    Recommendations provideRecommendations(TraktV2 trakt) {
        return trakt.recommendations();
    }

    @Singleton
    @Provides
    Sync provideSync(TraktV2 trakt) {
        return trakt.sync();
    }

    @Provides
    TraktV2 provideTrakt(Application application) {
        return ServiceUtils.getTrakt(application);
    }

}
