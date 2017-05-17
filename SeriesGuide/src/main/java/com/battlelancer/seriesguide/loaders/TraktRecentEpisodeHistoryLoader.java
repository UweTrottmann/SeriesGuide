package com.battlelancer.seriesguide.loaders;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.util.SparseArrayCompat;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.adapters.NowAdapter;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt5.entities.HistoryEntry;
import com.uwetrottmann.trakt5.entities.UserSlug;
import com.uwetrottmann.trakt5.enums.HistoryType;
import com.uwetrottmann.trakt5.services.Users;
import dagger.Lazy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Loads last 24 hours of trakt watched episodes, or at least one older episode.
 */
public class TraktRecentEpisodeHistoryLoader
        extends GenericSimpleLoader<TraktRecentEpisodeHistoryLoader.Result> {

    public static final int MAX_HISTORY_SIZE = 25;

    public static class Result {
        public List<NowAdapter.NowItem> items;
        @Nullable public String errorText;

        public Result(List<NowAdapter.NowItem> items) {
            this(items, null);
        }

        public Result(List<NowAdapter.NowItem> items, @Nullable String errorText) {
            this.items = items;
            this.errorText = errorText;
        }
    }

    @Inject Lazy<Users> traktUsers;

    public TraktRecentEpisodeHistoryLoader(Activity activity) {
        super(activity);
        SgApp.from(activity).getServicesComponent().inject(this);
    }

    @Override
    @NonNull
    public Result loadInBackground() {
        if (!TraktCredentials.get(getContext()).hasCredentials()) {
            return buildResultFailure(R.string.trakt_error_credentials);
        }

        List<HistoryEntry> history = null;
        try {
            Response<List<HistoryEntry>> response = buildCall().execute();
            if (response.isSuccessful()) {
                history = response.body();
            } else {
                if (SgTrakt.isUnauthorized(getContext(), response)) {
                    return buildResultFailure(R.string.trakt_error_credentials);
                }
                SgTrakt.trackFailedRequest(getContext(), getAction(), response);
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(getContext(), getAction(), e);
            return AndroidUtils.isNetworkConnected(getContext())
                    ? buildResultFailure() : buildResultFailure(R.string.offline);
        }

        if (history == null) {
            return buildResultFailure();
        } else if (history.isEmpty()) {
            return new Result(null); // no history available (yet)
        }

        // add header
        List<NowAdapter.NowItem> items = new ArrayList<>();
        items.add(new NowAdapter.NowItem().header(
                getContext().getString(R.string.recently_watched)));
        // add items
        addItems(items, history);
        // add link to more history
        items.add(new NowAdapter.NowItem().moreLink(getContext().getString(R.string.user_stream)));

        return new Result(items);
    }

    protected void addItems(List<NowAdapter.NowItem> items, List<HistoryEntry> history) {
        SparseArrayCompat<String> localShows = ShowTools.getShowTvdbIdsAndPosters(getContext());
        long timeDayAgo = System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS;
        for (int i = 0, size = history.size(); i < size; i++) {
            HistoryEntry entry = history.get(i);

            if (entry.episode == null || entry.episode.ids == null || entry.episode.ids.tvdb == null
                    || entry.show == null || entry.watched_at == null) {
                // missing required values
                continue;
            }

            // only include episodes watched in the last 24 hours
            // however, include at least one older episode if there are none, yet
            if (TimeTools.isBeforeMillis(entry.watched_at, timeDayAgo) && items.size() > 1) {
                break;
            }

            // look for a TVDB poster
            String posterUrl;
            Integer showTvdbId = entry.show.ids == null ? null : entry.show.ids.tvdb;
            if (showTvdbId != null && localShows != null) {
                // prefer poster of already added show, fall back to first uploaded poster
                posterUrl = TvdbImageTools.smallSizeOrFirstUrl(localShows.get(showTvdbId),
                        showTvdbId);
            } else {
                posterUrl = null;
            }

            String description = (entry.episode.season == null || entry.episode.number == null)
                    ? entry.episode.title
                    : TextTools.getNextEpisodeString(getContext(), entry.episode.season,
                            entry.episode.number, entry.episode.title);
            NowAdapter.NowItem item = new NowAdapter.NowItem()
                    .displayData(
                            entry.watched_at.toInstant().toEpochMilli(),
                            entry.show.title,
                            description,
                            posterUrl
                    )
                    .tvdbIds(entry.episode.ids.tvdb, showTvdbId)
                    .recentlyWatchedTrakt(entry.action);
            items.add(item);
        }
    }

    @NonNull
    protected String getAction() {
        return "get user episode history";
    }

    protected Call<List<HistoryEntry>> buildCall() {
        return buildUserEpisodeHistoryCall(traktUsers.get());
    }

    public static Call<List<HistoryEntry>> buildUserEpisodeHistoryCall(Users traktUsers) {
        return traktUsers.history(UserSlug.ME, HistoryType.EPISODES, 1, MAX_HISTORY_SIZE,
                null, null, null);
    }

    private Result buildResultFailure() {
        return new Result(null, getContext().getString(R.string.api_error_generic,
                getContext().getString(R.string.trakt)));
    }

    private Result buildResultFailure(@StringRes int emptyTextResId) {
        return new Result(null, getContext().getString(emptyTextResId));
    }
}
