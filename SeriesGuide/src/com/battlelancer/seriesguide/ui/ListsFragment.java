/*
 * Copyright 2012 Uwe Trottmann
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.battlelancer.seriesguide.provider.SeriesContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesContract.Lists;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.ui.ShowsFragment.ViewHolder;
import com.battlelancer.seriesguide.ui.dialogs.ListsDialogFragment;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.seriesguide.R;

/**
 * Displays one user created mList which includes a mixture of shows, seasons
 * and episodes.
 */
public class ListsFragment extends SherlockFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener {

    private static final int LOADER_ID = R.layout.lists_fragment;

    private static final int CONTEXT_REMOVE_ID = 300;

    private static final int CONTEXT_MANAGE_LISTS_ID = 301;

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

    private GridView mList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.lists_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new ListItemAdapter(getActivity(), null, 0);

        // setup grid view
        mList = (GridView) getView().findViewById(android.R.id.list);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(this);
        mList.setEmptyView(getView().findViewById(android.R.id.empty));

        registerForContextMenu(mList);

        getLoaderManager().initLoader(LOADER_ID, getArguments(), this);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(0, CONTEXT_MANAGE_LISTS_ID, 0, R.string.list_item_manage);
        menu.add(0, CONTEXT_REMOVE_ID, 1, R.string.list_item_remove);
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

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case CONTEXT_REMOVE_ID: {
                String itemId = ((Cursor) mAdapter.getItem(info.position))
                        .getString(ListItemsQuery.LIST_ITEM_ID);
                getActivity().getContentResolver().delete(ListItems.buildListItemUri(itemId), null,
                        null);
                getActivity().getContentResolver().notifyChange(ListItems.CONTENT_WITH_DETAILS_URI,
                        null);

                fireTrackerEvent("Remove from mList");
                return true;
            }
            case CONTEXT_MANAGE_LISTS_ID: {
                final Cursor listItem = (Cursor) mAdapter.getItem(info.position);
                String tvdbId = listItem.getString(ListItemsQuery.ITEM_REF_ID);
                int type = listItem.getInt(ListItemsQuery.ITEM_TYPE);
                ListsDialogFragment.showListsDialog(tvdbId, type,
                        getFragmentManager());

                fireTrackerEvent("Manage lists");
                return true;
            }
            default:
                return super.onContextItemSelected(item);
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Cursor listItem = (Cursor) mAdapter.getItem(position);
        int itemType = listItem.getInt(ListItemsQuery.ITEM_TYPE);
        String itemRefId = listItem.getString(ListItemsQuery.ITEM_REF_ID);

        switch (itemType) {
            case 1: {
                // display show overview
                Intent intent = new Intent(getActivity(), OverviewActivity.class);
                intent.putExtra(OverviewFragment.InitBundle.SHOW_TVDBID, Integer.valueOf(itemRefId));
                startActivity(intent);
                break;
            }
            case 2: {
                // display episodes of season
                Intent intent = new Intent(getActivity(), EpisodesActivity.class);
                intent.putExtra(EpisodesActivity.InitBundle.SEASON_TVDBID,
                        Integer.valueOf(itemRefId));
                startActivity(intent);
                break;
            }
            case 3: {
                // display episode details
                Intent intent = new Intent(getActivity(), EpisodesActivity.class);
                intent.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID,
                        Integer.valueOf(itemRefId));
                startActivity(intent);
                break;
            }
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
                }, ListItemsQuery.SORTING);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    private class ListItemAdapter extends CursorAdapter {

        private LayoutInflater mInflater;
        private SharedPreferences mPrefs;

        public ListItemAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
            mInflater = LayoutInflater.from(context);
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (!mDataValid) {
                throw new IllegalStateException(
                        "this should only be called when the cursor is valid");
            }
            if (!mCursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }

            final ViewHolder viewHolder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.shows_row, null);

                viewHolder = new ViewHolder();
                viewHolder.name = (TextView) convertView.findViewById(R.id.seriesname);
                viewHolder.timeAndNetwork = (TextView) convertView
                        .findViewById(R.id.textViewShowsTimeAndNetwork);
                viewHolder.episode = (TextView) convertView
                        .findViewById(R.id.TextViewShowListNextEpisode);
                viewHolder.episodeTime = (TextView) convertView.findViewById(R.id.episodetime);
                viewHolder.poster = (ImageView) convertView.findViewById(R.id.showposter);
                viewHolder.favorited = (ImageView) convertView.findViewById(R.id.favoritedLabel);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            // show title
            viewHolder.name.setText(mCursor.getString(ListItemsQuery.SHOW_TITLE));

            // favorite star
            final boolean isFavorited = mCursor.getInt(ListItemsQuery.SHOW_FAVORITE) == 1;
            viewHolder.favorited.setVisibility(isFavorited ? View.VISIBLE : View.GONE);

            // item title
            int itemType = mCursor.getInt(ListItemsQuery.ITEM_TYPE);
            switch (itemType) {
                default:
                case 1:
                    // shows

                    // air time and network
                    final String[] values = Utils.parseMillisecondsToTime(
                            mCursor.getLong(ListItemsQuery.AIRSTIME),
                            mCursor.getString(ListItemsQuery.SHOW_AIRSDAY), mContext);
                    if (getResources().getBoolean(R.bool.isLargeTablet)) {
                        // network first, then time, one line
                        viewHolder.timeAndNetwork.setText(mCursor
                                .getString(ListItemsQuery.SHOW_NETWORK) + " / "
                                + values[1] + " " + values[0]);
                    } else {
                        // smaller screen, time first, network second line
                        viewHolder.timeAndNetwork.setText(values[1] + " " + values[0] + "\n"
                                + mCursor.getString(ListItemsQuery.SHOW_NETWORK));
                    }

                    // next episode info
                    String fieldValue = mCursor.getString(ListItemsQuery.SHOW_NEXTTEXT);
                    if (TextUtils.isEmpty(fieldValue)) {
                        // show show status if there are currently no more
                        // episodes
                        int status = mCursor.getInt(ListItemsQuery.SHOW_STATUS);

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
                        fieldValue = mCursor.getString(ListItemsQuery.SHOW_NEXTAIRDATETEXT);
                        viewHolder.episodeTime.setText(fieldValue);
                    }
                    break;
                case 2:
                    // seasons
                    viewHolder.timeAndNetwork.setText(R.string.season);
                    viewHolder.episode.setText(Utils.getSeasonString(mContext,
                            mCursor.getInt(ListItemsQuery.ITEM_TITLE)));
                    viewHolder.episodeTime.setText("");
                    break;
                case 3:
                    // episodes
                    viewHolder.timeAndNetwork.setText(R.string.episode);
                    viewHolder.episode.setText(Utils.getNextEpisodeString(mPrefs,
                            mCursor.getInt(ListItemsQuery.SHOW_NEXTTEXT),
                            mCursor.getInt(ListItemsQuery.SHOW_NEXTAIRDATETEXT),
                            mCursor.getString(ListItemsQuery.ITEM_TITLE)));
                    long airtime = mCursor.getLong(ListItemsQuery.AIRSTIME);
                    if (airtime != -1) {
                        final String[] dayAndTime = Utils
                                .formatToTimeAndDay(airtime, getActivity());
                        viewHolder.episodeTime.setText(new StringBuilder().append(dayAndTime[2])
                                .append(" (")
                                .append(dayAndTime[1])
                                .append(")"));
                    }
                    break;
            }

            // poster
            final String imagePath = mCursor.getString(ListItemsQuery.SHOW_POSTER);
            ImageProvider.getInstance(mContext).loadPosterThumb(viewHolder.poster, imagePath);

            return convertView;
        }

        @Override
        public void bindView(View arg0, Context arg1, Cursor arg2) {
            // do nothing here
        }

        @Override
        public View newView(Context arg0, Cursor arg1, ViewGroup arg2) {
            return null;
        }

    }

    interface ListItemsQuery {

        String[] PROJECTION = new String[] {
                ListItems._ID, ListItems.LIST_ITEM_ID, ListItems.ITEM_REF_ID, ListItems.TYPE,
                Shows.REF_SHOW_ID, Shows.TITLE, Shows.OVERVIEW, Shows.POSTER, Shows.NETWORK,
                Shows.AIRSTIME, Shows.AIRSDAYOFWEEK, Shows.STATUS, Shows.NEXTTEXT,
                Shows.NEXTAIRDATETEXT, Shows.FAVORITE
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

        int AIRSTIME = 9;

        int SHOW_AIRSDAY = 10;

        int SHOW_STATUS = 11;

        int SHOW_NEXTTEXT = 12;

        int SHOW_NEXTAIRDATETEXT = 13;

        int SHOW_FAVORITE = 14;

    }

    private static void fireTrackerEvent(String label) {
        EasyTracker.getTracker().sendEvent(TAG, "Context Item", label, (long) 0);
    }
}
