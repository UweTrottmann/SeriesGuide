package com.battlelancer.seriesguide.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.NowAdapter;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TextTools;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.HistoryEntry;
import com.uwetrottmann.trakt5.entities.Username;
import com.uwetrottmann.trakt5.enums.Extended;
import com.uwetrottmann.trakt5.enums.HistoryType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
        public @StringRes int errorTextResId;

        public Result(List<NowAdapter.NowItem> items, @StringRes int errorTextResId) {
            this.items = items;
            this.errorTextResId = errorTextResId;
        }
    }

    public TraktRecentEpisodeHistoryLoader(Context context) {
        super(context);
    }

    @Override
    @NonNull
    public Result loadInBackground() {
        TraktV2 trakt = ServiceUtils.getTrakt(getContext());
        if (!TraktCredentials.get(getContext()).hasCredentials()) {
            return buildResultFailure(R.string.trakt_error_credentials);
        }

        List<HistoryEntry> history = null;
        try {
            Response<List<HistoryEntry>> response = buildCall(trakt).execute();
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
            return buildResultFailure(
                    AndroidUtils.isNetworkConnected(getContext()) ? R.string.trakt_error_general
                            : R.string.offline);
        }

        if (history == null) {
            return buildResultFailure(R.string.trakt_error_general);
        } else if (history.isEmpty()) {
            return new Result(null, 0); // no history available (yet)
        }

        // add header
        List<NowAdapter.NowItem> items = new ArrayList<>();
        items.add(new NowAdapter.NowItem().header(
                getContext().getString(R.string.recently_watched)));
        // add items
        addItems(items, history);
        // add link to more history
        items.add(new NowAdapter.NowItem().moreLink(getContext().getString(R.string.user_stream)));

        return new Result(items, 0);
    }

    protected void addItems(List<NowAdapter.NowItem> items, List<HistoryEntry> history) {
        // add episodes
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
            if (entry.watched_at.isBefore(timeDayAgo) && items.size() > 1) {
                break;
            }

            String poster = (entry.show.images == null || entry.show.images.poster == null)
                    ? null : entry.show.images.poster.thumb;
            String description = (entry.episode.season == null || entry.episode.number == null)
                    ? entry.episode.title
                    : TextTools.getNextEpisodeString(getContext(), entry.episode.season,
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
    }

    @NonNull
    protected String getAction() {
        return "get user episode history";
    }

    protected Call<List<HistoryEntry>> buildCall(TraktV2 trakt) {
        return buildUserEpisodeHistoryCall(trakt);
    }

    public static Call<List<HistoryEntry>> buildUserEpisodeHistoryCall(TraktV2 trakt) {
        return trakt.users()
                .history(Username.ME, HistoryType.EPISODES, 1, MAX_HISTORY_SIZE,
                        Extended.IMAGES, null, null);
    }

    protected static Result buildResultFailure(int emptyTextResId) {
        return new Result(null, emptyTextResId);
    }
}
