/*
 * Copyright 2011 Uwe Trottmann
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

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.battlelancer.seriesguide.provider.SeriesContract.EpisodeSearch;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.uwetrottmann.seriesguide.R;

/**
 * Displays a list of search results and allows searching for episodes.
 */
public class SearchFragment extends SherlockListFragment implements LoaderCallbacks<Cursor> {

    private static final int LOADER_ID = R.string.search_title;

    private SimpleCursorAdapter mAdapter;

    private String mShowTitle;

    interface InitBundle {
        String QUERY = "query";

        String SHOW_TITLE = "title";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.search_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // get color attribute
        final TypedValue greenValue = new TypedValue();
        getActivity().getTheme().resolveAttribute(R.attr.textColorSgGreen,
                greenValue, true);

        String[] from = new String[] {
                Episodes.TITLE, Episodes.OVERVIEW, Episodes.NUMBER, Episodes.WATCHED, Shows.TITLE
        };
        int[] to = new int[] {
                R.id.TextViewSearchRow, R.id.TextViewSearchSnippet, R.id.TextViewSearchEpNumbers,
                R.id.TextViewSearchEpWatchedState, R.id.TextViewSearchSeriesName
        };

        mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.search_row, null, from, to, 0);
        mAdapter.setViewBinder(new ViewBinder() {

            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (columnIndex == SearchQuery.NUMBER) {
                    TextView numbers = (TextView) view;
                    String epnumber = getString(R.string.episode) + " "
                            + cursor.getString(columnIndex);
                    String senumber = getString(R.string.season) + " "
                            + cursor.getString(SearchQuery.SEASON);
                    numbers.setText(senumber + " " + epnumber);
                    return true;
                }
                if (columnIndex == SearchQuery.WATCHED) {
                    boolean isWatched = (1 == cursor.getInt(columnIndex));
                    TextView watchedState = (TextView) view;
                    watchedState.setText(isWatched ? getString(R.string.episode_iswatched)
                            : getString(R.string.episode_notwatched));
                    watchedState.setTextColor(isWatched ? getResources().getColor(
                            greenValue.resourceId) : Color.GRAY);
                    return true;
                }
                if (columnIndex == SearchQuery.OVERVIEW) {
                    TextView watchedState = (TextView) view;
                    // make matched term bold
                    watchedState.setText(Html.fromHtml(cursor.getString(SearchQuery.OVERVIEW)));
                    return true;
                }

                return false;
            }
        });
        setListAdapter(mAdapter);

        getLoaderManager().initLoader(LOADER_ID, getArguments(), this);
    }

    public void onPerformSearch(Bundle args) {
        getLoaderManager().restartLoader(LOADER_ID, args, this);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent i = new Intent(getActivity(), EpisodeDetailsActivity.class);
        i.putExtra(EpisodeDetailsActivity.InitBundle.EPISODE_TVDBID, (int) id);
        startActivity(i);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String selection = null;
        final String query = args.getString(SearchManager.QUERY);
        String[] selectionArgs = new String[] {
                query
        };

        Bundle appData = args.getBundle(SearchManager.APP_DATA);
        if (appData != null) {
            String showtitle = appData.getString(InitBundle.SHOW_TITLE);
            if (showtitle != null) {
                // preserve show filter as long as this fragment is alive
                mShowTitle = showtitle;
            }
        }

        // set show filter
        if (mShowTitle != null) {
            selection = Shows.TITLE + "=?";
            selectionArgs = new String[] {
                    query, mShowTitle
            };
        }

        return new CursorLoader(getActivity(), EpisodeSearch.CONTENT_URI_SEARCH,
                SearchQuery.PROJECTION, selection, selectionArgs, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    interface SearchQuery {
        String[] PROJECTION = new String[] {
                Episodes._ID, Episodes.TITLE, Episodes.OVERVIEW, Episodes.NUMBER, Episodes.SEASON,
                Episodes.WATCHED, Shows.TITLE
        };

        int _ID = 0;

        int TITLE = 1;

        int OVERVIEW = 2;

        int NUMBER = 3;

        int SEASON = 4;

        int WATCHED = 5;

        int SHOW_TITLE = 6;
    }
}
