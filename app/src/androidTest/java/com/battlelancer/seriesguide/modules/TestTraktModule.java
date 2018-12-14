package com.battlelancer.seriesguide.modules;

import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.services.Episodes;
import com.uwetrottmann.trakt5.services.Movies;
import com.uwetrottmann.trakt5.services.Search;
import com.uwetrottmann.trakt5.services.Shows;
import com.uwetrottmann.trakt5.services.Sync;
import com.uwetrottmann.trakt5.services.Users;

public class TestTraktModule extends TraktModule {

    @Override
    Episodes provideEpisodes(TraktV2 trakt) {
        return null;
    }

    @Override
    Movies provideMovies(TraktV2 trakt) {
        return null;
    }

    @Override
    Shows provideShows(TraktV2 trakt) {
        return null;
    }

    @Override
    Search provideSearch(TraktV2 trakt) {
        return null;
    }

    @Override
    Sync provideSync(TraktV2 trakt) {
        return null;
    }

    @Override
    Users provideUsers(TraktV2 trakt) {
        return null;
    }
}
