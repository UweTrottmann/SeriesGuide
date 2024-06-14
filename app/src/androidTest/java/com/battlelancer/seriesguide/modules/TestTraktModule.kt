// SPDX-License-Identifier: Apache-2.0
// Copyright 2017-2024 Uwe Trottmann

package com.battlelancer.seriesguide.modules

import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.uwetrottmann.trakt5.services.Episodes
import com.uwetrottmann.trakt5.services.Movies
import com.uwetrottmann.trakt5.services.Search
import com.uwetrottmann.trakt5.services.Shows
import com.uwetrottmann.trakt5.services.Sync
import com.uwetrottmann.trakt5.services.Users

class TestTraktModule : TraktModule() {
    override fun provideEpisodes(trakt: SgTrakt): Episodes? {
        return null
    }

    override fun provideMovies(trakt: SgTrakt): Movies? {
        return null
    }

    override fun provideShows(trakt: SgTrakt): Shows? {
        return null
    }

    override fun provideSearch(trakt: SgTrakt): Search? {
        return null
    }

    override fun provideSync(trakt: SgTrakt): Sync? {
        return null
    }

    override fun provideUsers(trakt: SgTrakt): Users? {
        return null
    }
}