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

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.battlelancer.seriesguide.Constants.ShowSorting;
import com.battlelancer.seriesguide.provider.SeriesContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.ui.dialogs.CheckInDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.ConfirmDeleteDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.ListsDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.SortDialogFragment;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.FlagTask.FlagTaskCompletedEvent;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.seriesguide.R;

import de.greenrobot.event.EventBus;

/**
 * Displays the list of shows in a users local library.
 * 
 * @author Uwe Trottmann
 */
public class ShowsFragment extends SherlockFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener, OnClickListener {

    private static final String TAG = "Shows";

    public static final int LOADER_ID = R.layout.shows_fragment;

    public static final String FILTER_ID = "filterid";

    // context menu items
    private static final int CONTEXT_DELETE_ID = 200;

    private static final int CONTEXT_UPDATE_ID = 201;

    private static final int CONTEXT_FLAG_NEXT_ID = 202;

    private static final int CONTEXT_FAVORITE_ID = 203;

    private static final int CONTEXT_UNFAVORITE_ID = 204;

    private static final int CONTEXT_HIDE_ID = 205;

    private static final int CONTEXT_UNHIDE_ID = 206;

    private static final int CONTEXT_MANAGE_LISTS_ID = 207;

    private static final int CONTEXT_CHECKIN_ID = 208;

    // Show Filter Ids
    private static final int SHOWFILTER_ALL = 0;

    private static final int SHOWFILTER_FAVORITES = 1;

    private static final int SHOWFILTER_UNSEENEPISODES = 2;

    private static final int SHOWFILTER_HIDDEN = 3;

    private SlowAdapter mAdapter;

    private GridView mGrid;

    private ShowSorting mSorting;

    public static ShowsFragment newInstance() {
        ShowsFragment f = new ShowsFragment();
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.shows_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());

        // get settings
        updateSorting(prefs);
        int showfilter = prefs.getInt(SeriesGuidePreferences.KEY_SHOWFILTER, 0);

        int resIdStar = Utils.resolveAttributeToResourceId(getSherlockActivity().getTheme(),
                R.attr.drawableStar);
        int resIdStarZero = Utils.resolveAttributeToResourceId(getSherlockActivity().getTheme(),
                R.attr.drawableStar0);
        mAdapter = new SlowAdapter(getActivity(), null, 0, resIdStar, resIdStarZero, this);

        // setup grid view
        mGrid = (GridView) getView().findViewById(R.id.showlist);
        mGrid.setAdapter(mAdapter);
        mGrid.setOnItemClickListener(this);
        setEmptyView(showfilter);
        registerForContextMenu(mGrid);

        // start loading data, use saved show filter
        Bundle args = new Bundle();
        args.putInt(FILTER_ID, showfilter);
        getLoaderManager().initLoader(LOADER_ID, args, this);

        // listen for some settings changes
        prefs.registerOnSharedPreferenceChangeListener(mPrefsListener);
    }

    private void setEmptyView(int showfilter) {
        View oldEmptyView = mGrid.getEmptyView();

        View emptyView = null;
        switch (showfilter) {
            case SHOWFILTER_FAVORITES:
                emptyView = getView().findViewById(R.id.emptyFavorites);
                break;
            case SHOWFILTER_HIDDEN:
                emptyView = getView().findViewById(R.id.emptyHidden);
                break;
            default:
                emptyView = getView().findViewById(R.id.empty);
                break;
        }
        if (emptyView != null) {
            mGrid.setEmptyView(emptyView);
        }

        if (oldEmptyView != null) {
            oldEmptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(mPrefsListener);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menuInfo.toString();

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        final Cursor show = getActivity().getContentResolver().query(
                Shows.buildShowUri(String.valueOf(info.id)), new String[] {
                        Shows.FAVORITE, Shows.HIDDEN
                }, null, null, null);
        show.moveToFirst();
        if (show.getInt(0) == 0) {
            menu.add(0, CONTEXT_FAVORITE_ID, 2, R.string.context_favorite);
        } else {
            menu.add(0, CONTEXT_UNFAVORITE_ID, 2, R.string.context_unfavorite);
        }
        if (show.getInt(1) == 0) {
            menu.add(0, CONTEXT_HIDE_ID, 3, R.string.context_hide);
        } else {
            menu.add(0, CONTEXT_UNHIDE_ID, 3, R.string.context_unhide);
        }
        show.close();

        menu.add(0, CONTEXT_CHECKIN_ID, 0, R.string.checkin);
        menu.add(0, CONTEXT_FLAG_NEXT_ID, 1, R.string.context_marknext);
        menu.add(0, CONTEXT_MANAGE_LISTS_ID, 4, R.string.list_item_manage);
        menu.add(0, CONTEXT_UPDATE_ID, 5, R.string.context_updateshow);
        menu.add(0, CONTEXT_DELETE_ID, 6, R.string.delete_show);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case CONTEXT_CHECKIN_ID: {
                fireTrackerEvent("Check in");

                Cursor show = (Cursor) mAdapter.getItem(info.position);
                int episodeTvdbId = show.getInt(ShowsQuery.NEXTEPISODE);
                if (episodeTvdbId <= 0) {
                    return true;
                }

                // display a check-in dialog
                CheckInDialogFragment f = CheckInDialogFragment.newInstance(getActivity(),
                        episodeTvdbId);
                f.show(getFragmentManager(), "checkin-dialog");

                return true;
            }
            case CONTEXT_FAVORITE_ID: {
                onFavoriteShow(String.valueOf(info.id), true);
                return true;
            }
            case CONTEXT_UNFAVORITE_ID: {
                onFavoriteShow(String.valueOf(info.id), false);
                return true;
            }
            case CONTEXT_HIDE_ID: {
                fireTrackerEvent("Hide show");

                ContentValues values = new ContentValues();
                values.put(Shows.HIDDEN, true);
                getActivity().getContentResolver().update(
                        Shows.buildShowUri(String.valueOf(info.id)), values, null, null);
                Toast.makeText(getActivity(), getString(R.string.hidden), Toast.LENGTH_SHORT)
                        .show();
                return true;
            }
            case CONTEXT_UNHIDE_ID: {
                fireTrackerEvent("Unhide show");

                ContentValues values = new ContentValues();
                values.put(Shows.HIDDEN, false);
                getActivity().getContentResolver().update(
                        Shows.buildShowUri(String.valueOf(info.id)), values, null, null);
                Toast.makeText(getActivity(), getString(R.string.unhidden), Toast.LENGTH_SHORT)
                        .show();
                return true;
            }
            case CONTEXT_DELETE_ID:
                if (!SgSyncAdapter.isSyncActive(getActivity(), true)) {
                    showDeleteDialog(info.id);
                }

                fireTrackerEvent("Delete show");
                return true;
            case CONTEXT_UPDATE_ID:
                SgSyncAdapter.requestSync(getActivity(), (int) info.id);

                fireTrackerEvent("Update show");
                return true;
            case CONTEXT_FLAG_NEXT_ID:
                fireTrackerEvent("Mark next episode");

                Cursor show = (Cursor) mAdapter.getItem(info.position);
                DBUtils.markNextEpisode(getActivity(), (int) info.id,
                        show.getInt(ShowsQuery.NEXTEPISODE));

                return true;
            case CONTEXT_MANAGE_LISTS_ID: {
                fireTrackerEvent("Manage lists");

                ListsDialogFragment.showListsDialog(String.valueOf(info.id), ListItemTypes.SHOW,
                        getFragmentManager());
                return true;
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        getActivity().openContextMenu(v);
    }

    @TargetApi(16)
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // display overview for this show

        Intent i = new Intent(getActivity(), OverviewActivity.class);
        i.putExtra(OverviewFragment.InitBundle.SHOW_TVDBID, (int) id);
        startActivity(i);
        getActivity().overridePendingTransition(R.anim.blow_up_enter, R.anim.blow_up_exit);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String selection = null;
        String[] selectionArgs = null;

        int filterId = args.getInt(FILTER_ID);
        switch (filterId) {
            case SHOWFILTER_ALL:
                selection = Shows.HIDDEN + "=?";
                selectionArgs = new String[] {
                        "0"
                };
                break;
            case SHOWFILTER_FAVORITES:
                selection = Shows.FAVORITE + "=? AND " + Shows.HIDDEN + "=?";
                selectionArgs = new String[] {
                        "1", "0"
                };
                break;
            case SHOWFILTER_UNSEENEPISODES:
                SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(getActivity());
                int upcomingLimit = Integer.valueOf(prefs.getString(
                        SeriesGuidePreferences.KEY_UPCOMING_LIMIT, "1"));

                selection = Shows.NEXTAIRDATEMS + "!=? AND " + Shows.NEXTAIRDATEMS + " <=? AND "
                        + Shows.HIDDEN + "=?";
                // Display shows upcoming within x amount of days + 1 hour
                String inTheFuture = String.valueOf(System.currentTimeMillis() + upcomingLimit
                        * DateUtils.DAY_IN_MILLIS + DateUtils.HOUR_IN_MILLIS);
                selectionArgs = new String[] {
                        DBUtils.UNKNOWN_NEXT_AIR_DATE, inTheFuture, "0"
                };
                break;
            case SHOWFILTER_HIDDEN:
                selection = Shows.HIDDEN + "=?";
                selectionArgs = new String[] {
                        "1"
                };
                break;
        }

        return new CursorLoader(getActivity(), Shows.CONTENT_URI, ShowsQuery.PROJECTION, selection,
                selectionArgs, mSorting.query());
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> arg0) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    private class SlowAdapter extends CursorAdapter {

        private final int LAYOUT = R.layout.shows_row;

        private LayoutInflater mLayoutInflater;

        private OnClickListener mOnClickListener;

        private int mStarDrawableId;

        private int mStarZeroDrawableId;

        public SlowAdapter(Context context, Cursor c, int flags, int starDrawableResId,
                int starZeroDrawableId, OnClickListener listener) {
            super(context, c, flags);
            mLayoutInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mStarDrawableId = starDrawableResId;
            mStarZeroDrawableId = starZeroDrawableId;
            mOnClickListener = listener;
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
                convertView = mLayoutInflater.inflate(LAYOUT, null);

                viewHolder = new ViewHolder();
                viewHolder.name = (TextView) convertView.findViewById(R.id.seriesname);
                viewHolder.timeAndNetwork = (TextView) convertView
                        .findViewById(R.id.textViewShowsTimeAndNetwork);
                viewHolder.episode = (TextView) convertView
                        .findViewById(R.id.TextViewShowListNextEpisode);
                viewHolder.episodeTime = (TextView) convertView.findViewById(R.id.episodetime);
                viewHolder.poster = (ImageView) convertView.findViewById(R.id.showposter);
                viewHolder.favorited = (ImageView) convertView.findViewById(R.id.favoritedLabel);
                viewHolder.contextMenu = (ImageView) convertView
                        .findViewById(R.id.imageViewShowsContextMenu);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            // set text properties immediately
            viewHolder.name.setText(mCursor.getString(ShowsQuery.TITLE));

            // favorite toggle
            final String showId = mCursor.getString(ShowsQuery._ID);
            final boolean isFavorited = mCursor.getInt(ShowsQuery.FAVORITE) == 1;
            viewHolder.favorited.setImageResource(isFavorited ? mStarDrawableId
                    : mStarZeroDrawableId);
            viewHolder.favorited.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onFavoriteShow(showId, !isFavorited);
                }
            });

            // next episode info
            String fieldValue = mCursor.getString(ShowsQuery.NEXTTEXT);
            if (fieldValue.length() == 0) {
                // show show status if there are currently no more
                // episodes
                int status = mCursor.getInt(ShowsQuery.STATUS);

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
                fieldValue = mCursor.getString(ShowsQuery.NEXTAIRDATETEXT);
                viewHolder.episodeTime.setText(fieldValue);
            }

            // airday
            final String[] values = Utils.parseMillisecondsToTime(
                    mCursor.getLong(ShowsQuery.AIRSTIME),
                    mCursor.getString(ShowsQuery.AIRSDAYOFWEEK), mContext);
            if (getResources().getBoolean(R.bool.isLargeTablet)) {
                // network first, then time, one line
                viewHolder.timeAndNetwork.setText(mCursor.getString(ShowsQuery.NETWORK) + " / "
                        + values[1] + " " + values[0]);
            } else {
                // smaller screen, time first, network second line
                viewHolder.timeAndNetwork.setText(values[1] + " " + values[0] + "\n"
                        + mCursor.getString(ShowsQuery.NETWORK));
            }

            // set poster
            final String imagePath = mCursor.getString(ShowsQuery.POSTER);
            ImageProvider.getInstance(mContext).loadPosterThumb(viewHolder.poster, imagePath);

            // context menu
            viewHolder.contextMenu.setVisibility(View.VISIBLE);
            viewHolder.contextMenu.setOnClickListener(mOnClickListener);

            return convertView;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // do nothing here
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mLayoutInflater.inflate(LAYOUT, parent, false);
        }
    }

    static class ViewHolder {

        public TextView name;

        public TextView timeAndNetwork;

        public TextView episode;

        public TextView episodeTime;

        public ImageView poster;

        public ImageView favorited;

        public ImageView contextMenu;

    }

    private interface ShowsQuery {

        String[] PROJECTION = {
                BaseColumns._ID, Shows.TITLE, Shows.NEXTTEXT, Shows.AIRSTIME, Shows.NETWORK,
                Shows.POSTER, Shows.AIRSDAYOFWEEK, Shows.STATUS, Shows.NEXTAIRDATETEXT,
                Shows.FAVORITE, Shows.NEXTEPISODE
        };

        int _ID = 0;

        int TITLE = 1;

        int NEXTTEXT = 2;

        int AIRSTIME = 3;

        int NETWORK = 4;

        int POSTER = 5;

        int AIRSDAYOFWEEK = 6;

        int STATUS = 7;

        int NEXTAIRDATETEXT = 8;

        int FAVORITE = 9;

        int NEXTEPISODE = 10;
    }

    private void onFavoriteShow(String showId, boolean isFavorite) {
        ContentValues values = new ContentValues();
        values.put(Shows.FAVORITE, isFavorite);
        getActivity().getContentResolver().update(
                Shows.buildShowUri(showId), values, null, null);

        Utils.runNotificationService(getActivity());

        Toast.makeText(getActivity(),
                getString(isFavorite ? R.string.favorited : R.string.unfavorited),
                Toast.LENGTH_SHORT)
                .show();

        fireTrackerEvent(isFavorite ? "Favorite show" : "Unfavorite show");
    }

    private void showDeleteDialog(long showId) {
        FragmentManager fm = getFragmentManager();
        ConfirmDeleteDialogFragment deleteDialog = ConfirmDeleteDialogFragment.newInstance(String
                .valueOf(showId));
        deleteDialog.show(fm, "fragment_delete");
    }

    public static void showSortDialog(FragmentManager fm, ShowSorting currentSorting) {
        SortDialogFragment sortDialog = SortDialogFragment.newInstance(R.array.shsorting,
                R.array.shsortingData, currentSorting.index(),
                SeriesGuidePreferences.KEY_SHOW_SORT_ORDER, R.string.pref_showsorting);
        sortDialog.show(fm, "fragment_sort");
    }

    /**
     * Fetches the sorting preference and stores it in {@code mSorting}.
     * 
     * @param prefs
     * @return Returns true if the value changed, false otherwise.
     */
    private boolean updateSorting(SharedPreferences prefs) {
        final ShowSorting oldSorting = mSorting;

        mSorting = ShowSorting.fromValue(prefs.getString(
                SeriesGuidePreferences.KEY_SHOW_SORT_ORDER, ShowSorting.FAVORITES_FIRST.value()));

        if (oldSorting != mSorting) {
            return true;
        } else {
            return false;
        }
    }

    private final OnSharedPreferenceChangeListener mPrefsListener = new OnSharedPreferenceChangeListener() {

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            boolean isAffectingChange = false;

            if (key.equals(SeriesGuidePreferences.KEY_SHOW_SORT_ORDER)) {
                updateSorting(sharedPreferences);
                isAffectingChange = true;
            } else if (key.equals(SeriesGuidePreferences.KEY_UPCOMING_LIMIT)) {
                isAffectingChange = true;
            }

            if (isAffectingChange) {
                onFilterChanged(getSherlockActivity().getSupportActionBar()
                        .getSelectedNavigationIndex());
            }
        }
    };

    public void onEvent(FlagTaskCompletedEvent event) {
        if (isAdded()) {
            Utils.updateLatestEpisode(getActivity(), String.valueOf(event.mType.getShowTvdbId()));
        }
    }

    public void onFilterChanged(int itemPosition) {
        // requery with the new filter
        Bundle args = new Bundle();
        args.putInt(ShowsFragment.FILTER_ID, itemPosition);
        getLoaderManager().restartLoader(ShowsFragment.LOADER_ID, args, this);

        // update the empty view
        setEmptyView(itemPosition);
    }

    private static void fireTrackerEvent(String label) {
        EasyTracker.getTracker().sendEvent(TAG, "Context Item", label, (long) 0);
    }

}
