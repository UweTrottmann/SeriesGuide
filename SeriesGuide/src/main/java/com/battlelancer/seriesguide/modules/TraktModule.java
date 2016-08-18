package com.battlelancer.seriesguide.modules;

import android.app.Application;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.services.Episodes;
import com.uwetrottmann.trakt5.services.Movies;
import com.uwetrottmann.trakt5.services.Recommendations;
import com.uwetrottmann.trakt5.services.Search;
import com.uwetrottmann.trakt5.services.Shows;
import com.uwetrottmann.trakt5.services.Sync;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

@Module
public class TraktModule {

    @Singleton
    @Provides
    Episodes provideEpisodes(TraktV2 trakt) {
        return trakt.episodes();
    }

    @Singleton
    @Provides
    Movies provideMovies(TraktV2 trakt) {
        return trakt.movies();
    }

    @Singleton
    @Provides
    Shows provideShows(TraktV2 trakt) {
        return trakt.shows();
    }

    @Singleton
    @Provides
    Recommendations provideRecommendations(TraktV2 trakt) {
        return trakt.recommendations();
    }

    @Singleton
    @Provides
    Search provideSearch(TraktV2 trakt) {
        return trakt.search();
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
