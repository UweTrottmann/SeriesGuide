package com.battlelancer.seriesguide.modules

import com.battlelancer.seriesguide.backend.HexagonTools
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.ui.comments.TraktCommentsLoader
import com.battlelancer.seriesguide.ui.movies.MovieTools
import com.battlelancer.seriesguide.ui.search.AddShowTask
import com.battlelancer.seriesguide.ui.search.TraktAddLoader
import com.battlelancer.seriesguide.ui.shows.ShowTools
import com.uwetrottmann.tmdb2.Tmdb
import com.uwetrottmann.tmdb2.services.MoviesService
import com.uwetrottmann.tmdb2.services.PeopleService
import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.services.Sync
import com.uwetrottmann.trakt5.services.Users
import dagger.Component
import javax.inject.Singleton

/**
 * WARNING: for Dagger2 to work with kapt, this interface has to be in Kotlin.
 */
@Singleton
@Component(
    modules = [
        AppModule::class,
        HttpClientModule::class,
        TmdbModule::class,
        TraktModule::class
    ]
)
interface ServicesComponent {

    fun hexagonTools(): HexagonTools
    fun moviesService(): MoviesService
    fun movieTools(): MovieTools
    fun peopleService(): PeopleService?
    fun showTools(): ShowTools
    fun tmdb(): Tmdb
    fun trakt(): TraktV2
    fun traktSync(): Sync?
    fun traktUsers(): Users?

    fun inject(addShowTask: AddShowTask)
    fun inject(sgSyncAdapter: SgSyncAdapter)
    fun inject(traktAddLoader: TraktAddLoader)
    fun inject(traktCommentsLoader: TraktCommentsLoader)
}
