package com.battlelancer.seriesguide.modules

import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.services.Episodes
import com.uwetrottmann.trakt5.services.Movies
import com.uwetrottmann.trakt5.services.Search
import com.uwetrottmann.trakt5.services.Shows
import com.uwetrottmann.trakt5.services.Sync
import com.uwetrottmann.trakt5.services.Users

class TestTraktModule : TraktModule() {
    override fun provideEpisodes(trakt: TraktV2): Episodes? {
        return null
    }

    override fun provideMovies(trakt: TraktV2): Movies? {
        return null
    }

    override fun provideShows(trakt: TraktV2): Shows? {
        return null
    }

    override fun provideSearch(trakt: TraktV2): Search? {
        return null
    }

    override fun provideSync(trakt: TraktV2): Sync? {
        return null
    }

    override fun provideUsers(trakt: TraktV2): Users? {
        return null
    }
}