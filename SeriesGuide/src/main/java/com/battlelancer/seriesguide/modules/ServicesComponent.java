package com.battlelancer.seriesguide.modules;

import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.loaders.ShowCreditsLoader;
import com.battlelancer.seriesguide.loaders.TmdbMoviesLoader;
import com.battlelancer.seriesguide.loaders.TraktAddLoader;
import com.battlelancer.seriesguide.loaders.TraktCommentsLoader;
import com.battlelancer.seriesguide.loaders.TvdbAddLoader;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.thetvdbapi.TvdbTools;
import com.battlelancer.seriesguide.util.AddShowTask;
import com.battlelancer.seriesguide.util.ConnectTraktTask;
import com.battlelancer.seriesguide.util.MovieTools;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.TraktRatingsTask;
import com.uwetrottmann.thetvdb.services.TheTvdbEpisodes;
import com.uwetrottmann.tmdb2.services.MoviesService;
import com.uwetrottmann.tmdb2.services.PeopleService;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.services.Checkin;
import com.uwetrottmann.trakt5.services.Comments;
import com.uwetrottmann.trakt5.services.Sync;
import com.uwetrottmann.trakt5.services.Users;
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
public interface ServicesComponent {

    HexagonTools hexagonTools();
    MoviesService moviesService();
    MovieTools movieTools();
    PeopleService peopleService();
    ShowTools showTools();
    TraktV2 trakt();
    Checkin traktCheckin();
    Comments traktComments();
    Sync traktSync();
    Users traktUsers();
    TheTvdbEpisodes tvdbEpisodes();

    TvdbTools tvdbTools();
    void inject(AddShowTask addShowTask);
    void inject(ConnectTraktTask connectTraktTask);
    void inject(SgSyncAdapter sgSyncAdapter);
    void inject(ShowCreditsLoader showCreditsLoader);
    void inject(ShowTools.ShowsUploadTask showsUploadTask);
    void inject(TmdbMoviesLoader tmdbMoviesLoader);
    void inject(TraktAddLoader traktAddLoader);
    void inject(TraktCommentsLoader traktCommentsLoader);
    void inject(TraktRatingsTask traktRatingsTask);
    void inject(TvdbAddLoader tvdbAddLoader);
}
