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
import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.NowAdapter;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.Friend;
import com.uwetrottmann.trakt.v2.entities.HistoryEntry;
import com.uwetrottmann.trakt.v2.enums.Extended;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import com.uwetrottmann.trakt.v2.services.Users;
import java.util.ArrayList;
import java.util.List;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * Loads trakt friends, then returns the most recently watched episode for each friend.
 */
public class TraktFriendsEpisodeHistoryLoader extends GenericSimpleLoader<List<NowAdapter.NowItem>> {

    public TraktFriendsEpisodeHistoryLoader(Context context) {
        super(context);
    }

    @Override
    public List<NowAdapter.NowItem> loadInBackground() {
        TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(getContext());
        if (trakt == null) {
            return null;
        }
        Users traktUsers = trakt.users();

        // get all trakt friends
        List<Friend> friends;
        try {
            friends = traktUsers.friends("me", Extended.IMAGES);
        } catch (RetrofitError e) {
            Timber.e(e, "Failed to load trakt friends");
            return null;
        } catch (OAuthUnauthorizedException e) {
            TraktCredentials.get(getContext()).setCredentialsInvalid();
            return null;
        }

        if (friends == null || friends.size() == 0) {
            return null;
        }

        List<NowAdapter.NowItem> items = new ArrayList<>();
        for (int i = 0; i < friends.size(); i++) {
            Friend friend = friends.get(i);

            if (friend.user == null || TextUtils.isEmpty(friend.user.username)) {
                // at least need a username
                continue;
            }

            // get last watched episode
            List<HistoryEntry> history;
            try {
                history = traktUsers.historyEpisodes(friend.user.username, 1, 1, Extended.IMAGES);
            } catch (RetrofitError e) {
                // abort, either lost connection or server error or other error
                Timber.e(e, "Failed to load friend episode history");
                return null;
            } catch (OAuthUnauthorizedException ignored) {
                // friend might have revoked friendship just now :(
                continue;
            }

            if (history == null || history.size() == 0) {
                // no history
                continue;
            }

            HistoryEntry entry = history.get(0);
            if (entry.watched_at == null || entry.episode == null
                    || entry.episode.season == null || entry.episode.number == null
                    || entry.show == null) {
                // missing required values
                continue;
            }

            String poster = (entry.show.images == null || entry.show.images.poster == null)
                    ? null : entry.show.images.poster.thumb;
            String avatar = (friend.user.images == null || friend.user.images.avatar == null)
                    ? null : friend.user.images.avatar.full;
            NowAdapter.NowItem nowItem = new NowAdapter.NowItem().
                    displayData(
                            entry.watched_at.getMillis(),
                            entry.show.title,
                            Utils.getNextEpisodeString(getContext(), entry.episode.season,
                                    entry.episode.number, entry.episode.title),
                            poster
                    )
                    .tvdbIds(entry.episode.ids == null ? null : entry.episode.ids.tvdb,
                            entry.show.ids == null ? null : entry.show.ids.tvdb)
                    .friend(friend.user.username, avatar, entry.action);
            items.add(nowItem);
        }

        // add header
        if (items.size() > 0) {
            items.add(0, new NowAdapter.NowItem().header(
                    getContext().getString(R.string.friends_recently)));
        }

        return items;
    }
}
