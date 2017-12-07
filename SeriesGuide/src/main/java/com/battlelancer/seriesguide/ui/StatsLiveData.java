package com.battlelancer.seriesguide.ui;

import android.annotation.SuppressLint;
import android.arch.lifecycle.LiveData;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.SparseIntArray;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.ShowTools;

public class StatsLiveData extends LiveData<StatsLiveData.StatsUpdateEvent> {

    private final Context context;
    private AsyncTask<Void, StatsUpdateEvent, StatsUpdateEvent> task;

    public StatsLiveData(Context context) {
        this.context = context;
    }

    public void loadStats() {
        if (task == null || task.getStatus() == AsyncTask.Status.FINISHED) {
            task = new StatsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class StatsTask extends AsyncTask<Void, StatsUpdateEvent, StatsUpdateEvent> {

        private static final long PREVIEW_UPDATE_INTERVAL_MS = DateUtils.SECOND_IN_MILLIS;

        @Nullable
        @Override
        protected StatsUpdateEvent doInBackground(Void... params) {
            Stats stats = new Stats();
            ContentResolver resolver = context.getContentResolver();

            // movies
            if (!processMovies(resolver, stats)) {
                return buildFailure(stats); // failed to process movies
            }

            if (isCancelled()) {
                return buildFailure(stats);
            }

            // shows
            SparseIntArray showRuntimes = processShows(resolver, stats);
            if (showRuntimes == null) {
                return buildFailure(stats); // failed to process shows
            }

            if (isCancelled()) {
                return buildFailure(stats);
            }

            // episodes
            boolean includeSpecials = !DisplaySettings.isHidingSpecials(context);
            if (!processEpisodes(resolver, stats, includeSpecials)) {
                return buildFailure(stats); // failed to process episodes
            }

            if (isCancelled()) {
                return buildFailure(stats);
            }

            // report intermediate results before longest op
            publishProgress(buildUpdate(stats));

            // calculate runtime of watched episodes per show
            long totalRuntimeMin = 0;
            long previewTime = System.currentTimeMillis() + PREVIEW_UPDATE_INTERVAL_MS;
            for (int i = 0, size = showRuntimes.size(); i < size; i++) {
                int showTvdbId = showRuntimes.keyAt(i);
                long runtimeOfShowMin = showRuntimes.valueAt(i);

                int watchedEpisodesOfShowCount = DBUtils.getCountOf(resolver,
                        Episodes.buildEpisodesOfShowUri(showTvdbId),
                        Episodes.SELECTION_WATCHED
                                + (includeSpecials ? "" : " AND " + Episodes.SELECTION_NO_SPECIALS),
                        null, -1);
                if (watchedEpisodesOfShowCount == -1) {
                    // episode query failed, return what we have so far
                    stats.episodesWatchedRuntime(totalRuntimeMin * DateUtils.MINUTE_IN_MILLIS);
                    return buildFailure(stats);
                }
                // make sure we calculate with long here (first arg is long) to avoid overflows
                long runtimeOfEpisodesMin = runtimeOfShowMin * watchedEpisodesOfShowCount;

                totalRuntimeMin += runtimeOfEpisodesMin;

                // post regular update of minimum
                long currentTime = System.currentTimeMillis();
                if (currentTime > previewTime) {
                    previewTime = currentTime + PREVIEW_UPDATE_INTERVAL_MS;
                    stats.episodesWatchedRuntime(totalRuntimeMin * DateUtils.MINUTE_IN_MILLIS);
                    publishProgress(buildUpdate(stats));
                }
            }
            stats.episodesWatchedRuntime(totalRuntimeMin * DateUtils.MINUTE_IN_MILLIS);

            // return final values
            return new StatsUpdateEvent(stats, true, true);
        }

        private StatsUpdateEvent buildFailure(Stats stats) {
            return new StatsUpdateEvent(stats, false, false);
        }

        private StatsUpdateEvent buildUpdate(Stats stats) {
            return new StatsUpdateEvent(stats, false, true);
        }

        @Override
        protected void onProgressUpdate(StatsUpdateEvent... values) {
            setValue(values[0]);
        }

        @Override
        protected void onPostExecute(StatsUpdateEvent event) {
            setValue(event);
        }

        private boolean processMovies(ContentResolver resolver, Stats stats) {
            // movies (count, in watchlist, runtime of watchlist)
            final Cursor movies = resolver.query(SeriesGuideContract.Movies.CONTENT_URI,
                    new String[] { SeriesGuideContract.Movies._ID,
                            SeriesGuideContract.Movies.IN_WATCHLIST,
                            SeriesGuideContract.Movies.RUNTIME_MIN }, null, null, null
            );
            if (movies == null) {
                return false;
            }
            stats.movies = movies.getCount();

            int inWatchlist = 0;
            long watchlistRuntime = 0;
            while (movies.moveToNext()) {
                if (movies.getInt(1) == 1) {
                    inWatchlist++;
                    watchlistRuntime += movies.getInt(2) * DateUtils.MINUTE_IN_MILLIS;
                }
            }
            movies.close();

            stats.moviesWatchlist = inWatchlist;
            stats.moviesWatchlistRuntime = watchlistRuntime;
            return true;
        }

        @Nullable
        private SparseIntArray processShows(ContentResolver resolver, Stats stats) {
            Cursor shows = resolver.query(Shows.CONTENT_URI,
                    new String[] {
                            Shows._ID, // 0
                            Shows.STATUS,
                            Shows.NEXTEPISODE,
                            Shows.RUNTIME // 3
                    }, null, null, null
            );
            if (shows == null) {
                return null;
            }

            int continuing = 0;
            int withnext = 0;
            // count all shows
            int showsCount = shows.getCount();
            SparseIntArray showRuntimes = new SparseIntArray(showsCount);
            while (shows.moveToNext()) {
                // count continuing shows
                if (shows.getInt(1) == ShowTools.Status.CONTINUING) {
                    continuing++;
                }
                // count shows with next episodes
                if (shows.getInt(2) != ShowTools.Status.ENDED) {
                    withnext++;
                }
                // map show to its runtime
                showRuntimes.put(shows.getInt(0), shows.getInt(3));
            }
            shows.close();

            stats.shows(showsCount)
                    .showsContinuing(continuing)
                    .showsWithNextEpisodes(withnext);
            return showRuntimes;
        }

        private boolean processEpisodes(ContentResolver resolver, Stats stats,
                boolean includeSpecials) {
            // all episodes
            int allEpisodesCount = DBUtils.getCountOf(resolver, Episodes.CONTENT_URI,
                    includeSpecials ? null : Episodes.SELECTION_NO_SPECIALS, null, -1);
            if (allEpisodesCount == -1) {
                return false;
            }
            stats.episodes(allEpisodesCount);

            // watched episodes
            int watchedEpisodesCount = DBUtils.getCountOf(resolver, Episodes.CONTENT_URI,
                    Episodes.SELECTION_WATCHED
                            + (includeSpecials ? "" : " AND " + Episodes.SELECTION_NO_SPECIALS),
                    null, -1);
            if (watchedEpisodesCount == -1) {
                return false;
            }
            stats.episodesWatched(watchedEpisodesCount);

            return true;
        }
    }

    public static class StatsUpdateEvent {
        @NonNull public final Stats stats;
        public final boolean finalValues;
        public final boolean successful;

        public StatsUpdateEvent(@NonNull Stats stats, boolean finalValues,
                boolean successful) {
            this.stats = stats;
            this.finalValues = finalValues;
            this.successful = successful;
        }
    }

    public static class Stats {
        private int shows;
        private int showsContinuing;
        private int showsWithNext;
        private int episodes;
        private int episodesWatched;
        private long episodesWatchedRuntime;
        public int movies;
        public int moviesWatchlist;
        public long moviesWatchlistRuntime;

        public int shows() {
            return shows;
        }

        public Stats shows(int number) {
            shows = number;
            return this;
        }

        public int showsWithNextEpisodes() {
            return showsWithNext;
        }

        public Stats showsWithNextEpisodes(int number) {
            showsWithNext = number;
            return this;
        }

        public int showsContinuing() {
            return showsContinuing;
        }

        public Stats showsContinuing(int number) {
            showsContinuing = number;
            return this;
        }

        public int episodes() {
            return episodes;
        }

        public Stats episodes(int number) {
            episodes = number;
            return this;
        }

        public long episodesWatchedRuntime() {
            return episodesWatchedRuntime;
        }

        public Stats episodesWatchedRuntime(long runtime) {
            episodesWatchedRuntime = runtime;
            return this;
        }

        public int episodesWatched() {
            return episodesWatched;
        }

        public Stats episodesWatched(int number) {
            episodesWatched = number;
            return this;
        }
    }

}
