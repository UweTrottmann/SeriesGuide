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

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.loaders.TraktAddLoader;
import com.battlelancer.seriesguide.ui.AddActivity.AddPagerAdapter;
import com.battlelancer.seriesguide.util.TaskManager;
import de.greenrobot.event.EventBus;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Multi-purpose "Add show" tab. Can display either the connected trakt user's recommendations,
 * library or watchlist.
 */
public class TraktAddFragment extends AddFragment {

    public static TraktAddFragment newInstance(int type) {
        TraktAddFragment f = new TraktAddFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt("traktlisttype", type);
        f.setArguments(args);

        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_addshow_trakt, container, false);
        ButterKnife.bind(this, v);

        // set initial view states
        setProgressVisible(true, false);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // setup adapter
        adapter = new AddAdapter(getActivity(), R.layout.item_addshow,
                new ArrayList<SearchResult>());

        // load data
        int type = getListType();
        getLoaderManager().initLoader(type, null, mTraktAddCallbacks);

        // add menu options
        if (type == AddPagerAdapter.LIBRARY_TAB_POSITION
                || type == AddPagerAdapter.WATCHLIST_TAB_POSITION) {
            setHasOptionsMenu(true);
        }
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
            if (searchResults != null) {
                List<SearchResult> showsToAdd = new LinkedList<>();
                // only include shows not already added
                for (SearchResult result : searchResults) {
                    if (!result.isAdded) {
                        showsToAdd.add(result);
                        result.isAdded = true;
                    }
                }
                TaskManager.getInstance(getActivity()).performAddTask(showsToAdd, false, false);
                EventBus.getDefault().post(new AddShowEvent());
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

    private LoaderManager.LoaderCallbacks<TraktAddLoader.Result> mTraktAddCallbacks
            = new LoaderManager.LoaderCallbacks<TraktAddLoader.Result>() {
        @Override
        public Loader<TraktAddLoader.Result> onCreateLoader(int id, Bundle args) {
            return new TraktAddLoader(getActivity(), getListType());
        }

        @Override
        public void onLoadFinished(Loader<TraktAddLoader.Result> loader,
                TraktAddLoader.Result data) {
            if (!isAdded()) {
                return;
            }
            setSearchResults(data.results);
            setEmptyMessage(data.emptyTextResId);
            setProgressVisible(false, true);
        }

        @Override
        public void onLoaderReset(Loader<TraktAddLoader.Result> loader) {
            // keep currently displayed data
        }
    };
}
