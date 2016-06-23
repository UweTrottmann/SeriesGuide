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
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TextTools;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.Friend;
import com.uwetrottmann.trakt5.entities.HistoryEntry;
import com.uwetrottmann.trakt5.entities.Username;
import com.uwetrottmann.trakt5.enums.Extended;
import com.uwetrottmann.trakt5.enums.HistoryType;
import com.uwetrottmann.trakt5.services.Users;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads trakt friends, then returns the most recently watched episode for each friend.
 */
public class TraktFriendsEpisodeHistoryLoader
        extends GenericSimpleLoader<List<NowAdapter.NowItem>> {

    public TraktFriendsEpisodeHistoryLoader(Context context) {
        super(context);
    }

    @Override
    public List<NowAdapter.NowItem> loadInBackground() {
        TraktV2 trakt = ServiceUtils.getTrakt(getContext());
        if (!TraktCredentials.get(getContext()).hasCredentials()) {
            return null;
        }
        Users traktUsers = trakt.users();

        // get all trakt friends
        List<Friend> friends = SgTrakt.executeAuthenticatedCall(getContext(),
                traktUsers.friends(Username.ME, Extended.IMAGES), "get friends");
        if (friends == null) {
            return null;
        }

        int size = friends.size();
        if (size == 0) {
            return null; // no friends, done.
        }

        // estimate list size
        List<NowAdapter.NowItem> items = new ArrayList<>(size + 1);

        // add header
        items.add(
                new NowAdapter.NowItem().header(getContext().getString(R.string.friends_recently)));

        // add last watched episode for each friend
        for (int i = 0; i < size; i++) {
            Friend friend = friends.get(i);

            // at least need a username
            if (friend.user == null) {
                continue;
            }
            String username = friend.user.username;
            if (TextUtils.isEmpty(username)) {
                continue;
            }

            // get last watched episode
            List<HistoryEntry> history = SgTrakt.executeCall(getContext(),
                    traktUsers.history(new Username(username), HistoryType.EPISODES, 1, 1,
                            Extended.IMAGES), "get friend episode history");
            if (history == null || history.size() == 0) {
                continue; // no history
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
                            TextTools.getNextEpisodeString(getContext(), entry.episode.season,
                                    entry.episode.number, entry.episode.title),
                            poster
                    )
                    .tvdbIds(entry.episode.ids == null ? null : entry.episode.ids.tvdb,
                            entry.show.ids == null ? null : entry.show.ids.tvdb)
                    .friend(username, avatar, entry.action);
            items.add(nowItem);
        }

        // only have a header? return nothing
        if (items.size() == 1) {
            return Collections.emptyList();
        }

        return items;
    }
}
