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

package com.battlelancer.seriesguide.ui.streams;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import com.battlelancer.seriesguide.adapters.MovieHistoryAdapter;
import com.battlelancer.seriesguide.loaders.TraktMovieHistoryLoader;
import com.battlelancer.seriesguide.ui.HistoryActivity;
import com.battlelancer.seriesguide.ui.MovieDetailsActivity;
import com.battlelancer.seriesguide.ui.MovieDetailsFragment;
import com.uwetrottmann.trakt5.entities.HistoryEntry;

/**
 * Displays a stream of movies the user has recently watched on trakt.
 */
public class UserMovieStreamFragment extends StreamFragment {

    private MovieHistoryAdapter mAdapter;

    @Override
    protected ListAdapter getListAdapter() {
        if (mAdapter == null) {
            mAdapter = new MovieHistoryAdapter(getActivity());
        }
        return mAdapter;
    }

    @Override
    protected void initializeStream() {
        getLoaderManager().initLoader(HistoryActivity.MOVIES_LOADER_ID, null,
                mActivityLoaderCallbacks);
    }

    @Override
    protected void refreshStream() {
        getLoaderManager().restartLoader(HistoryActivity.MOVIES_LOADER_ID, null,
                mActivityLoaderCallbacks);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // do not respond if we get a header position by accident
        if (position < 0) {
            return;
        }

        HistoryEntry item = mAdapter.getItem(position);
        if (item == null) {
            return;
        }

        // display movie details
        if (item.movie == null || item.movie.ids == null) {
            return;
        }
        Intent i = new Intent(getActivity(), MovieDetailsActivity.class);
        i.putExtra(MovieDetailsFragment.InitBundle.TMDB_ID, item.movie.ids.tmdb);

        ActivityCompat.startActivity(getActivity(), i,
                ActivityOptionsCompat
                        .makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight())
                        .toBundle()
        );
    }

    private LoaderManager.LoaderCallbacks<TraktMovieHistoryLoader.Result> mActivityLoaderCallbacks =
            new LoaderManager.LoaderCallbacks<TraktMovieHistoryLoader.Result>() {
                @Override
                public Loader<TraktMovieHistoryLoader.Result> onCreateLoader(int id, Bundle args) {
                    showProgressBar(true);
                    return new TraktMovieHistoryLoader(getActivity());
                }

                @Override
                public void onLoadFinished(Loader<TraktMovieHistoryLoader.Result> loader,
                        TraktMovieHistoryLoader.Result data) {
                    if (!isAdded()) {
                        return;
                    }
                    mAdapter.setData(data.results);
                    setEmptyMessage(data.emptyTextResId);
                    showProgressBar(false);
                }

                @Override
                public void onLoaderReset(Loader<TraktMovieHistoryLoader.Result> loader) {
                    // keep current data
                }
            };
}
