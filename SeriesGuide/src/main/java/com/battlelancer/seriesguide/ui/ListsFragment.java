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
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.PopupMenu;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.ListItemsAdapter;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.ListsDistillationSettings;
import com.battlelancer.seriesguide.ui.dialogs.ManageListsDialogFragment;
import com.battlelancer.seriesguide.util.Utils;
import de.greenrobot.event.EventBus;

/**
 * Displays one user created list which includes a mixture of shows, seasons and episodes.
 */
public class ListsFragment extends Fragment implements OnItemClickListener, View.OnClickListener {

    /** LoaderManager is created unique to fragment, so use same id for all of them */
    private static final int LOADER_ID = 1;

    private static final String TAG = "Lists";

    public static ListsFragment newInstance(String list_id) {
        ListsFragment f = new ListsFragment();

        Bundle args = new Bundle();
        args.putString(InitBundle.LIST_ID, list_id);
        f.setArguments(args);

        return f;
    }

    interface InitBundle {

        String LIST_ID = "list_id";
    }

    private ListItemsAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new ListItemsAdapter(getActivity(), onContextMenuClickListener);

        // setup grid view
        GridView list = (GridView) getView().findViewById(android.R.id.list);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(this);
        list.setEmptyView(getView().findViewById(android.R.id.empty));
        list.setFastScrollAlwaysVisible(false);
        list.setFastScrollEnabled(true);

        getLoaderManager().initLoader(LOADER_ID, getArguments(), loaderCallbacks);
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onClick(View v) {
        getActivity().openContextMenu(v);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Cursor listItem = (Cursor) mAdapter.getItem(position);
        int itemType = listItem.getInt(ListItemsAdapter.Query.ITEM_TYPE);
        String itemRefId = listItem.getString(ListItemsAdapter.Query.ITEM_REF_ID);

        Intent intent = null;
        switch (itemType) {
            case 1: {
                // display show overview
                intent = new Intent(getActivity(), OverviewActivity.class);
                intent.putExtra(OverviewFragment.InitBundle.SHOW_TVDBID,
                        Integer.valueOf(itemRefId));
                break;
            }
            case 2: {
                // display episodes of season
                intent = new Intent(getActivity(), EpisodesActivity.class);
                intent.putExtra(EpisodesActivity.InitBundle.SEASON_TVDBID,
                        Integer.valueOf(itemRefId));
                break;
            }
            case 3: {
                // display episode details
                intent = new Intent(getActivity(), EpisodesActivity.class);
                intent.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID,
                        Integer.valueOf(itemRefId));
                break;
            }
        }

        if (intent != null) {
            ActivityCompat.startActivity(getActivity(), intent,
                    ActivityOptionsCompat
                            .makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight())
                            .toBundle()
            );
        }
    }

    public void onEventMainThread(ListsDistillationSettings.ListsSortOrderChangedEvent event) {
        // sort order has changed, reload lists
        getLoaderManager().restartLoader(LOADER_ID, getArguments(), loaderCallbacks);
    }

    private LoaderManager.LoaderCallbacks<Cursor> loaderCallbacks
            = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String listId = args.getString(InitBundle.LIST_ID);
            return new CursorLoader(getActivity(), ListItems.CONTENT_WITH_DETAILS_URI,
                    ListItemsAdapter.Query.PROJECTION,
                    // items of this list, but exclude any if show was removed from the database
                    // (the join on show data will fail, hence the show id will be 0/null)
                    Lists.LIST_ID + "=? AND " + Shows.REF_SHOW_ID + ">0",
                    new String[] {
                            listId
                    }, ListsDistillationSettings.getSortQuery(getActivity())
            );
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mAdapter.swapCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mAdapter.swapCursor(null);
        }
    };

    private ListItemsAdapter.OnContextMenuClickListener onContextMenuClickListener
            = new ListItemsAdapter.OnContextMenuClickListener() {
        @Override
        public void onClick(View view, ListItemsAdapter.ListItemViewHolder viewHolder) {
            PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
            popupMenu.inflate(R.menu.lists_popup_menu);
            popupMenu.setOnMenuItemClickListener(
                    new PopupMenuItemClickListener(view.getContext(), getFragmentManager(),
                            viewHolder.itemId, viewHolder.itemTvdbId, viewHolder.itemType));
            popupMenu.show();
        }
    };

    private static class PopupMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

        private final Context context;
        private final FragmentManager fragmentManager;
        private final String itemId;
        private final int itemTvdbId;
        private final int itemType;

        public PopupMenuItemClickListener(Context context, FragmentManager fm, String itemId,
                int itemTvdbId, int itemType) {
            this.context = context;
            this.fragmentManager = fm;
            this.itemId = itemId;
            this.itemTvdbId = itemTvdbId;
            this.itemType = itemType;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_action_lists_manage: {
                    ManageListsDialogFragment.showListsDialog(itemTvdbId, itemType,
                            fragmentManager);
                    fireTrackerEvent("Manage lists");
                    return true;
                }
                case R.id.menu_action_lists_remove: {
                    context.getContentResolver()
                            .delete(ListItems.buildListItemUri(itemId), null, null);
                    context.getContentResolver()
                            .notifyChange(ListItems.CONTENT_WITH_DETAILS_URI, null);
                    fireTrackerEvent("Remove from mList");
                    return true;
                }
            }
            return false;
        }

        private void fireTrackerEvent(String label) {
            Utils.trackContextMenu(context, TAG, label);
        }
    }
}
