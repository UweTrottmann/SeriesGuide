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

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.uwetrottmann.androidutils.AsyncTask;
import com.uwetrottmann.seriesguide.R;

/**
 * Displays some statistics about the users show database, e.g. number of shows,
 * episodes, share of watched episodes, etc.
 */
public class StatsFragment extends SherlockFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.stats_fragment, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        new StatsTask(getActivity().getContentResolver()).execute();
    }

    private class StatsTask extends AsyncTask<Void, Void, Stats> {

        private ContentResolver mResolver;

        public StatsTask(ContentResolver contentResolver) {
            mResolver = contentResolver;
        }

        @Override
        protected Stats doInBackground(Void... params) {
            Stats stats = new Stats();

            // number of...
            // all shows
            final Cursor shows = mResolver.query(Shows.CONTENT_URI, new String[] {
                    Shows._ID
            }, null, null, null);
            if (shows != null) {
                stats.shows(shows.getCount());
                shows.close();
            }

            // continuing shows
            final Cursor showsContinuing = mResolver.query(Shows.CONTENT_URI, new String[] {
                    Shows._ID
            }, Shows.STATUS + "=1", null, null);
            if (showsContinuing != null) {
                stats.showsContinuing(showsContinuing.getCount());
                showsContinuing.close();
            }

            // all episodes
            final Cursor episodes = mResolver.query(Episodes.CONTENT_URI, new String[] {
                    Episodes._ID
            }, null, null, null);
            if (episodes != null) {
                stats.episodes(episodes.getCount());
                episodes.close();
            }

            // watched episodes
            final Cursor episodesWatched = mResolver.query(Episodes.CONTENT_URI, new String[] {
                    Episodes._ID
            }, Episodes.WATCHED + "=1", null, null);
            if (episodesWatched != null) {
                stats.episodesWatched(episodesWatched.getCount());
                episodesWatched.close();
            }

            return stats;
        }

        @Override
        protected void onPostExecute(Stats stats) {
            if (isAdded()) {
                // all shows
                ((TextView) getView().findViewById(R.id.textViewShows)).setText(String
                        .valueOf(stats
                                .shows()));

                // continuing shows
                ProgressBar progressShowsContinuing = (ProgressBar) getView().findViewById(
                        R.id.progressBarShowsContinuing);
                progressShowsContinuing.setMax(stats.shows());
                progressShowsContinuing.setProgress(stats.showsContinuing());
                ((TextView) getView().findViewById(R.id.textViewShowsContinuing))
                        .setText(getString(R.string.shows_continuing,
                                stats.showsContinuing()));

                // all episodes
                ((TextView) getView().findViewById(R.id.textViewEpisodes)).setText(String
                        .valueOf(stats
                                .episodes()));

                // watched episodes
                ProgressBar progressEpisodesWatched = (ProgressBar) getView().findViewById(
                        R.id.progressBarEpisodesWatched);
                progressEpisodesWatched.setMax(stats.episodes());
                progressEpisodesWatched.setProgress(stats.episodesWatched());

                ((TextView) getView().findViewById(R.id.textViewEpisodesWatched))
                        .setText(getString(R.string.episodes_watched,
                                stats.episodesWatched()));

                // runtime
                String watchedDuration = getTimeDuration(stats.episodesWatched()
                        * DateUtils.HOUR_IN_MILLIS);
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
        private int mEpisodes;
        private int mEpisodesWatched;
        private int mEpisodesWatchedRuntime;

        public int shows() {
            return mShows;
        }

        public Stats shows(int number) {
            mShows = number;
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

        public int episodesWatchedRuntime() {
            return mEpisodesWatchedRuntime;
        }

        public Stats episodesWatchedRuntime(int runtime) {
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
