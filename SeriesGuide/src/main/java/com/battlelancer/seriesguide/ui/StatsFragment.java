/*
 * Copyright 2013 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.SherlockFragment;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.uwetrottmann.androidutils.AsyncTask;
import com.uwetrottmann.seriesguide.R;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Locale;

/**
 * Displays some statistics about the users show database, e.g. number of shows,
 * episodes, share of watched episodes, etc.
 */
public class StatsFragment extends SherlockFragment {

    private AsyncTask<Void, Stats, Stats> mStatsTask;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.stats_fragment, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        mStatsTask = new StatsTask(getActivity().getContentResolver()).execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mStatsTask != null && mStatsTask.getStatus() != AsyncTask.Status.FINISHED) {
            mStatsTask.cancel(true);
            mStatsTask = null;
        }
    }

    private class StatsTask extends AsyncTask<Void, Stats, Stats> {

        private ContentResolver mResolver;

        public StatsTask(ContentResolver contentResolver) {
            mResolver = contentResolver;
        }

        @Override
        protected Stats doInBackground(Void... params) {
            Stats stats = new Stats();

            // number of...
            // ...all shows
            final Cursor shows = mResolver.query(Shows.CONTENT_URI, new String[] {
                    Shows._ID, Shows.STATUS, Shows.NEXTEPISODE, Shows.RUNTIME
            }, null, null, null);
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

                // ...all episodes
                final Cursor episodes = mResolver.query(Episodes.CONTENT_URI, new String[] {
                        Episodes._ID
                }, null, null, null);
                if (episodes != null) {
                    stats.episodes(episodes.getCount());
                    episodes.close();
                }

                // ...watched episodes
                final Cursor episodesWatched = mResolver.query(Episodes.CONTENT_URI, new String[] {
                        Episodes._ID
                }, Episodes.WATCHED + "=" + EpisodeFlags.WATCHED, null, null);
                if (episodesWatched != null) {
                    stats.episodesWatched(episodesWatched.getCount());
                    episodesWatched.close();
                }

                // report intermediate results before longer running op
                publishProgress(stats);

                // calculate runtime of watched episodes per show
                shows.moveToPosition(-1);
                long totalRuntime = 0;
                while (shows.moveToNext()) {
                    final Cursor episodesWatchedOfShow = mResolver.query(
                            Episodes.buildEpisodesOfShowUri(shows.getString(0)), new String[] {
                                Episodes._ID
                            }, Episodes.WATCHED + "=1", null, null);
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
                ((TextView) getView().findViewById(R.id.textViewShows)).setText(
                        String.valueOf(stats.shows()));

                // shows with next episodes
                ProgressBar progressShowsWithNext = (ProgressBar) findAndShowView(R.id.progressBarShowsWithNext);
                progressShowsWithNext.setMax(stats.shows());
                progressShowsWithNext.setProgress(stats.showsWithNextEpisodes());

                ((TextView) findAndShowView(R.id.textViewShowsWithNext)).setText(getString(
                        R.string.shows_with_next,
                        stats.showsWithNextEpisodes()).toUpperCase(Locale.getDefault()));

                // continuing shows
                ProgressBar progressShowsContinuing = (ProgressBar) findAndShowView(R.id.progressBarShowsContinuing);
                progressShowsContinuing.setMax(stats.shows());
                progressShowsContinuing.setProgress(stats.showsContinuing());

                ((TextView) findAndShowView(R.id.textViewShowsContinuing)).setText(getString(
                        R.string.shows_continuing,
                        stats.showsContinuing()).toUpperCase(Locale.getDefault()));

                // all episodes
                ((TextView) getView().findViewById(R.id.textViewEpisodes)).setText(
                        String.valueOf(stats.episodes()));

                // watched episodes
                ProgressBar progressEpisodesWatched = (ProgressBar) findAndShowView(R.id.progressBarEpisodesWatched);
                progressEpisodesWatched.setMax(stats.episodes());
                progressEpisodesWatched.setProgress(stats.episodesWatched());

                ((TextView) findAndShowView(R.id.textViewEpisodesWatched)).setText(getString(
                        R.string.episodes_watched,
                        stats.episodesWatched()).toUpperCase(Locale.getDefault()));
            }
        }

        private View findAndShowView(int viewResId) {
            View v = getView().findViewById(viewResId);
            if (v.getVisibility() != View.VISIBLE) {
                v.setVisibility(View.VISIBLE);
            }
            return v;
        }

        @Override
        protected void onPostExecute(Stats stats) {
            if (isAdded()) {
                View progress = getView().findViewById(R.id.progressEpisodesRuntime);
                if (progress.getVisibility() == View.VISIBLE) {
                    progress.setVisibility(View.GONE);
                }

                // runtime
                String watchedDuration = getTimeDuration(stats.episodesWatchedRuntime());
                ((TextView) getView().findViewById(R.id.textViewEpisodesRuntime))
                        .setText(watchedDuration);
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
