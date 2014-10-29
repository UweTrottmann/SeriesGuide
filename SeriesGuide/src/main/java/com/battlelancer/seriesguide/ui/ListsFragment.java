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
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.PopupMenu;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.BaseShowsAdapter;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.ui.dialogs.ManageListsDialogFragment;
import com.battlelancer.seriesguide.util.SeasonTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.Utils;
import java.util.Date;

/**
 * Displays one user created list which includes a mixture of shows, seasons and episodes.
 */
public class ListsFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener, View.OnClickListener {

    private static final int LOADER_ID = R.layout.fragment_list;

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

    private ListItemAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new ListItemAdapter(getActivity(), null, 0);

        // setup grid view
        GridView list = (GridView) getView().findViewById(android.R.id.list);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(this);
        list.setEmptyView(getView().findViewById(android.R.id.empty));
        list.setFastScrollAlwaysVisible(false);
        list.setFastScrollEnabled(true);

        getLoaderManager().initLoader(LOADER_ID, getArguments(), this);
    }

    @Override
    public void onClick(View v) {
        getActivity().openContextMenu(v);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Cursor listItem = (Cursor) mAdapter.getItem(position);
        int itemType = listItem.getInt(ListItemsQuery.ITEM_TYPE);
        String itemRefId = listItem.getString(ListItemsQuery.ITEM_REF_ID);

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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String listId = args.getString(InitBundle.LIST_ID);
        return new CursorLoader(getActivity(), ListItems.CONTENT_WITH_DETAILS_URI,
                ListItemsQuery.PROJECTION,
                Lists.LIST_ID + "=?",
                new String[] {
                        listId
                }, ListItemsQuery.SORTING
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

    private class ListItemAdapter extends BaseShowsAdapter {

        public ListItemAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            BaseShowsAdapter.ViewHolder viewHolder = (BaseShowsAdapter.ViewHolder) view.getTag();

            // show title
            viewHolder.name.setText(cursor.getString(ListItemsQuery.SHOW_TITLE));

            // favorited label
            final boolean isFavorited = cursor.getInt(ListItemsQuery.SHOW_FAVORITE) == 1;
            viewHolder.favorited.setVisibility(isFavorited ? View.VISIBLE : View.GONE);

            // item title
            final int itemType = cursor.getInt(ListItemsQuery.ITEM_TYPE);
            switch (itemType) {
                default:
                case 1:
                    // shows

                    // network, day and time
                    viewHolder.timeAndNetwork.setText(buildNetworkAndTimeString(context,
                            cursor.getInt(ListItemsQuery.SHOW_OR_EPISODE_RELEASE_TIME),
                            cursor.getInt(ListItemsQuery.SHOW_RELEASE_WEEKDAY),
                            cursor.getString(ListItemsQuery.SHOW_RELEASE_TIMEZONE),
                            cursor.getString(ListItemsQuery.SHOW_RELEASE_COUNTRY),
                            cursor.getString(ListItemsQuery.SHOW_NETWORK)));

                    // next episode info
                    String fieldValue = cursor.getString(ListItemsQuery.SHOW_NEXTTEXT);
                    if (TextUtils.isEmpty(fieldValue)) {
                        // show show status if there are currently no more
                        // episodes
                        int status = cursor.getInt(ListItemsQuery.SHOW_STATUS);

                        // Continuing == 1 and Ended == 0
                        if (status == 1) {
                            viewHolder.episodeTime.setText(getString(R.string.show_isalive));
                        } else if (status == 0) {
                            viewHolder.episodeTime.setText(getString(R.string.show_isnotalive));
                        } else {
                            viewHolder.episodeTime.setText("");
                        }
                        viewHolder.episode.setText("");
                    } else {
                        viewHolder.episode.setText(fieldValue);
                        fieldValue = cursor.getString(ListItemsQuery.SHOW_NEXTAIRDATETEXT);
                        viewHolder.episodeTime.setText(fieldValue);
                    }
                    break;
                case 2:
                    // seasons
                    viewHolder.timeAndNetwork.setText(R.string.season);
                    viewHolder.episode.setText(SeasonTools.getSeasonString(context,
                            cursor.getInt(ListItemsQuery.ITEM_TITLE)));
                    viewHolder.episodeTime.setText("");
                    break;
                case 3:
                    // episodes
                    viewHolder.timeAndNetwork.setText(R.string.episode);
                    viewHolder.episode.setText(Utils.getNextEpisodeString(context,
                            cursor.getInt(ListItemsQuery.SHOW_NEXTTEXT),
                            cursor.getInt(ListItemsQuery.SHOW_NEXTAIRDATETEXT),
                            cursor.getString(ListItemsQuery.ITEM_TITLE)));
                    long releaseTime = cursor.getLong(ListItemsQuery.SHOW_OR_EPISODE_RELEASE_TIME);
                    if (releaseTime != -1) {
                        // "in 15 mins (Fri)"
                        Date actualRelease = TimeTools.getEpisodeReleaseTime(context, releaseTime);
                        viewHolder.episodeTime.setText(getString(R.string.release_date_and_day,
                                TimeTools.formatToRelativeLocalReleaseTime(context, actualRelease),
                                TimeTools.formatToLocalReleaseDay(actualRelease)));
                    }
                    break;
            }

            // poster
            Utils.loadPosterThumbnail(context, viewHolder.poster,
                    cursor.getString(ListItemsQuery.SHOW_POSTER));

            // context menu
            viewHolder.contextMenu.setVisibility(View.VISIBLE);
            final String itemId = cursor.getString(ListItemsQuery.LIST_ITEM_ID);
            final int itemTvdbId = cursor.getInt(ListItemsQuery.ITEM_REF_ID);
            viewHolder.contextMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                    popupMenu.inflate(R.menu.lists_popup_menu);
                    popupMenu.setOnMenuItemClickListener(
                            new PopupMenuItemClickListener(itemId, itemTvdbId, itemType));
                    popupMenu.show();
                }
            });
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = super.newView(context, cursor, parent);

            ViewHolder viewHolder = (ViewHolder) v.getTag();
            viewHolder.favorited.setBackgroundResource(0); // remove selectable background

            return v;
        }

        private class PopupMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

            private final String mItemId;
            private final int mItemTvdbId;
            private final int mItemType;

            public PopupMenuItemClickListener(String itemId, int itemTvdbId, int itemType) {
                mItemId = itemId;
                mItemTvdbId = itemTvdbId;
                mItemType = itemType;
            }

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_action_lists_manage: {
                        ManageListsDialogFragment.showListsDialog(mItemTvdbId, mItemType,
                                getFragmentManager());
                        fireTrackerEvent("Manage lists");
                        return true;
                    }
                    case R.id.menu_action_lists_remove: {
                        getActivity().getContentResolver()
                                .delete(ListItems.buildListItemUri(mItemId), null, null);
                        getActivity().getContentResolver()
                                .notifyChange(ListItems.CONTENT_WITH_DETAILS_URI, null);
                        fireTrackerEvent("Remove from mList");
                        return true;
                    }
                }
                return false;
            }
        }
    }

    interface ListItemsQuery {

        String[] PROJECTION = new String[] {
                ListItems._ID,
                ListItems.LIST_ITEM_ID, // 1
                ListItems.ITEM_REF_ID,
                ListItems.TYPE,
                Shows.REF_SHOW_ID,
                Shows.TITLE,
                Shows.OVERVIEW,
                Shows.POSTER,
                Shows.NETWORK,
                Shows.RELEASE_TIME,
                Shows.RELEASE_WEEKDAY,
                Shows.RELEASE_TIMEZONE,
                Shows.RELEASE_COUNTRY,
                Shows.STATUS,
                Shows.NEXTTEXT,
                Shows.NEXTAIRDATETEXT,
                Shows.FAVORITE
        };

        String SORTING = Shows.TITLE + " COLLATE NOCASE ASC, " + ListItems.TYPE + " ASC";

        int LIST_ITEM_ID = 1;
        int ITEM_REF_ID = 2;
        int ITEM_TYPE = 3;
        int SHOW_ID = 4;
        int SHOW_TITLE = 5;
        int ITEM_TITLE = 6;
        int SHOW_POSTER = 7;
        int SHOW_NETWORK = 8;
        int SHOW_OR_EPISODE_RELEASE_TIME = 9;
        int SHOW_RELEASE_WEEKDAY = 10;
        int SHOW_RELEASE_TIMEZONE = 11;
        int SHOW_RELEASE_COUNTRY = 12;
        int SHOW_STATUS = 13;
        int SHOW_NEXTTEXT = 14;
        int SHOW_NEXTAIRDATETEXT = 15;
        int SHOW_FAVORITE = 16;
    }

    private void fireTrackerEvent(String label) {
        Utils.trackContextMenu(getActivity(), TAG, label);
    }
}
