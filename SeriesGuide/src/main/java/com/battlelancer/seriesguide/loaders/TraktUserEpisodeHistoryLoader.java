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
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.HistoryEntry;
import com.uwetrottmann.trakt.v2.enums.Extended;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import java.util.LinkedList;
import java.util.List;
import android.support.annotation.NonNull;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * Loads last 24 hours of trakt watched episodes, or at least one older episode.
 */
public class TraktUserEpisodeHistoryLoader
        extends GenericSimpleLoader<TraktUserEpisodeHistoryLoader.Result> {

    public static class Result {
        public List<NowAdapter.NowItem> items;
        public @StringRes int errorTextResId;

        public Result(List<NowAdapter.NowItem> items, @StringRes int errorTextResId) {
            this.items = items;
            this.errorTextResId = errorTextResId;
        }
    }

    private static final int MAX_HISTORY_SIZE = 25;

    public TraktUserEpisodeHistoryLoader(Context context) {
        super(context);
    }

    @Override
    @NonNull
    public Result loadInBackground() {
        TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(getContext());
        if (trakt == null) {
            return buildResultFailure(R.string.trakt_error_credentials);
        }

        List<HistoryEntry> history;
        try {
            history = trakt.users().historyEpisodes("me", 1, MAX_HISTORY_SIZE, Extended.IMAGES);
        } catch (RetrofitError e) {
            Timber.e(e, "Loading user episode history failed");
            return buildResultFailure(
                    AndroidUtils.isNetworkConnected(getContext()) ? R.string.trakt_error_general
                            : R.string.offline);
        } catch (OAuthUnauthorizedException e) {
            TraktCredentials.get(getContext()).setCredentialsInvalid();
            return buildResultFailure(R.string.trakt_error_credentials);
        }

        if (history == null) {
            return buildResultFailure(R.string.trakt_error_general);
        } else if (history.isEmpty()) {
            // no history available (yet)
            return new Result(null, 0);
        }

        // add header
        List<NowAdapter.NowItem> items = new LinkedList<>();
        items.add(new NowAdapter.NowItem().header(
                getContext().getString(R.string.recently_watched)));

        // add episodes
        long timeDayAgo = System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS;
        for (int i = 0; i < history.size(); i++) {
            HistoryEntry entry = history.get(i);

            if (entry.episode == null || entry.episode.ids == null || entry.episode.ids.tvdb == null
                    || entry.show == null || entry.watched_at == null) {
                // missing required values
                continue;
            }

            // only include episodes watched in the last 24 hours
            // however, include at least one older episode if there are none, yet
            if (entry.watched_at.isBefore(timeDayAgo) && items.size() > 1) {
                break;
            }

            String poster = (entry.show.images == null || entry.show.images.poster == null)
                    ? null : entry.show.images.poster.thumb;
            String description = (entry.episode.season == null || entry.episode.number == null)
                    ? entry.episode.title
                    : Utils.getNextEpisodeString(getContext(), entry.episode.season,
                            entry.episode.number, entry.episode.title);
            NowAdapter.NowItem item = new NowAdapter.NowItem()
                    .displayData(
                            entry.watched_at.getMillis(),
                            entry.show.title,
                            description,
                            poster
                    )
                    .tvdbIds(entry.episode.ids.tvdb,
                            entry.show.ids == null ? null : entry.show.ids.tvdb)
                    .recentlyWatchedTrakt(entry.action);
            items.add(item);
        }

        // add link to more history
        items.add(new NowAdapter.NowItem().moreLink(getContext().getString(R.string.user_stream)));

        return new Result(items, 0);
    }

    private static Result buildResultFailure(int emptyTextResId) {
        return new Result(null, emptyTextResId);
    }
}
