package com.battlelancer.seriesguide.ui.shows;

import android.app.Activity;
import android.text.format.DateUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.collection.SparseArrayCompat;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.provider.SgEpisode2Helper;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.traktapi.TraktCredentials;
import com.battlelancer.seriesguide.util.Errors;
import com.battlelancer.seriesguide.util.ImageTools;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt5.entities.HistoryEntry;
import com.uwetrottmann.trakt5.entities.UserSlug;
import com.uwetrottmann.trakt5.enums.HistoryType;
import com.uwetrottmann.trakt5.services.Users;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Loads last 24 hours of trakt watched episodes, or at least one older episode.
 */
public class TraktRecentEpisodeHistoryLoader
        extends GenericSimpleLoader<TraktRecentEpisodeHistoryLoader.Result> {

    protected static final int MAX_HISTORY_SIZE = 10;

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

    public TraktRecentEpisodeHistoryLoader(Activity activity) {
        super(activity);
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
                Errors.logAndReport(getAction(), response);
            }
        } catch (Exception e) {
            Errors.logAndReport(getAction(), e);
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
        SparseArrayCompat<String> tmdbIdsToPoster = SgApp.getServicesComponent(getContext())
                .showTools().getTmdbIdsToPoster();
        SgEpisode2Helper episodeHelper = SgRoomDatabase.getInstance(getContext())
                .sgEpisode2Helper();
        long timeDayAgo = System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS;

        for (int i = 0, size = history.size(); i < size; i++) {
            HistoryEntry entry = history.get(i);

            if (entry.episode == null || entry.show == null || entry.watched_at == null) {
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
            Integer showTmdbId = entry.show.ids == null ? null : entry.show.ids.tmdb;
            if (showTmdbId != null) {
                // prefer poster of already added show, fall back to first uploaded poster
                posterUrl = ImageTools.posterUrlOrResolve(tmdbIdsToPoster.get(showTmdbId),
                        showTmdbId, DisplaySettings.LANGUAGE_EN, getContext());
            } else {
                posterUrl = null;
            }

            String description = (entry.episode.season == null || entry.episode.number == null)
                    ? entry.episode.title
                    : TextTools.getNextEpisodeString(getContext(), entry.episode.season,
                            entry.episode.number, entry.episode.title);

            Integer episodeTmdbIdOrNull = entry.episode.ids != null ? entry.episode.ids.tmdb : null;
            long localEpisodeIdOrZero = episodeTmdbIdOrNull != null
                    ? episodeHelper.getEpisodeIdByTmdbId(episodeTmdbIdOrNull) : 0;

            NowAdapter.NowItem item = new NowAdapter.NowItem()
                    .displayData(
                            entry.watched_at.toInstant().toEpochMilli(),
                            entry.show.title,
                            description,
                            posterUrl
                    )
                    .episodeIds(localEpisodeIdOrZero, showTmdbId != null ? showTmdbId : 0)
                    .recentlyWatchedTrakt(entry.action);
            items.add(item);
        }
    }

    @NonNull
    protected String getAction() {
        return "get user episode history";
    }

    protected Call<List<HistoryEntry>> buildCall() {
        Users traktUsers = SgApp.getServicesComponent(getContext()).traktUsers();
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
