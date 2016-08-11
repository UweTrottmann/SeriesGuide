package com.battlelancer.seriesguide.modules;

import com.battlelancer.seriesguide.loaders.MovieCreditsLoader;
import com.battlelancer.seriesguide.loaders.MovieTrailersLoader;
import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = { AppModule.class, ServicesModule.class })
public interface ServicesComponent {
    void inject(MovieCreditsLoader movieCreditsLoader);
    void inject(MovieTrailersLoader movieTrailersLoader);
}
