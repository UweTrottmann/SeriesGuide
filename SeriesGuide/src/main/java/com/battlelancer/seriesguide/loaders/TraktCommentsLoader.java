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
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.ui.TraktCommentsFragment;
import com.battlelancer.seriesguide.util.MovieTools;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TraktTools;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.Comment;
import com.uwetrottmann.trakt.v2.enums.Extended;
import java.util.List;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * Loads up comments from trakt for a movie (tvdbId arg is 0), show (episode arg is 0) or episode.
 */
public class TraktCommentsLoader extends GenericSimpleLoader<TraktCommentsLoader.Result> {

    public static class Result {
        public List<Comment> results;
        public int emptyTextResId;

        public Result(List<Comment> results, int emptyTextResId) {
            this.results = results;
            this.emptyTextResId = emptyTextResId;
        }
    }

    private static final int PAGE_SIZE = 25;
    private Bundle mArgs;

    public TraktCommentsLoader(Context context, Bundle args) {
        super(context);
        mArgs = args;
    }

    @Override
    public Result loadInBackground() {
        TraktV2 trakt = ServiceUtils.getTraktV2(getContext());
        try {
            // movie comments?
            int movieTmdbId = mArgs.getInt(TraktCommentsFragment.InitBundle.MOVIE_TMDB_ID);
            if (movieTmdbId != 0) {
                Integer movieTraktId = MovieTools.lookupTraktId(trakt.search(), movieTmdbId);
                if (movieTraktId == null) {
                    return buildResultFailure(R.string.trakt_error_general);
                }

                List<Comment> comments = trakt.movies().comments(String.valueOf(movieTraktId), 1,
                        PAGE_SIZE, Extended.IMAGES);
                return buildResultSuccess(comments);
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
                    if (showTraktId == null) {
                        return buildResultFailure(R.string.trakt_error_general);
                    }

                    List<Comment> comments = trakt.episodes().comments(showTraktId, season, episode,
                            1, PAGE_SIZE, Extended.IMAGES);
                    return buildResultSuccess(comments);
                } else {
                    Timber.e("loadInBackground: could not find episode in database");
                    return buildResultFailure(R.string.unknown);
                }
            }

            // show comments!
            int showTvdbId = mArgs.getInt(TraktCommentsFragment.InitBundle.SHOW_TVDB_ID);
            String showTraktId = TraktTools.lookupShowTraktId(getContext(), showTvdbId);
            if (showTraktId == null) {
                return buildResultFailure(R.string.trakt_error_general);
            }

            List<Comment> comments = trakt.shows().comments(showTraktId, 1, PAGE_SIZE,
                    Extended.IMAGES);
            return buildResultSuccess(comments);
        } catch (RetrofitError e) {
            Timber.e(e, "Loading comments failed");
            return buildResultFailure(AndroidUtils.isNetworkConnected(getContext())
                    ? R.string.trakt_error_general : R.string.offline);
        }
    }

    private static Result buildResultSuccess(List<Comment> results) {
        return new Result(results, R.string.no_shouts);
    }

    private static Result buildResultFailure(int emptyTextResId) {
        return new Result(null, emptyTextResId);
    }
}
