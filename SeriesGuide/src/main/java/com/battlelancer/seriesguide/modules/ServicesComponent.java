package com.battlelancer.seriesguide.modules;

import com.battlelancer.seriesguide.backend.CloudSetupFragment;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.loaders.MovieCreditsLoader;
import com.battlelancer.seriesguide.loaders.MovieTrailersLoader;
import com.battlelancer.seriesguide.loaders.PersonLoader;
import com.battlelancer.seriesguide.loaders.ShowCreditsLoader;
import com.battlelancer.seriesguide.loaders.TmdbMoviesLoader;
import com.battlelancer.seriesguide.loaders.TraktAddLoader;
import com.battlelancer.seriesguide.loaders.TraktCommentsLoader;
import com.battlelancer.seriesguide.loaders.TraktEpisodeHistoryLoader;
import com.battlelancer.seriesguide.loaders.TraktFriendsEpisodeHistoryLoader;
import com.battlelancer.seriesguide.loaders.TraktFriendsMovieHistoryLoader;
import com.battlelancer.seriesguide.loaders.TraktRecentEpisodeHistoryLoader;
import com.battlelancer.seriesguide.loaders.TvdbAddLoader;
import com.battlelancer.seriesguide.loaders.TvdbShowLoader;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.thetvdbapi.TvdbEpisodeDetailsTask;
import com.battlelancer.seriesguide.traktapi.TraktAuthActivity;
import com.battlelancer.seriesguide.ui.dialogs.TraktCancelCheckinDialogFragment;
import com.battlelancer.seriesguide.util.AddShowTask;
import com.battlelancer.seriesguide.util.ConnectTraktTask;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.MovieTools;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.TraktRatingsTask;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.TraktTools;
import com.uwetrottmann.trakt5.services.Sync;
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
    MovieTools movieTools();
    Sync traktSync();

    void inject(AddShowTask addShowTask);
    void inject(CloudSetupFragment cloudSetupFragment);
    void inject(ConnectTraktTask connectTraktTask);
    void inject(EpisodeTools.EpisodeFlagTask episodeFlagTask);
    void inject(MovieCreditsLoader movieCreditsLoader);
    void inject(MovieTrailersLoader movieTrailersLoader);
    void inject(MovieTools movieTools);
    void inject(PersonLoader personLoader);
    void inject(SgSyncAdapter sgSyncAdapter);
    void inject(ShowCreditsLoader showCreditsLoader);
    void inject(ShowTools.ShowsUploadTask showsUploadTask);
    void inject(TmdbMoviesLoader tmdbMoviesLoader);
    void inject(TraktAddLoader traktAddLoader);
    void inject(TraktAuthActivity traktAuthActivity);
    void inject(TraktCancelCheckinDialogFragment traktCancelCheckinDialogFragment);
    void inject(TraktCommentsLoader traktCommentsLoader);
    void inject(TraktEpisodeHistoryLoader traktEpisodeHistoryLoader);
    void inject(TraktFriendsEpisodeHistoryLoader traktFriendsEpisodeHistoryLoader);
    void inject(TraktFriendsMovieHistoryLoader traktFriendsMovieHistoryLoader);
    void inject(TraktRatingsTask traktRatingsTask);
    void inject(TraktRecentEpisodeHistoryLoader traktRecentEpisodeHistoryLoader);
    void inject(TraktTask traktTask);
    void inject(TraktTools traktTools);
    void inject(TvdbAddLoader tvdbAddLoader);
    void inject(TvdbEpisodeDetailsTask tvdbEpisodeDetailsTask);
    void inject(TvdbShowLoader tvdbShowLoader);
}
