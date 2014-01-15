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
import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.util.MovieTools;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.TraktTask.TraktActionCompleteEvent;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.TextView;

import de.greenrobot.event.EventBus;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;

/**
 * Loads and displays the users trakt movie watchlist.
 */
public class MoviesWatchListFragment extends SherlockFragment implements
        LoaderCallbacks<Cursor>, OnItemClickListener, OnClickListener {

    private static final String TAG = "Movie Watchlist";

    private static final int LAYOUT = R.layout.fragment_movies;

    private static final int LOADER_ID = LAYOUT;

    private static final int CONTEXT_REMOVE_ID = 0;

    private GridView mGridView;

    private MoviesCursorAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(LAYOUT, container, false);

        mGridView = (GridView) v.findViewById(android.R.id.list);
        TextView emptyView = (TextView) v.findViewById(R.id.textViewMoviesEmpty);
        emptyView.setText(R.string.movies_watchlist_empty);
        mGridView.setEmptyView(emptyView);
        mGridView.setOnItemClickListener(this);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new MoviesCursorAdapter(getActivity(), this);
        mGridView.setAdapter(mAdapter);

        registerForContextMenu(mGridView);

        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public void onStart() {
        super.onStart();
        /*
         * Already register here instead of onResume() because watchlist might
         * be modified by search fragment adjacent in view pager
         */
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(0, CONTEXT_REMOVE_ID, 0, R.string.watchlist_remove);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        /*
         * This fixes all fragments receiving the context menu dispatch, see
         * http://stackoverflow.com/questions/5297842/how-to-handle-
         * oncontextitemselected-in-a-multi-fragment-activity and others.
         */
        if (!getUserVisibleHint()) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
            case CONTEXT_REMOVE_ID: {
                AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
                Cursor movie = (Cursor) mAdapter.getItem(info.position);
                int tmdbId = movie.getInt(MoviesCursorAdapter.MoviesQuery.TMDB_ID);

                MovieTools.removeFromWatchlist(getActivity(), tmdbId);

                fireTrackerEvent("Remove from watchlist");
                return true;
            }
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        getActivity().openContextMenu(v);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor movie = (Cursor) mAdapter.getItem(position);
        int tmdbId = movie.getInt(MoviesCursorAdapter.MoviesQuery.TMDB_ID);

        // launch details activity
        Intent i = new Intent(getActivity(), MovieDetailsActivity.class);
        i.putExtra(MovieDetailsFragment.InitBundle.TMDB_ID, tmdbId);
        startActivity(i);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        return new CursorLoader(getActivity(), Movies.CONTENT_URI_WATCHLIST,
                MoviesCursorAdapter.MoviesQuery.PROJECTION, null, null, Movies.DEFAULT_SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    public void onEvent(TraktActionCompleteEvent event) {
        int traktAction = event.mTraktTaskArgs.getInt(TraktTask.InitBundle.TRAKTACTION);
        if (traktAction == TraktAction.WATCHLIST_MOVIE.index
                || traktAction == TraktAction.UNWATCHLIST_MOVIE.index) {
            // reload movie watchlist after user added/removed
            getLoaderManager().restartLoader(LOADER_ID, null, this);
        }
    }

    private void fireTrackerEvent(String label) {
        Utils.trackAction(getActivity(), TAG, label);
    }

}
