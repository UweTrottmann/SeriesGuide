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
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.provider.SeriesContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesContract.Lists;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.Utils;

/**
 * Displays one user created list which includes a mixture of shows, seasons and
 * episodes.
 */
public class ListsFragment extends SherlockFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener {

    private static final int LOADER_ID = R.layout.lists_fragment;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.lists_fragment, container, false);
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

        getLoaderManager().initLoader(LOADER_ID, getArguments(), this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.lists_menu, menu);
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

                int showId = listItem.getInt(ListItemsQuery.SHOW_ID);
                intent.putExtra(EpisodesActivity.InitBundle.SHOW_TVDBID, showId);

                int seasonNumber = listItem.getInt(ListItemsQuery.ITEM_TITLE);
                intent.putExtra(EpisodesActivity.InitBundle.SEASON_NUMBER, seasonNumber);

                startActivity(intent);
                break;
            }
            case 3: {
                // display episode details
                Intent intent = new Intent(getActivity(), EpisodeDetailsActivity.class);
                intent.putExtra(EpisodeDetailsActivity.InitBundle.EPISODE_TVDBID,
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
                }, null);
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

        public ListItemAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
            mInflater = LayoutInflater.from(context);
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
                convertView = mInflater.inflate(R.layout.show_rowairtime, null);

                viewHolder = new ViewHolder();
                viewHolder.name = (TextView) convertView.findViewById(R.id.seriesname);
                viewHolder.network = (TextView) convertView
                        .findViewById(R.id.TextViewShowListNetwork);
                viewHolder.episode = (TextView) convertView
                        .findViewById(R.id.TextViewShowListNextEpisode);
                viewHolder.episodeTime = (TextView) convertView.findViewById(R.id.episodetime);
                viewHolder.airsTime = (TextView) convertView
                        .findViewById(R.id.TextViewShowListAirtime);
                viewHolder.poster = (ImageView) convertView.findViewById(R.id.showposter);

                // hide unused views
                viewHolder.episodeTime.setVisibility(View.GONE);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            // show title and network
            viewHolder.name.setText(mCursor.getString(ListItemsQuery.SHOW_TITLE));
            viewHolder.network.setText(mCursor.getString(ListItemsQuery.SHOW_NETWORK));

            // airday
            final String[] values = Utils.parseMillisecondsToTime(
                    mCursor.getLong(ListItemsQuery.SHOW_AIRSTIME),
                    mCursor.getString(ListItemsQuery.SHOW_AIRSDAY), mContext);
            if (getResources().getBoolean(R.bool.isLargeTablet)) {
                viewHolder.airsTime.setText("/ " + values[1] + " " + values[0]);
            } else {
                viewHolder.airsTime.setText(values[1] + " " + values[0]);
            }

            // item title
            int itemType = mCursor.getInt(ListItemsQuery.ITEM_TYPE);
            String itemTitle;
            switch (itemType) {
                default:
                case 1:
                    // shows
                    itemTitle = "";
                    break;
                case 2:
                    // seasons
                    itemTitle = Utils.getSeasonString(mContext,
                            mCursor.getInt(ListItemsQuery.ITEM_TITLE));
                    break;
                case 3:
                    // episodes
                    itemTitle = mCursor.getString(ListItemsQuery.ITEM_TITLE);
                    break;
            }
            viewHolder.episode.setText(itemTitle);

            // poster
            final String imagePath = mCursor.getString(ListItemsQuery.SHOW_POSTER);
            ImageProvider.getInstance(mContext).loadPosterThumb(viewHolder.poster, imagePath);

            return convertView;
        }

        @Override
        public void bindView(View arg0, Context arg1, Cursor arg2) {
        }

        @Override
        public View newView(Context arg0, Cursor arg1, ViewGroup arg2) {
            return null;
        }

    }

    static class ViewHolder {

        public TextView name;

        public TextView network;

        public TextView episode;

        public TextView episodeTime;

        public TextView airsTime;

        public ImageView poster;
    }

    interface ListItemsQuery {

        String[] PROJECTION = new String[] {
                ListItems._ID, ListItems.LIST_ITEM_ID, ListItems.ITEM_REF_ID, ListItems.TYPE,
                Shows.REF_SHOW_ID, Shows.TITLE, Shows.OVERVIEW, Shows.POSTER, Shows.NETWORK,
                Shows.AIRSTIME, Shows.AIRSDAYOFWEEK
        };

        int LIST_ITEM_ID = 1;

        int ITEM_REF_ID = 2;

        int ITEM_TYPE = 3;

        int SHOW_ID = 4;

        int SHOW_TITLE = 5;

        int ITEM_TITLE = 6;

        int SHOW_POSTER = 7;

        int SHOW_NETWORK = 8;

        int SHOW_AIRSTIME = 9;

        int SHOW_AIRSDAY = 10;
    }

}
