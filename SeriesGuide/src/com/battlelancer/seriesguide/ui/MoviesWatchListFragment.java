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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.actionbarsherlock.app.SherlockFragment;
import com.battlelancer.seriesguide.adapters.MoviesWatchListAdapter;
import com.battlelancer.seriesguide.loaders.TraktMoviesWatchlistLoader;
import com.jakewharton.trakt.entities.Movie;
import com.uwetrottmann.seriesguide.R;

import java.util.List;

/**
 * Loads and displays the users trakt movie watchlist.
 */
public class MoviesWatchListFragment extends SherlockFragment implements
        LoaderCallbacks<List<Movie>>, OnItemClickListener {

    private MoviesWatchListAdapter mAdapter;
    private GridView mGridView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.movies_watchlist_fragment, container, false);

        mGridView = (GridView) v.findViewById(R.id.gridViewMoviesWatchlist);
        mGridView.setEmptyView(v.findViewById(R.id.textViewMoviesWatchlistEmpty));

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new MoviesWatchListAdapter(getActivity());

        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(this);

        getLoaderManager().initLoader(R.layout.movies_watchlist_fragment, null, this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Movie movie = mAdapter.getItem(position);

        // launch details activity
        Intent i = new Intent(getActivity(), MovieDetailsActivity.class);
        i.putExtra(MovieDetailsFragment.InitBundle.TMDB_ID, Integer.valueOf(movie.tmdbId));
        startActivity(i);
    }

    @Override
    public Loader<List<Movie>> onCreateLoader(int loaderId, Bundle args) {
        return new TraktMoviesWatchlistLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<Movie>> loader, List<Movie> data) {
        mAdapter.setData(data);
    }

    @Override
    public void onLoaderReset(Loader<List<Movie>> loader) {
        mAdapter.setData(null);
    }
}
