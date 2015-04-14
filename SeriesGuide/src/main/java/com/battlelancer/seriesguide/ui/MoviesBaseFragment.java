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

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.MoviesCursorAdapter;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.MoviesDistillationSettings;
import de.greenrobot.event.EventBus;

import static com.battlelancer.seriesguide.settings.MoviesDistillationSettings.MoviesSortOrder;
import static com.battlelancer.seriesguide.settings.MoviesDistillationSettings.MoviesSortOrderChangedEvent;

/**
 * A shell for a fragment displaying a number of movies.
 */
public abstract class MoviesBaseFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener,
        MoviesCursorAdapter.PopupMenuClickListener {

    private static final int LAYOUT = R.layout.fragment_movies;

    private GridView mGridView;

    protected TextView mEmptyView;

    protected MoviesCursorAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(LAYOUT, container, false);

        mGridView = (GridView) v.findViewById(R.id.gridViewMovies);
        mEmptyView = (TextView) v.findViewById(R.id.textViewMoviesEmpty);
        mGridView.setEmptyView(mEmptyView);
        mGridView.setOnItemClickListener(this);

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EventBus.getDefault().register(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new MoviesCursorAdapter(getActivity(), this);
        mGridView.setAdapter(mAdapter);

        getLoaderManager().initLoader(getLoaderId(), null, this);

        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // guard against not attached to activity
        if (!isAdded()) {
            return;
        }

        inflater.inflate(R.menu.movies_menu, menu);

        menu.findItem(R.id.menu_action_movies_sort_ignore_articles)
                .setChecked(DisplaySettings.isSortOrderIgnoringArticles(getActivity()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_movies_sort_title) {
            if (MoviesDistillationSettings.getSortOrderId(getActivity())
                    == MoviesSortOrder.TITLE_ALPHABETICAL_ID) {
                changeSortOrder(MoviesSortOrder.TITLE_REVERSE_ALHPABETICAL_ID);
            } else {
                // was sorted title reverse or by release date
                changeSortOrder(MoviesSortOrder.TITLE_ALPHABETICAL_ID);
            }
            return true;
        }
        if (itemId == R.id.menu_action_movies_sort_release) {
            if (MoviesDistillationSettings.getSortOrderId(getActivity())
                    == MoviesSortOrder.RELEASE_DATE_NEWEST_FIRST_ID) {
                changeSortOrder(MoviesSortOrder.RELEASE_DATE_OLDEST_FIRST_ID);
            } else {
                // was sorted by oldest first or by title
                changeSortOrder(MoviesSortOrder.RELEASE_DATE_NEWEST_FIRST_ID);
            }
            return true;
        }
        if (itemId == R.id.menu_action_movies_sort_ignore_articles) {
            changeSortIgnoreArticles(!DisplaySettings.isSortOrderIgnoringArticles(getActivity()));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void changeSortOrder(int sortOrderId) {
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                .putInt(MoviesDistillationSettings.KEY_SORT_ORDER, sortOrderId)
                .commit();

        EventBus.getDefault().post(new MoviesSortOrderChangedEvent());
    }

    private void changeSortIgnoreArticles(boolean value) {
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                .putBoolean(DisplaySettings.KEY_SORT_IGNORE_ARTICLE, value)
                .commit();

        // refresh icon state
        getActivity().supportInvalidateOptionsMenu();

        EventBus.getDefault().post(new MoviesSortOrderChangedEvent());
    }

    public void onEventMainThread(MoviesSortOrderChangedEvent event) {
        getLoaderManager().restartLoader(getLoaderId(), null, this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor movie = (Cursor) mAdapter.getItem(position);
        int tmdbId = movie.getInt(MoviesCursorAdapter.MoviesQuery.TMDB_ID);

        // launch movie details activity
        Intent i = new Intent(getActivity(), MovieDetailsActivity.class);
        i.putExtra(MovieDetailsFragment.InitBundle.TMDB_ID, tmdbId);

        ActivityCompat.startActivity(getActivity(), i,
                ActivityOptionsCompat
                        .makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight())
                        .toBundle()
        );
    }

    @Override
    public abstract void onPopupMenuClick(View v, int movieTmdbId);

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
