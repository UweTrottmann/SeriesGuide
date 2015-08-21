/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.ui;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.uwetrottmann.androidutils.AndroidUtils;
import de.greenrobot.event.EventBus;
import java.util.Locale;

/**
 * Displays some statistics about the users show database, e.g. number of shows, episodes, share of
 * watched episodes, etc.
 */
public class StatsFragment extends Fragment {

    @Bind(R.id.textViewStatsShows) TextView mShowCount;
    @Bind(R.id.textViewStatsShowsWithNext) TextView mShowsWithNextEpisode;
    @Bind(R.id.progressBarStatsShowsWithNext) ProgressBar mProgressShowsWithNextEpisode;
    @Bind(R.id.textViewStatsShowsContinuing) TextView mShowsContinuing;
    @Bind(R.id.progressBarStatsShowsContinuing) ProgressBar mProgressShowsContinuing;

    @Bind(R.id.textViewStatsEpisodes) TextView mEpisodeCount;
    @Bind(R.id.textViewStatsEpisodesWatched) TextView mEpisodesWatched;
    @Bind(R.id.progressBarStatsEpisodesWatched) ProgressBar mProgressEpisodesWatched;
    @Bind(R.id.textViewStatsEpisodesRuntime) TextView mEpisodesRuntime;
    @Bind(R.id.progressBarStatsEpisodesRuntime) ProgressBar mProgressEpisodesRuntime;

    @Bind(R.id.textViewStatsMovies) TextView mMovieCount;
    @Bind(R.id.textViewStatsMoviesWatchlist) TextView mMoviesWatchlist;
    @Bind(R.id.progressBarStatsMoviesWatchlist) ProgressBar mProgressMoviesWatchlist;
    @Bind(R.id.textViewStatsMoviesWatchlistRuntime) TextView mMoviesWatchlistRuntime;

    private AsyncTask<Void, Stats, Stats> statsTask;
    private Stats currentStats;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_stats, container, false);
        ButterKnife.bind(this, v);

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

        ButterKnife.unbind(this);
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

    private void loadStats() {
        cleanupStatsTask();
        statsTask = new StatsTask(getActivity());
        AndroidUtils.executeOnPool(statsTask);
    }

    private void cleanupStatsTask() {
        if (statsTask != null && statsTask.getStatus() != AsyncTask.Status.FINISHED) {
            statsTask.cancel(true);
        }
        statsTask = null;
    }

    public void onEventMainThread(StatsTask.StatsUpdateEvent event) {
        if (!isAdded()) {
            return;
        }
        currentStats = event.stats;
        updateStats(event.stats);
    }

    private void updateStats(Stats stats) {
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
        if (!stats.hasFinalValues) {
            // showing minimum (= not the final value)
            watchedDuration = "> " + watchedDuration;
        }
        mEpisodesRuntime.setText(watchedDuration);
        mEpisodesRuntime.setVisibility(View.VISIBLE);
        if (stats.hasFinalValues) {
            mProgressEpisodesRuntime.setVisibility(View.GONE);
        }

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
            if (!currentStats.hasFinalValues) {
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

    private static class StatsTask extends AsyncTask<Void, Stats, Stats> {

        public class StatsUpdateEvent {
            public final Stats stats;

            public StatsUpdateEvent(Stats stats, boolean hasFinalValues) {
                stats.hasFinalValues = hasFinalValues;
                this.stats = stats;
            }
        }

        private final Context context;

        public StatsTask(Context context) {
            this.context = context.getApplicationContext();
        }

        @Override
        protected Stats doInBackground(Void... params) {
            Stats stats = new Stats();
            ContentResolver resolver = context.getContentResolver();

            // number of...
            // ...movies (count, in watchlist, runtime of watchlist)
            final Cursor movies = resolver.query(SeriesGuideContract.Movies.CONTENT_URI,
                    new String[] { SeriesGuideContract.Movies._ID,
                            SeriesGuideContract.Movies.IN_WATCHLIST,
                            SeriesGuideContract.Movies.RUNTIME_MIN }, null, null, null
            );
            if (movies != null) {
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
            }

            if (isCancelled()) {
                return stats;
            }

            // ...all shows
            final Cursor shows = resolver.query(Shows.CONTENT_URI,
                    new String[] {
                            Shows._ID, Shows.STATUS, Shows.NEXTEPISODE, Shows.RUNTIME
                    }, null, null, null
            );
            if (shows != null) {
                int continuing = 0;
                int withnext = 0;
                while (shows.moveToNext()) {
                    // ...continuing shows
                    if (shows.getInt(1) == 1) {
                        continuing++;
                    }
                    // ...shows with next episodes
                    if (shows.getInt(2) != 0) {
                        withnext++;
                    }
                }
                stats.shows(shows.getCount()).showsContinuing(continuing)
                        .showsWithNextEpisodes(withnext);

                boolean includeSpecials = !DisplaySettings.isHidingSpecials(context);

                // ...all episodes
                final Cursor episodes = resolver.query(Episodes.CONTENT_URI,
                        new String[] { Episodes._ID },
                        includeSpecials ? null : Episodes.SELECTION_NO_SPECIALS,
                        null, null);
                if (episodes != null) {
                    stats.episodes(episodes.getCount());
                    episodes.close();
                }

                // ...watched episodes
                final Cursor episodesWatched = resolver.query(Episodes.CONTENT_URI,
                        new String[] { Episodes._ID },
                        Episodes.SELECTION_WATCHED
                                + (includeSpecials ? "" : " AND " + Episodes.SELECTION_NO_SPECIALS),
                        null, null
                );
                if (episodesWatched != null) {
                    stats.episodesWatched(episodesWatched.getCount());
                    episodesWatched.close();
                }

                if (isCancelled()) {
                    shows.close();
                    return stats;
                }

                // report intermediate results before longer running op
                publishProgress(stats);

                // calculate runtime of watched episodes per show
                shows.moveToPosition(-1);
                long totalRuntime = 0;
                int count = 0;
                while (shows.moveToNext()) {
                    final Cursor episodesWatchedOfShow = resolver.query(
                            Episodes.buildEpisodesOfShowUri(shows.getString(0)),
                            new String[] { Episodes._ID },
                            Episodes.SELECTION_WATCHED
                                    + (includeSpecials
                                    ? "" : " AND " + Episodes.SELECTION_NO_SPECIALS),
                            null, null
                    );
                    if (episodesWatchedOfShow == null) {
                        continue;
                    }
                    long runtimeOfShow = shows.getInt(3) * DateUtils.MINUTE_IN_MILLIS;
                    long runtimeOfEpisodes = episodesWatchedOfShow.getCount() * runtimeOfShow;
                    totalRuntime += runtimeOfEpisodes;

                    episodesWatchedOfShow.close();
                    count++;
                    // post regular update of minimum
                    if (count == 25) {
                        count = 0;
                        stats.episodesWatchedRuntime(totalRuntime);
                        publishProgress(stats);
                    }
                }
                stats.episodesWatchedRuntime(totalRuntime);

                shows.close();
            }

            return stats;
        }

        @Override
        protected void onProgressUpdate(Stats... values) {
            EventBus.getDefault().post(new StatsUpdateEvent(values[0], false));
        }

        @Override
        protected void onPostExecute(Stats stats) {
            EventBus.getDefault().post(new StatsUpdateEvent(stats, true));
        }
    }

    private static class Stats {
        public boolean hasFinalValues;
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
