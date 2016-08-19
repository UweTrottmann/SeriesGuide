package com.battlelancer.seriesguide.modules;

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
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.thetvdbapi.TvdbTools;
import com.battlelancer.seriesguide.tmdbapi.SgTmdbInterceptor;
import com.battlelancer.seriesguide.traktapi.TraktAuthActivity;
import com.battlelancer.seriesguide.ui.dialogs.TraktCancelCheckinDialogFragment;
import com.battlelancer.seriesguide.util.AddShowTask;
import com.battlelancer.seriesguide.util.ConnectTraktTask;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.MovieTools;
import com.battlelancer.seriesguide.util.TraktRatingsTask;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.TraktTools;
import com.battlelancer.seriesguide.util.tasks.BaseMovieActionTask;
import com.battlelancer.seriesguide.util.tasks.BaseRateItemTask;
import com.battlelancer.seriesguide.util.tasks.BaseShowActionTask;
import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = { AppModule.class, TmdbModule.class, TraktModule.class, TvdbModule.class })
public interface ServicesComponent {
    void inject(AddShowTask addShowTask);
    void inject(BaseMovieActionTask baseMovieActionTask);
    void inject(BaseRateItemTask baseRateItemTask);
    void inject(BaseShowActionTask baseShowActionTask);
    void inject(ConnectTraktTask connectTraktTask);
    void inject(EpisodeTools.EpisodeFlagTask episodeFlagTask);
    void inject(MovieCreditsLoader movieCreditsLoader);
    void inject(MovieTrailersLoader movieTrailersLoader);
    void inject(MovieTools movieTools);
    void inject(PersonLoader personLoader);
    void inject(SgSyncAdapter sgSyncAdapter);
    void inject(SgTmdbInterceptor sgTmdbInterceptor);
    void inject(ShowCreditsLoader showCreditsLoader);
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
    void inject(TvdbTools tvdbTools);
}
