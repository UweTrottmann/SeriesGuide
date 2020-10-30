package com.battlelancer.seriesguide.ui.movies;

import android.app.Activity;
import android.text.format.DateUtils;
import androidx.annotation.NonNull;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.ui.shows.NowAdapter;
import com.battlelancer.seriesguide.ui.shows.TraktRecentEpisodeHistoryLoader;
import com.battlelancer.seriesguide.util.TimeTools;
import com.uwetrottmann.trakt5.entities.HistoryEntry;
import com.uwetrottmann.trakt5.entities.UserSlug;
import com.uwetrottmann.trakt5.enums.HistoryType;
import com.uwetrottmann.trakt5.services.Users;
import java.util.List;
import retrofit2.Call;

/**
 * Loads last 72 hours of trakt watched movies, or at least one older watched movie.
 */
public class TraktRecentMovieHistoryLoader extends TraktRecentEpisodeHistoryLoader {

    TraktRecentMovieHistoryLoader(Activity activity) {
        super(activity);
    }

    @Override
    protected void addItems(List<NowAdapter.NowItem> items, List<HistoryEntry> history) {
        // add movies
        long threeDaysAgo = System.currentTimeMillis() - 3 * DateUtils.DAY_IN_MILLIS;
        for (int i = 0, size = history.size(); i < size; i++) {
            HistoryEntry entry = history.get(i);

            if (entry.movie == null || entry.movie.ids == null || entry.movie.ids.tmdb == null
                    || entry.watched_at == null) {
                // missing required values
                continue;
            }

            // only include movies watched in the last 72 hours
            // however, include at least one older one if there are none
            if (TimeTools.isBeforeMillis(entry.watched_at, threeDaysAgo) && items.size() > 1) {
                break;
            }

            // trakt has removed image support: currently displaying no image
            items.add(new NowAdapter.NowItem()
                    .displayData(
                            entry.watched_at.toInstant().toEpochMilli(),
                            entry.movie.title,
                            null,
                            null
                    )
                    .tmdbId(entry.movie.ids.tmdb)
                    .recentlyWatchedTrakt(entry.action)
            );
        }
    }

    @NonNull
    @Override
    protected String getAction() {
        return "get user movie history";
    }

    @Override
    protected Call<List<HistoryEntry>> buildCall() {
        Users traktUsers = SgApp.getServicesComponent(getContext()).traktUsers();
        return traktUsers.history(UserSlug.ME, HistoryType.MOVIES, 1, MAX_HISTORY_SIZE,
                null, null, null);
    }
}
