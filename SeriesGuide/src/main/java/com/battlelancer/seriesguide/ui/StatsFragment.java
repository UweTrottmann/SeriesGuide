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
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.uwetrottmann.androidutils.AndroidUtils;
import java.util.Locale;

/**
 * Displays some statistics about the users show database, e.g. number of shows,
 * episodes, share of watched episodes, etc.
 */
public class StatsFragment extends Fragment {

    @InjectView(R.id.textViewStatsShows) TextView mShowCount;
    @InjectView(R.id.textViewStatsShowsWithNext) TextView mShowsWithNextEpisode;
    @InjectView(R.id.progressBarStatsShowsWithNext) ProgressBar mProgressShowsWithNextEpisode;
    @InjectView(R.id.textViewStatsShowsContinuing) TextView mShowsContinuing;
    @InjectView(R.id.progressBarStatsShowsContinuing) ProgressBar mProgressShowsContinuing;

    @InjectView(R.id.textViewStatsEpisodes) TextView mEpisodeCount;
    @InjectView(R.id.textViewStatsEpisodesWatched) TextView mEpisodesWatched;
    @InjectView(R.id.progressBarStatsEpisodesWatched) ProgressBar mProgressEpisodesWatched;
    @InjectView(R.id.textViewStatsEpisodesRuntime) TextView mEpisodesRuntime;
    @InjectView(R.id.progressBarStatsEpisodesRuntime) ProgressBar mProgressEpisodesRuntime;

    @InjectView(R.id.textViewStatsMovies) TextView mMovieCount;
    @InjectView(R.id.textViewStatsMoviesWatchlist) TextView mMoviesWatchlist;
    @InjectView(R.id.progressBarStatsMoviesWatchlist) ProgressBar mProgressMoviesWatchlist;
    @InjectView(R.id.textViewStatsMoviesWatchlistRuntime) TextView mMoviesWatchlistRuntime;

    private AsyncTask<Void, Stats, Stats> mStatsTask;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.stats_fragment, container, false);
        ButterKnife.inject(this, v);

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

        loadStats();
    }

    @Override
    public void onStop() {
        super.onStop();

        cleanupStatsTask();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        ButterKnife.reset(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.stats_menu, menu);

        menu.findItem(R.id.menu_action_stats_filter_specials)
                .setChecked(DisplaySettings.isHidingSpecials(getActivity()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
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
        mStatsTask = new StatsTask();
        AndroidUtils.executeOnPool(mStatsTask);
    }

    private void cleanupStatsTask() {
        if (mStatsTask != null && mStatsTask.getStatus() != AsyncTask.Status.FINISHED) {
            mStatsTask.cancel(true);
        }
        mStatsTask = null;
    }

    private class StatsTask extends AsyncTask<Void, Stats, Stats> {

        @Override
        protected Stats doInBackground(Void... params) {
            Stats stats = new Stats();
            ContentResolver resolver = getActivity().getContentResolver();

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

                boolean includeSpecials = !DisplaySettings.isHidingSpecials(getActivity());

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
                while (shows.moveToNext()) {
                    final Cursor episodesWatchedOfShow = resolver.query(
                            Episodes.buildEpisodesOfShowUri(shows.getString(0)),
                            new String[] { Episodes._ID },
                            Episodes.SELECTION_WATCHED
                                    + (includeSpecials
                                    ? "" : " AND " + Episodes.SELECTION_NO_SPECIALS),
                            null, null
                    );
                    if (episodesWatchedOfShow != null) {
                        long runtimeOfShow = shows.getInt(3) * DateUtils.MINUTE_IN_MILLIS;
                        long runtimeOfEpisodes = episodesWatchedOfShow.getCount() * runtimeOfShow;
                        totalRuntime += runtimeOfEpisodes;

                        episodesWatchedOfShow.close();
                    }
                }
                stats.episodesWatchedRuntime(totalRuntime);

                shows.close();
            }

            return stats;
        }

        @Override
        protected void onProgressUpdate(Stats... values) {
            if (isAdded()) {
                Stats stats = values[0];

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
        }

        @Override
        protected void onPostExecute(Stats stats) {
            if (isAdded()) {
                mProgressEpisodesRuntime.setVisibility(View.GONE);

                // runtime
                String watchedDuration = getTimeDuration(stats.episodesWatchedRuntime());
                mEpisodesRuntime.setText(watchedDuration);
                mEpisodesRuntime.setVisibility(View.VISIBLE);
            }
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
