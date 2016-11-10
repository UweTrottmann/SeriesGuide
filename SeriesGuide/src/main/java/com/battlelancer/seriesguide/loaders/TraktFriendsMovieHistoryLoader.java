package com.battlelancer.seriesguide.loaders;

import android.app.Activity;
import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.adapters.NowAdapter;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
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
 * Loads trakt friends, then returns the most recently watched movie for each friend.
 */
public class TraktFriendsMovieHistoryLoader extends GenericSimpleLoader<List<NowAdapter.NowItem>> {

    @Inject Lazy<Users> traktUsers;

    public TraktFriendsMovieHistoryLoader(Activity activity) {
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
                traktUsers.get().friends(Username.ME, Extended.FULL), "get friends");
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
                    traktUsers.get().history(new Username(username), HistoryType.MOVIES, 1, 1,
                            Extended.DEFAULT_MIN, null, null), "get friend movie history");
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
                            entry.watched_at.getMillis(),
                            entry.movie.title,
                            null,
                            null
                    )
                    .tmdbId(entry.movie.ids == null ? null : entry.movie.ids.tmdb)
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
