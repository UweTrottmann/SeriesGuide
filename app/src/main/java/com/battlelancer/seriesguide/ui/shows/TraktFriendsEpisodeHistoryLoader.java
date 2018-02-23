package com.battlelancer.seriesguide.ui.shows;

import android.app.Activity;
import android.support.v4.util.SparseArrayCompat;
import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.traktapi.TraktCredentials;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.util.TextTools;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt5.entities.Friend;
import com.uwetrottmann.trakt5.entities.HistoryEntry;
import com.uwetrottmann.trakt5.entities.UserSlug;
import com.uwetrottmann.trakt5.enums.Extended;
import com.uwetrottmann.trakt5.enums.HistoryType;
import com.uwetrottmann.trakt5.services.Users;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads trakt friends, then returns the most recently watched episode for each friend.
 */
class TraktFriendsEpisodeHistoryLoader extends GenericSimpleLoader<List<NowAdapter.NowItem>> {

    TraktFriendsEpisodeHistoryLoader(Activity activity) {
        super(activity);
    }

    @Override
    public List<NowAdapter.NowItem> loadInBackground() {
        if (!TraktCredentials.get(getContext()).hasCredentials()) {
            return null;
        }

        // get all trakt friends
        Users traktUsers = SgApp.getServicesComponent(getContext()).traktUsers();
        List<Friend> friends = SgTrakt.executeAuthenticatedCall(getContext(),
                traktUsers.friends(UserSlug.ME, Extended.FULL), "get friends");
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
        SparseArrayCompat<String> localShows = ShowTools.getShowTvdbIdsAndPosters(getContext());
        boolean hideTitle = DisplaySettings.preventSpoilers(getContext());
        for (int i = 0; i < size; i++) {
            Friend friend = friends.get(i);

            // at least need a userSlug
            if (friend.user == null) {
                continue;
            }
            String userSlug = friend.user.ids.slug;
            if (TextUtils.isEmpty(userSlug)) {
                continue;
            }

            // get last watched episode
            List<HistoryEntry> history = SgTrakt.executeCall(getContext(),
                    traktUsers.history(new UserSlug(userSlug), HistoryType.EPISODES, 1, 1,
                            null, null, null), "get friend episode history");
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

            // look for a TVDB poster
            String posterUrl;
            Integer showTvdbId = entry.show.ids == null ? null : entry.show.ids.tvdb;
            if (showTvdbId != null && localShows != null) {
                // prefer poster of already added show, fall back to first uploaded poster
                posterUrl = TvdbImageTools.smallSizeOrResolveUrl(localShows.get(showTvdbId),
                        showTvdbId);
            } else {
                posterUrl = null;
            }

            String avatar = (friend.user.images == null || friend.user.images.avatar == null)
                    ? null : friend.user.images.avatar.full;
            String episodeString = TextTools.getNextEpisodeString(getContext(),
                    entry.episode.season, entry.episode.number,
                    hideTitle ? null : entry.episode.title);
            NowAdapter.NowItem nowItem = new NowAdapter.NowItem().
                    displayData(
                            entry.watched_at.toInstant().toEpochMilli(),
                            entry.show.title,
                            episodeString,
                            posterUrl
                    )
                    .tvdbIds(entry.episode.ids == null ? null : entry.episode.ids.tvdb, showTvdbId)
                    .friend(friend.user.username, avatar, entry.action);
            items.add(nowItem);
        }

        // only have a header? return nothing
        if (items.size() == 1) {
            return Collections.emptyList();
        }

        return items;
    }
}
