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

import com.actionbarsherlock.app.SherlockFragment;
import com.battlelancer.seriesguide.adapters.MoviesCursorAdapter;
import com.uwetrottmann.seriesguide.R;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

/**
 * A shell for a fragment displaying a number of movies.
 */
public abstract class MoviesBaseFragment extends SherlockFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener,
        View.OnClickListener {

    private static final int LAYOUT = R.layout.fragment_movies;

    private GridView mGridView;

    protected TextView mEmptyView;

    protected MoviesCursorAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(LAYOUT, container, false);

        mGridView = (GridView) v.findViewById(android.R.id.list);
        mEmptyView = (TextView) v.findViewById(R.id.textViewMoviesEmpty);
        mGridView.setEmptyView(mEmptyView);
        mGridView.setOnItemClickListener(this);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new MoviesCursorAdapter(getActivity(), this);
        mGridView.setAdapter(mAdapter);

        registerForContextMenu(mGridView);

        getLoaderManager().initLoader(getLoaderId(), null, this);
    }

    @Override
    public void onClick(View v) {
        getActivity().openContextMenu(v);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor movie = (Cursor) mAdapter.getItem(position);
        int tmdbId = movie.getInt(MoviesCursorAdapter.MoviesQuery.TMDB_ID);

        // launch movie details activity
        Intent i = new Intent(getActivity(), MovieDetailsActivity.class);
        i.putExtra(MovieDetailsFragment.InitBundle.TMDB_ID, tmdbId);
        startActivity(i);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    /**
     * Return a loader id different from any other used within {@link com.battlelancer.seriesguide.ui.MoviesActivity}.
     */
    protected abstract int getLoaderId();

}
