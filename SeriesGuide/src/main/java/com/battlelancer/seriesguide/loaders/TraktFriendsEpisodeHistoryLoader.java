package com.battlelancer.seriesguide.loaders;

import android.app.Activity;
import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.adapters.NowAdapter;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.util.TextTools;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt5.entities.Friend;
import com.uwetrottmann.trakt5.entities.HistoryEntry;
import com.uwetrottmann.trakt5.entities.Username;
import com.uwetrottmann.trakt5.enums.Extended;
import com.uwetrottmann.trakt5.enums.HistoryType;
import com.uwetrottmann.trakt5.services.Users;
import dagger.Lazy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

/**
 * Loads trakt friends, then returns the most recently watched episode for each friend.
 */
public class TraktFriendsEpisodeHistoryLoader
        extends GenericSimpleLoader<List<NowAdapter.NowItem>> {

    @Inject Lazy<Users> traktUsers;

    public TraktFriendsEpisodeHistoryLoader(Activity activity) {
        super(activity);
        SgApp.from(activity).getServicesComponent().inject(this);
    }

    @Override
    public List<NowAdapter.NowItem> loadInBackground() {
        if (!TraktCredentials.get(getContext()).hasCredentials()) {
            return null;
        }

        // get all trakt friends
        List<Friend> friends = SgTrakt.executeAuthenticatedCall(getContext(),
                traktUsers.get().friends(Username.ME, Extended.IMAGES), "get friends");
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
                    traktUsers.get().history(new Username(username), HistoryType.EPISODES, 1, 1,
                            Extended.IMAGES, null, null), "get friend episode history");
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
