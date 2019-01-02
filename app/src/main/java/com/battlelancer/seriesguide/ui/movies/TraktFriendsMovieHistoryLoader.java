package com.battlelancer.seriesguide.ui.movies;

import android.app.Activity;
import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.ui.shows.NowAdapter;
import com.battlelancer.seriesguide.traktapi.TraktCredentials;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
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
 * Loads trakt friends, then returns the most recently watched movie for each friend.
 */
class TraktFriendsMovieHistoryLoader extends GenericSimpleLoader<List<NowAdapter.NowItem>> {

    TraktFriendsMovieHistoryLoader(Activity activity) {
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

        // add last watched movie for each friend
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
            List<HistoryEntry> history = SgTrakt.executeCall(
                    traktUsers.history(new UserSlug(userSlug), HistoryType.MOVIES, 1, 1,
                            null, null, null), "get friend movie history");
            if (history == null || history.size() == 0) {
                continue; // no history
            }

            HistoryEntry entry = history.get(0);
            if (entry.watched_at == null || entry.movie == null) {
                // missing required values
                continue;
            }

            String avatar = (friend.user.images == null || friend.user.images.avatar == null)
                    ? null : friend.user.images.avatar.full;
            // trakt has removed image support: currently displaying no image
            NowAdapter.NowItem nowItem = new NowAdapter.NowItem().
                    displayData(
                            entry.watched_at.toInstant().toEpochMilli(),
                            entry.movie.title,
                            null,
                            null
                    )
                    .tmdbId(entry.movie.ids == null ? null : entry.movie.ids.tmdb)
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
