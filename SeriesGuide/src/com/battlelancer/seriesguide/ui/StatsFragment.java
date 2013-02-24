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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

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

            final Cursor shows = mResolver.query(Shows.CONTENT_URI,
                    new String[] {
                        Shows._ID
                    }, null, null, null);
            if (shows != null) {
                stats.numberOfShows(String.valueOf(shows.getCount()));
                shows.close();
            }

            return stats;
        }

        @Override
        protected void onPostExecute(Stats stats) {
            if (isAdded()) {
                ((TextView) getView().findViewById(R.id.textViewNumberOfShows)).setText(stats
                        .numberOfShows());
            }
        }

    }

    private static class Stats {
        private String mNumberOfShows;

        public String numberOfShows() {
            return mNumberOfShows;
        }

        public Stats numberOfShows(String number) {
            mNumberOfShows = number;
            return this;
        }

    }

}
