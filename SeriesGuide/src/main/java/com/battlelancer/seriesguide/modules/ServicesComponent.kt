package com.battlelancer.seriesguide.modules

import com.battlelancer.seriesguide.backend.HexagonTools
import com.battlelancer.seriesguide.ui.people.ShowCreditsLoader
import com.battlelancer.seriesguide.ui.movies.TmdbMoviesLoader
import com.battlelancer.seriesguide.loaders.TraktAddLoader
import com.battlelancer.seriesguide.ui.comments.TraktCommentsLoader
import com.battlelancer.seriesguide.loaders.TvdbAddLoader
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.thetvdbapi.TvdbTools
import com.battlelancer.seriesguide.util.AddShowTask
import com.battlelancer.seriesguide.util.ConnectTraktTask
import com.battlelancer.seriesguide.util.MovieTools
import com.battlelancer.seriesguide.util.ShowTools
import com.battlelancer.seriesguide.util.TraktRatingsTask
import com.uwetrottmann.thetvdb.services.TheTvdbEpisodes
import com.uwetrottmann.tmdb2.services.MoviesService
import com.uwetrottmann.tmdb2.services.PeopleService
import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.services.Checkin
import com.uwetrottmann.trakt5.services.Comments
import com.uwetrottmann.trakt5.services.Sync
import com.uwetrottmann.trakt5.services.Users
import dagger.Component
import javax.inject.Singleton

/**
 * WARNING: for Dagger2 to work with kapt, this interface has to be in Kotlin.
 */
@Singleton
@Component(modules = arrayOf(
        AppModule::class,
        HttpClientModule::class,
        TmdbModule::class,
        TraktModule::class,
        TvdbModule::class
))
interface ServicesComponent {

    fun hexagonTools(): HexagonTools
    fun moviesService(): MoviesService
    fun movieTools(): MovieTools
    fun peopleService(): PeopleService
    fun showTools(): ShowTools
    fun trakt(): TraktV2
    fun traktCheckin(): Checkin
    fun traktComments(): Comments
    fun traktSync(): Sync
    fun traktUsers(): Users
    fun tvdbEpisodes(): TheTvdbEpisodes
    fun tvdbTools(): TvdbTools

    fun inject(addShowTask: AddShowTask)
    fun inject(connectTraktTask: ConnectTraktTask)
    fun inject(sgSyncAdapter: SgSyncAdapter)
    fun inject(showCreditsLoader: ShowCreditsLoader)
    fun inject(showsUploadTask: ShowTools.ShowsUploadTask)
    fun inject(tmdbMoviesLoader: TmdbMoviesLoader)
    fun inject(traktAddLoader: TraktAddLoader)
    fun inject(traktCommentsLoader: TraktCommentsLoader)
    fun inject(traktRatingsTask: TraktRatingsTask)
    fun inject(tvdbAddLoader: TvdbAddLoader)
}
