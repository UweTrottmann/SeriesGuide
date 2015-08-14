/*
 * Copyright 2015 Uwe Trottmann
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
import android.support.annotation.StringRes;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.NowAdapter;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.HistoryEntry;
import com.uwetrottmann.trakt.v2.enums.Extended;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import java.util.LinkedList;
import java.util.List;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * Loads last 72 hours of trakt watched movies, or at least one older watched movie.
 */
public class TraktUserMovieHistoryLoader
        extends GenericSimpleLoader<TraktUserMovieHistoryLoader.Result> {

    public static class Result {
        public List<NowAdapter.NowItem> items;
        public @StringRes int errorTextResId;

        public Result(List<NowAdapter.NowItem> items, @StringRes int errorTextResId) {
            this.items = items;
            this.errorTextResId = errorTextResId;
        }
    }

    private static final int MAX_HISTORY_SIZE = 25;

    public TraktUserMovieHistoryLoader(Context context) {
        super(context);
    }

    @Override
    public Result loadInBackground() {
        TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(getContext());
        if (trakt == null) {
            return buildResultFailure(R.string.trakt_error_credentials);
        }

        List<HistoryEntry> history;
        try {
            history = trakt.users().historyMovies("me", 1, MAX_HISTORY_SIZE, Extended.IMAGES);
        } catch (RetrofitError e) {
            Timber.e(e, "Loading user movie history failed");
            return buildResultFailure(AndroidUtils.isNetworkConnected(getContext())
                    ? R.string.trakt_error_general : R.string.offline);
        } catch (OAuthUnauthorizedException e) {
            TraktCredentials.get(getContext()).setCredentialsInvalid();
            return buildResultFailure(R.string.trakt_error_credentials);
        }

        if (history == null) {
            Timber.e("Loading user movie history failed, was null");
            return buildResultFailure(R.string.trakt_error_general);
        } else if (history.isEmpty()) {
            // no history available (yet)
            return new Result(null, 0);
        }

        List<NowAdapter.NowItem> items = new LinkedList<>();
        items.add(
                new NowAdapter.NowItem().header(getContext().getString(R.string.recently_watched)));

        // add movies
        long threeDaysAgo = System.currentTimeMillis() - 3 * DateUtils.DAY_IN_MILLIS;
        for (int i = 0; i < history.size(); i++) {
            HistoryEntry entry = history.get(i);

            if (entry.movie == null || entry.movie.ids == null || entry.movie.ids.tmdb == null
                    || entry.watched_at == null) {
                // missing required values
                continue;
            }

            // only include movies watched in the last 72 hours
            // however, include at least one older one if there are none
            if (entry.watched_at.isBefore(threeDaysAgo) && items.size() > 1) {
                break;
            }

            String poster = (entry.movie.images == null || entry.movie.images.poster == null) ? null
                    : entry.movie.images.poster.thumb;
            items.add(new NowAdapter.NowItem()
                            .displayData(
                                    entry.watched_at.getMillis(),
                                    "",
                                    entry.movie.title,
                                    poster
                            )
                            .tmdbId(entry.movie.ids.tmdb)
                            .recentlyWatchedTrakt(entry.action)
            );
        }

        // add link to more history
        items.add(new NowAdapter.NowItem().moreLink(getContext().getString(R.string.user_stream)));

        return new Result(items, 0);
    }

    private static Result buildResultFailure(int emptyTextResId) {
        return new Result(null, emptyTextResId);
    }
}
