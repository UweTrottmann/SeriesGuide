package com.battlelancer.seriesguide.modules;

import android.app.Application;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.tmdbapi.SgTmdb;
import com.uwetrottmann.tmdb2.Tmdb;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

@Module
public class ServicesModule {

    @Singleton
    @Provides
    Tmdb provideSgTmdb(Application application) {
        return new SgTmdb(application, BuildConfig.TMDB_API_KEY);
    }

}
