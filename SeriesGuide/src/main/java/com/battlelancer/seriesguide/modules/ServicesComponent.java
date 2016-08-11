package com.battlelancer.seriesguide.modules;

import com.battlelancer.seriesguide.loaders.MovieCreditsLoader;
import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = { AppModule.class, ServicesModule.class })
public interface ServicesComponent {
    void inject(MovieCreditsLoader movieCreditsLoader);
}
