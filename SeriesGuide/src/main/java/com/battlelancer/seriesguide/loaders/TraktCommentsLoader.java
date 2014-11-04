/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.loaders;

import android.content.Context;
import android.os.Bundle;
import com.battlelancer.seriesguide.ui.TraktCommentsFragment;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.Comment;
import com.uwetrottmann.trakt.v2.entities.SearchResult;
import com.uwetrottmann.trakt.v2.entities.Show;
import com.uwetrottmann.trakt.v2.enums.Extended;
import com.uwetrottmann.trakt.v2.enums.IdType;
import java.util.List;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * Loads up comments from trakt for a movie (tvdbId arg is 0), show (episode arg is 0) or episode.
 */
public class TraktCommentsLoader extends GenericSimpleLoader<List<Comment>> {

    private static final int PAGE_SIZE = 25;
    private Bundle mArgs;

    public TraktCommentsLoader(Context context, Bundle args) {
        super(context);
        mArgs = args;
    }

    @Override
    public List<Comment> loadInBackground() {
        TraktV2 trakt = ServiceUtils.getTraktV2(getContext());
        try {
            int movieTmdbId = mArgs.getInt(TraktCommentsFragment.InitBundle.MOVIE_TMDB_ID);
            if (movieTmdbId != 0) {
                // movie comments
                List<SearchResult> results = trakt.search()
                        .idLookup(IdType.TMDB, String.valueOf(movieTmdbId));
                if (results != null) {
                    for (SearchResult result : results) {
                        if (result.movie != null) {
                            if (result.movie.ids == null || result.movie.ids.trakt == null) {
                                return null;
                            }
                            return trakt.movies()
                                    .comments(String.valueOf(result.movie.ids.trakt), 1, PAGE_SIZE,
                                            Extended.IMAGES);
                        }
                    }
                }
                return null;
            }

            int showTvdbId = mArgs.getInt(TraktCommentsFragment.InitBundle.SHOW_TVDB_ID);

            // look up show trakt id
            List<SearchResult> searchResults = trakt.search()
                    .idLookup(IdType.TVDB, String.valueOf(showTvdbId));
            if (searchResults == null || searchResults.size() == 0) {
                return null;
            }
            Show show = searchResults.get(0).show;
            if (show == null || show.ids == null || show.ids.trakt == null) {
                return null;
            }
            String showTraktId = String.valueOf(show.ids.trakt);

            int episodeNumber = mArgs.getInt(TraktCommentsFragment.InitBundle.EPISODE_NUMBER);
            if (episodeNumber != 0) {
                // episode comments
                int seasonNumber = mArgs.getInt(TraktCommentsFragment.InitBundle.SEASON_NUMBER);
                return trakt.episodes()
                        .comments(showTraktId, seasonNumber, episodeNumber, Extended.IMAGES);
            }

            // show comments
            return trakt.shows().comments(showTraktId, 1, PAGE_SIZE, Extended.IMAGES);
        } catch (RetrofitError e) {
            Timber.e(e, "Loading comments failed");
        }

        return null;
    }
}
