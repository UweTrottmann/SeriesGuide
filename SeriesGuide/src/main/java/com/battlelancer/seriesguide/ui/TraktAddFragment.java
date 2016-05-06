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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.loaders.TraktAddLoader;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.widgets.EmptyView;
import de.greenrobot.event.EventBus;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Multi-purpose "Add show" tab. Can display either the connected trakt user's recommendations,
 * library or watchlist.
 */
public class TraktAddFragment extends AddFragment {

    /**
     * Which trakt list should be shown. One of {@link TraktAddFragment.ListType}.
     */
    public final static String ARG_TYPE = "traktListType";

    @IntDef({ TYPE_RECOMMENDED, TYPE_WATCHED, TYPE_COLLECTION, TYPE_WATCHLIST })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ListType {
    }

    public final static int TYPE_RECOMMENDED = 0;
    public final static int TYPE_WATCHED = 1;
    public final static int TYPE_COLLECTION = 2;
    public final static int TYPE_WATCHLIST = 3;

    public static TraktAddFragment newInstance(int type) {
        TraktAddFragment f = new TraktAddFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, type);
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

        // setup adapter, enable context menu only for recommendations and watchlist
        int listType = getListType();
        adapter = new AddAdapter(getActivity(), new ArrayList<SearchResult>(),
                listType == TYPE_RECOMMENDED || listType == TYPE_WATCHLIST ? showMenuClickListener
                        : null, listType != TYPE_WATCHLIST);

        // load data
        getLoaderManager().initLoader(SearchActivity.TRAKT_BASE_LOADER_ID + listType, null,
                mTraktAddCallbacks);

        // add menu options
        setHasOptionsMenu(true);
    }

    private AddAdapter.OnContextMenuClickListener showMenuClickListener
            = new AddAdapter.OnContextMenuClickListener() {
        @Override
        public void onClick(View view, int showTvdbId) {
            PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
            popupMenu.inflate(R.menu.add_dialog_popup_menu);

            int listType = getListType();
            if (listType == TYPE_RECOMMENDED) {
                popupMenu.getMenu()
                        .findItem(R.id.menu_action_show_watchlist_remove)
                        .setVisible(false);
            } else if (listType == TYPE_WATCHLIST) {
                popupMenu.getMenu().findItem(R.id.menu_action_show_watchlist_add).setVisible(false);
            }

            popupMenu.setOnMenuItemClickListener(
                    new AddItemMenuItemClickListener(view.getContext(), showTvdbId));
            popupMenu.show();
        }
    };

    public static class AddItemMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

        private final Context context;
        private final int showTvdbId;

        public AddItemMenuItemClickListener(Context context, int showTvdbId) {
            this.context = context;
            this.showTvdbId = showTvdbId;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_action_show_watchlist_add) {
                ShowTools.addToWatchlist(context, showTvdbId);
                return true;
            }
            if (itemId == R.id.menu_action_show_watchlist_remove) {
                ShowTools.removeFromWatchlist(context, showTvdbId);
                return true;
            }
            return false;
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

    public void onEventMainThread(
            @SuppressWarnings("UnusedParameters") ShowTools.ShowChangedEvent event) {
        int listType = getListType();
        if (listType == TYPE_WATCHLIST) {
            // reload watchlist if a show was removed
            getLoaderManager().restartLoader(SearchActivity.TRAKT_BASE_LOADER_ID + listType, null,
                    mTraktAddCallbacks);
        }
    }

    @Override
    protected void setupEmptyView(EmptyView emptyView) {
        emptyView.setButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setProgressVisible(true, false);
                getLoaderManager().restartLoader(
                        SearchActivity.TRAKT_BASE_LOADER_ID + getListType(), null,
                        mTraktAddCallbacks);
            }
        });
    }

    @ListType
    private int getListType() {
        final int type = getArguments().getInt(ARG_TYPE);
        switch (type) {
            case TYPE_RECOMMENDED:
                return TYPE_RECOMMENDED;
            case TYPE_COLLECTION:
                return TYPE_COLLECTION;
            case TYPE_WATCHLIST:
                return TYPE_WATCHLIST;
            case TYPE_WATCHED:
            default:
                return TYPE_WATCHED;
        }
    }

    private LoaderManager.LoaderCallbacks<TraktAddLoader.Result> mTraktAddCallbacks
            = new LoaderManager.LoaderCallbacks<TraktAddLoader.Result>() {
        @Override
        public Loader<TraktAddLoader.Result> onCreateLoader(int id, Bundle args) {
            return new TraktAddLoader(getContext(), getListType());
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
