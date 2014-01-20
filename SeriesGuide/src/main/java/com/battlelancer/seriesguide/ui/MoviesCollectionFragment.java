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

import com.battlelancer.seriesguide.adapters.MoviesCursorAdapter;
import com.battlelancer.seriesguide.util.MovieTools;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.R;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;

/**
 * Displays a users collection of movies in a grid.
 */
public class MoviesCollectionFragment extends MoviesBaseFragment {

    protected static final int LOADER_ID = 301;

    private static final String TAG = "Movie Collection";

    private static final int CONTEXT_COLLECTION_REMOVE_ID = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        mEmptyView.setText(R.string.movies_collection_empty);

        return v;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(0, CONTEXT_COLLECTION_REMOVE_ID, 0, R.string.action_collection_remove);
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
            case CONTEXT_COLLECTION_REMOVE_ID: {

                // TODO actually remove from collection

                fireTrackerEvent("Remove from collection");
                return true;
            }
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), Movies.CONTENT_URI,
                MoviesCursorAdapter.MoviesQuery.PROJECTION, Movies.SELECTION_COLLECTION, null,
                Movies.DEFAULT_SORT);
    }

    @Override
    protected int getLoaderId() {
        return LOADER_ID;
    }

    private void fireTrackerEvent(String label) {
        Utils.trackAction(getActivity(), TAG, label);
    }
}
