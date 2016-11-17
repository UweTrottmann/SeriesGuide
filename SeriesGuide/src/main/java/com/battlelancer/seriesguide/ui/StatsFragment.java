package com.battlelancer.seriesguide.ui;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.os.AsyncTaskCompat;
import android.text.format.DateUtils;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.widgets.EmptyView;
import java.util.Locale;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Displays some statistics about the users show database, e.g. number of shows, episodes, share of
 * watched episodes, etc.
 */
public class StatsFragment extends Fragment {

    @BindView(R.id.emptyViewStats) EmptyView errorView;

    @BindView(R.id.textViewStatsShows) TextView mShowCount;
    @BindView(R.id.textViewStatsShowsWithNext) TextView mShowsWithNextEpisode;
    @BindView(R.id.progressBarStatsShowsWithNext) ProgressBar mProgressShowsWithNextEpisode;
    @BindView(R.id.textViewStatsShowsContinuing) TextView mShowsContinuing;
    @BindView(R.id.progressBarStatsShowsContinuing) ProgressBar mProgressShowsContinuing;

    @BindView(R.id.textViewStatsEpisodes) TextView mEpisodeCount;
    @BindView(R.id.textViewStatsEpisodesWatched) TextView mEpisodesWatched;
    @BindView(R.id.progressBarStatsEpisodesWatched) ProgressBar mProgressEpisodesWatched;
    @BindView(R.id.textViewStatsEpisodesRuntime) TextView mEpisodesRuntime;
    @BindView(R.id.progressBarStatsEpisodesRuntime) ProgressBar mProgressEpisodesRuntime;

    @BindView(R.id.textViewStatsMovies) TextView mMovieCount;
    @BindView(R.id.textViewStatsMoviesWatchlist) TextView mMoviesWatchlist;
    @BindView(R.id.progressBarStatsMoviesWatchlist) ProgressBar mProgressMoviesWatchlist;
    @BindView(R.id.textViewStatsMoviesWatchlistRuntime) TextView mMoviesWatchlistRuntime;

    private Unbinder unbinder;
    private AsyncTask<Void, StatsUpdateEvent, StatsUpdateEvent> statsTask;
    private Stats currentStats;
    private boolean hasFinalValues;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_stats, container, false);
        unbinder = ButterKnife.bind(this, v);

        errorView.setVisibility(View.GONE);
        errorView.setButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadStatsKeepExisting();
            }
        });

        // set some views invisible so they can be animated in once stats are computed
        mShowsWithNextEpisode.setVisibility(View.INVISIBLE);
        mProgressShowsWithNextEpisode.setVisibility(View.INVISIBLE);
        mShowsContinuing.setVisibility(View.INVISIBLE);
        mProgressShowsContinuing.setVisibility(View.INVISIBLE);

        mEpisodesWatched.setVisibility(View.INVISIBLE);
        mProgressEpisodesWatched.setVisibility(View.INVISIBLE);
        mEpisodesRuntime.setVisibility(View.INVISIBLE);

        mMoviesWatchlist.setVisibility(View.INVISIBLE);
        mProgressMoviesWatchlist.setVisibility(View.INVISIBLE);
        mMoviesWatchlistRuntime.setVisibility(View.INVISIBLE);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
        loadStats();
    }

    @Override
    public void onStop() {
        super.onStop();

        cleanupStatsTask();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // guard against not attached to activity
        if (!isAdded()) {
            return;
        }

        inflater.inflate(R.menu.stats_menu, menu);

        menu.findItem(R.id.menu_action_stats_filter_specials)
                .setChecked(DisplaySettings.isHidingSpecials(getActivity()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_stats_share) {
            shareStats();
            return true;
        }
        if (itemId == R.id.menu_action_stats_filter_specials) {
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                    .putBoolean(DisplaySettings.KEY_HIDE_SPECIALS, !item.isChecked())
                    .commit();
            getActivity().supportInvalidateOptionsMenu();
            loadStats();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadStatsKeepExisting() {
        if (statsTask != null && statsTask.getStatus() != AsyncTask.Status.FINISHED) {
            return; // stats task still running
        }
        runStatsTask();
    }

    private void loadStats() {
        cleanupStatsTask();
        runStatsTask();
    }

    private void cleanupStatsTask() {
        if (statsTask != null && statsTask.getStatus() != AsyncTask.Status.FINISHED) {
            statsTask.cancel(true);
        }
        statsTask = null;
    }

    private void runStatsTask() {
        statsTask = new StatsTask(getActivity());
        AsyncTaskCompat.executeParallel(statsTask);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(StatsUpdateEvent event) {
        if (!isAdded()) {
            return;
        }
        currentStats = event.stats;
        hasFinalValues = event.finalValues;
        updateStats(event.stats, event.finalValues, event.successful);
    }

    private void updateStats(@NonNull Stats stats, boolean hasFinalValues, boolean successful) {
        // display error if not all stats could be calculated
        errorView.setVisibility(successful ? View.GONE : View.VISIBLE);

        // all shows
        mShowCount.setText(String.valueOf(stats.shows()));

        // shows with next episodes
        mProgressShowsWithNextEpisode.setMax(stats.shows());
        mProgressShowsWithNextEpisode.setProgress(stats.showsWithNextEpisodes());
        mProgressShowsWithNextEpisode.setVisibility(View.VISIBLE);

        mShowsWithNextEpisode.setText(getString(R.string.shows_with_next,
                stats.showsWithNextEpisodes()).toUpperCase(Locale.getDefault()));
        mShowsWithNextEpisode.setVisibility(View.VISIBLE);

        // continuing shows
        mProgressShowsContinuing.setMax(stats.shows());
        mProgressShowsContinuing.setProgress(stats.showsContinuing());
        mProgressShowsContinuing.setVisibility(View.VISIBLE);

        mShowsContinuing.setText(getString(R.string.shows_continuing,
                stats.showsContinuing()).toUpperCase(Locale.getDefault()));
        mShowsContinuing.setVisibility(View.VISIBLE);

        // all episodes
        mEpisodeCount.setText(String.valueOf(stats.episodes()));

        // watched episodes
        mProgressEpisodesWatched.setMax(stats.episodes());
        mProgressEpisodesWatched.setProgress(stats.episodesWatched());
        mProgressEpisodesWatched.setVisibility(View.VISIBLE);

        mEpisodesWatched.setText(getString(R.string.episodes_watched,
                stats.episodesWatched()).toUpperCase(Locale.getDefault()));
        mEpisodesWatched.setVisibility(View.VISIBLE);

        // episode runtime
        String watchedDuration = getTimeDuration(stats.episodesWatchedRuntime());
        if (!hasFinalValues) {
            // showing minimum (= not the final value)
            watchedDuration = "> " + watchedDuration;
        }
        mEpisodesRuntime.setText(watchedDuration);
        mEpisodesRuntime.setVisibility(View.VISIBLE);
        mProgressEpisodesRuntime.setVisibility(successful ?
                (hasFinalValues ? View.GONE : View.VISIBLE)
                : View.GONE);

        // movies
        mMovieCount.setText(String.valueOf(stats.movies));

        // movies in watchlist
        mProgressMoviesWatchlist.setMax(stats.movies);
        mProgressMoviesWatchlist.setProgress(stats.moviesWatchlist);
        mProgressMoviesWatchlist.setVisibility(View.VISIBLE);

        mMoviesWatchlist.setText(getString(R.string.movies_on_watchlist,
                stats.moviesWatchlist).toUpperCase(Locale.getDefault()));
        mMoviesWatchlist.setVisibility(View.VISIBLE);

        // runtime of movie watchlist
        mMoviesWatchlistRuntime.setText(getTimeDuration(stats.moviesWatchlistRuntime));
        mMoviesWatchlistRuntime.setVisibility(View.VISIBLE);
    }

    private String getTimeDuration(long duration) {
        long days = duration / DateUtils.DAY_IN_MILLIS;
        duration %= DateUtils.DAY_IN_MILLIS;
        long hours = duration / DateUtils.HOUR_IN_MILLIS;
        duration %= DateUtils.HOUR_IN_MILLIS;
        long minutes = duration / DateUtils.MINUTE_IN_MILLIS;

        StringBuilder result = new StringBuilder();
        if (days != 0) {
            result.append(getResources().getQuantityString(R.plurals.days_plural, (int) days,
                    (int) days));
        }
        if (hours != 0) {
            if (days != 0) {
                result.append(" ");
            }
            result.append(getResources().getQuantityString(R.plurals.hours_plural, (int) hours,
                    (int) hours));
        }
        if (minutes != 0 || (days == 0 && hours == 0)) {
            if (days != 0 || hours != 0) {
                result.append(" ");
            }
            result.append(getResources().getQuantityString(R.plurals.minutes_plural,
                    (int) minutes,
                    (int) minutes));
        }

        return result.toString();
    }

    private void shareStats() {
        if (currentStats == null) {
            return;
        }

        StringBuilder statsString = new StringBuilder();
        statsString.append(getString(R.string.app_name))
                .append(" ")
                .append(getString(R.string.statistics));
        statsString.append("\n");
        statsString.append("\n");
        // shows
        statsString.append(currentStats.shows())
                .append(" ")
                .append(getString(R.string.statistics_shows));
        statsString.append("\n");
        statsString.append(
                getString(R.string.shows_with_next, currentStats.showsWithNextEpisodes()));
        statsString.append("\n");
        statsString.append(getString(R.string.shows_continuing, currentStats.showsContinuing()));
        statsString.append("\n");
        statsString.append("\n");
        // episodes
        statsString.append(currentStats.episodes()).append(" ").append(
                getString(R.string.statistics_episodes));
        statsString.append("\n");
        statsString.append(getString(R.string.episodes_watched, currentStats.episodesWatched()));
        statsString.append("\n");
        if (currentStats.episodesWatchedRuntime() != 0) {
            String watchedDuration = getTimeDuration(currentStats.episodesWatchedRuntime());
            if (!hasFinalValues) {
                // showing minimum (= not the final value)
                watchedDuration = "> " + watchedDuration;
            }
            statsString.append(watchedDuration)
                    .append(" ")
                    .append(getString(R.string.runtime_all_episodes));
            statsString.append("\n");
        }
        statsString.append("\n");
        // movies
        statsString.append(currentStats.movies)
                .append(" ")
                .append(getString(R.string.statistics_movies));
        statsString.append("\n");
        statsString.append(getString(R.string.movies_on_watchlist, currentStats.moviesWatchlist));
        statsString.append("\n");
        statsString.append(getTimeDuration(currentStats.moviesWatchlistRuntime))
                .append(" ")
                .append(getString(R.string.runtime_movies_watchlist));

        ShareUtils.startShareIntentChooser(getActivity(), statsString.toString(), R.string.share);
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

    private static class StatsTask extends AsyncTask<Void, StatsUpdateEvent, StatsUpdateEvent> {

        private static final long PREVIEW_UPDATE_INTERVAL_MS = DateUtils.SECOND_IN_MILLIS;

        private final Context context;

        public StatsTask(Context context) {
            this.context = context.getApplicationContext();
        }

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

        private static StatsUpdateEvent buildFailure(Stats stats) {
            return new StatsUpdateEvent(stats, false, false);
        }

        private static StatsUpdateEvent buildUpdate(Stats stats) {
            return new StatsUpdateEvent(stats, false, true);
        }

        @Override
        protected void onProgressUpdate(StatsUpdateEvent... values) {
            EventBus.getDefault().post(values[0]);
        }

        @Override
        protected void onPostExecute(StatsUpdateEvent event) {
            EventBus.getDefault().post(event);
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
        private static SparseIntArray processShows(ContentResolver resolver, Stats stats) {
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

    private static class Stats {
        private int mShows;
        private int mShowsContinuing;
        private int mShowsWithNext;
        private int mEpisodes;
        private int mEpisodesWatched;
        private long mEpisodesWatchedRuntime;
        public int movies;
        public int moviesWatchlist;
        public long moviesWatchlistRuntime;

        public int shows() {
            return mShows;
        }

        public Stats shows(int number) {
            mShows = number;
            return this;
        }

        public int showsWithNextEpisodes() {
            return mShowsWithNext;
        }

        public Stats showsWithNextEpisodes(int number) {
            mShowsWithNext = number;
            return this;
        }

        public int showsContinuing() {
            return mShowsContinuing;
        }

        public Stats showsContinuing(int number) {
            mShowsContinuing = number;
            return this;
        }

        public int episodes() {
            return mEpisodes;
        }

        public Stats episodes(int number) {
            mEpisodes = number;
            return this;
        }

        public long episodesWatchedRuntime() {
            return mEpisodesWatchedRuntime;
        }

        public Stats episodesWatchedRuntime(long runtime) {
            mEpisodesWatchedRuntime = runtime;
            return this;
        }

        public int episodesWatched() {
            return mEpisodesWatched;
        }

        public Stats episodesWatched(int number) {
            mEpisodesWatched = number;
            return this;
        }
    }
}
