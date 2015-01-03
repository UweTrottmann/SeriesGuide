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
import android.database.Cursor;
import android.os.Bundle;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.ui.TraktCommentsFragment;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TraktTools;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.Comment;
import com.uwetrottmann.trakt.v2.entities.SearchResult;
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
                        .idLookup(IdType.TMDB, String.valueOf(movieTmdbId), 1, 10);
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

            // episode comments?
            int episodeTvdbId = mArgs.getInt(TraktCommentsFragment.InitBundle.EPISODE_TVDB_ID);
            if (episodeTvdbId != 0) {
                // look up episode number, season and show id
                Cursor query = getContext().getContentResolver()
                        .query(SeriesGuideContract.Episodes.buildEpisodeUri(episodeTvdbId),
                                new String[] { SeriesGuideContract.Episodes.SEASON,
                                        SeriesGuideContract.Episodes.NUMBER,
                                        SeriesGuideContract.Shows.REF_SHOW_ID }, null, null, null);
                int season = -1;
                int episode = -1;
                int showTvdbId = -1;
                if (query != null) {
                    if (query.moveToFirst()) {
                        season = query.getInt(0);
                        episode = query.getInt(1);
                        showTvdbId = query.getInt(2);
                    }
                    query.close();
                }

                if (season != -1 && episode != -1 && showTvdbId != -1) {
                    // look up show trakt id
                    String showTraktId = TraktTools.lookupShowTraktId(getContext(), showTvdbId);
                    if (showTraktId != null) {
                        return trakt.episodes().comments(showTraktId, season, episode, 1, PAGE_SIZE,
                                Extended.IMAGES);
                    }
                    return null;
                } else {
                    Timber.e("loadInBackground: could not find episode in database");
                    return null;
                }
            }

            // show comments!
            int showTvdbId = mArgs.getInt(TraktCommentsFragment.InitBundle.SHOW_TVDB_ID);
            String showTraktId = TraktTools.lookupShowTraktId(getContext(), showTvdbId);
            if (showTraktId != null) {
                return trakt.shows().comments(showTraktId, 1, PAGE_SIZE, Extended.IMAGES);
            }
        } catch (RetrofitError e) {
            Timber.e(e, "Loading comments failed");
        }

        return null;
    }
}
