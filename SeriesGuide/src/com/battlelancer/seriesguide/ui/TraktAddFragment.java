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

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.ui.AddActivity.AddPagerAdapter;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.entities.TvShow;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class TraktAddFragment extends AddFragment {

    public static TraktAddFragment newInstance(int position) {
        TraktAddFragment f = new TraktAddFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt("traktlisttype", position);
        f.setArguments(args);

        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /*
         * never use this here (on config change the view needed before removing
         * the fragment)
         */
        // if (container == null) {
        // return null;
        // }
        return inflater.inflate(R.layout.traktaddfragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        int type = getListType();

        // only create and fill a new adapter if there is no previous one
        // (e.g. after config/page changed)
        if (mAdapter == null) {
            mAdapter = new AddAdapter(getActivity(), R.layout.add_searchresult,
                    new ArrayList<SearchResult>(), mAddButtonListener, mDetailsButtonListener);

            AndroidUtils.executeAsyncTask(new GetTraktShowsTask(getActivity()), type);
        }

        if (type == AddPagerAdapter.LIBRARY_TAB_POSITION
                || type == AddPagerAdapter.WATCHLIST_TAB_POSITION) {
            setHasOptionsMenu(true);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getTracker().trackView("Add Trakt Shows");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.trakt_library_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_add_all) {
            if (mSearchResults != null) {
                TaskManager.getInstance(getActivity()).performAddTask(mSearchResults);
            }
            // disable the item so the user has to come back
            item.setEnabled(false);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private int getListType() {
        return getArguments().getInt("traktlisttype");
    }

    public class GetTraktShowsTask extends AsyncTask<Integer, Void, List<SearchResult>> {

        private Context mContext;

        public GetTraktShowsTask(Context context) {
            mContext = context;
        }

        @Override
        protected List<SearchResult> doInBackground(Integer... params) {
            int type = params[0];
            List<SearchResult> showList = new ArrayList<SearchResult>();

            List<TvShow> shows = new ArrayList<TvShow>();

            if (type == AddPagerAdapter.TRENDING_TAB_POSITION) {
                try {
                    shows = Utils.getServiceManager(mContext).showService().trending().fire();
                } catch (Exception e) {
                    // we don't care
                }
            } else {
                try {
                    ServiceManager manager = Utils.getServiceManagerWithAuth(mContext, false);

                    switch (type) {
                        case AddPagerAdapter.RECOMMENDED_TAB_POSITION:
                            shows = manager.recommendationsService().shows().fire();
                            break;
                        case AddPagerAdapter.LIBRARY_TAB_POSITION:
                            shows = manager.userService()
                                    .libraryShowsAll(Utils.getTraktUsername(mContext)).fire();
                            break;
                        case AddPagerAdapter.WATCHLIST_TAB_POSITION:
                            shows = manager.userService()
                                    .watchlistShows(Utils.getTraktUsername(mContext)).fire();
                            break;
                    }
                } catch (Exception e) {
                    // we don't care
                }
            }

            // get a list of existing shows to filter against
            final Cursor existing = mContext.getContentResolver().query(Shows.CONTENT_URI,
                    new String[] {
                        Shows._ID
                    }, null, null, null);
            final HashSet<String> existingIds = new HashSet<String>();
            while (existing.moveToNext()) {
                existingIds.add(existing.getString(0));
            }
            existing.close();

            parseTvShowsToSearchResults(shows, showList, existingIds);

            return showList;
        }

        @Override
        protected void onPostExecute(List<SearchResult> result) {
            setSearchResults(result);
        }
    }

    /**
     * Parse a list of {@link TvShow} objects to a list of {@link SearchResult}
     * objects.
     * 
     * @param inputList
     * @param outputList
     * @return
     */
    private static void parseTvShowsToSearchResults(List<TvShow> inputList,
            List<SearchResult> outputList, HashSet<String> existingIds) {
        Iterator<TvShow> shows = inputList.iterator();
        while (shows.hasNext()) {
            TvShow tvShow = (TvShow) shows.next();

            // only list non-existing shows
            if (!existingIds.contains(tvShow.tvdbId)) {
                SearchResult show = new SearchResult();
                show.tvdbid = tvShow.tvdbId;
                show.title = tvShow.title;
                show.overview = tvShow.overview;
                String posterPath = tvShow.images.poster;
                if (posterPath != null) {
                    show.poster = posterPath.substring(0, posterPath.length() - 4) + "-138.jpg";
                }
                outputList.add(show);
            }
        }
    }
}
