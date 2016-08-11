package com.battlelancer.seriesguide.modules;

import com.battlelancer.seriesguide.loaders.MovieCreditsLoader;
import com.battlelancer.seriesguide.loaders.MovieTrailersLoader;
import com.battlelancer.seriesguide.loaders.PersonLoader;
import com.battlelancer.seriesguide.loaders.ShowCreditsLoader;
import com.battlelancer.seriesguide.loaders.TmdbMoviesLoader;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.thetvdbapi.TvdbTools;
import com.battlelancer.seriesguide.tmdbapi.SgTmdbInterceptor;
import com.battlelancer.seriesguide.util.MovieTools;
import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = { AppModule.class, TmdbModule.class, TvdbModule.class })
public interface ServicesComponent {
    void inject(MovieCreditsLoader movieCreditsLoader);
    void inject(MovieTrailersLoader movieTrailersLoader);
    void inject(MovieTools movieTools);
    void inject(PersonLoader personLoader);
    void inject(SgSyncAdapter sgSyncAdapter);
    void inject(SgTmdbInterceptor sgTmdbInterceptor);
    void inject(ShowCreditsLoader showCreditsLoader);
    void inject(TvdbTools tvdbTools);
    void inject(TmdbMoviesLoader tmdbMoviesLoader);
}
