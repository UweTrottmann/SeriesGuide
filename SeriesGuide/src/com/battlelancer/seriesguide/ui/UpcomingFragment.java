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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.battlelancer.seriesguide.WatchedBox;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.settings.ActivitySettings;
import com.battlelancer.seriesguide.ui.dialogs.CheckInDialogFragment;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.FlagTask;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.Lists;
import com.battlelancer.seriesguide.util.Maps;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersBaseAdapter;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersGridView;
import com.uwetrottmann.androidutils.CheatSheet;
import com.uwetrottmann.seriesguide.R;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class UpcomingFragment extends SherlockFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener,
        OnSharedPreferenceChangeListener {

    private static final int CONTEXT_FLAG_WATCHED_ID = 0;

    private static final int CONTEXT_FLAG_UNWATCHED_ID = 1;

    private static final int CONTEXT_CHECKIN_ID = 2;

    private SlowAdapter mAdapter;

    private boolean mDualPane;

    private StickyGridHeadersGridView mGridView;

    private TextView mEmptyView;

    /**
     * Data which has to be passed when creating {@link UpcomingFragment}. All
     * Bundle extras are strings, except LOADER_ID and EMPTY_STRING_ID.
     */
    public interface InitBundle {
        String TYPE = "type";

        String ANALYTICS_TAG = "analyticstag";

        String LOADER_ID = "loaderid";

        String EMPTY_STRING_ID = "emptyid";
    }

    public interface ActivityType {
        public String UPCOMING = "upcoming";
        public String RECENT = "recent";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.upcoming_fragment, container, false);

        mEmptyView = (TextView) v.findViewById(R.id.emptyViewUpcoming);
        mEmptyView.setText(getString(getArguments().getInt(InitBundle.EMPTY_STRING_ID)));

        mGridView = (StickyGridHeadersGridView) v.findViewById(R.id.gridViewUpcoming);
        mGridView.setEmptyView(mEmptyView);
        mGridView.setAreHeadersSticky(true);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        View detailsFragment = getActivity().findViewById(R.id.fragment_details);
        mDualPane = detailsFragment != null && detailsFragment.getVisibility() == View.VISIBLE;

        // setup adapter
        mAdapter = new SlowAdapter(getActivity(), null, 0);
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(this);

        // start loading data
        getActivity().getSupportLoaderManager().initLoader(getLoaderId(), null, this);

        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);

        registerForContextMenu(mGridView);
    }

    @Override
    public void onStart() {
        super.onStart();
        final String tag = getArguments().getString("analyticstag");
        EasyTracker.getTracker().sendView(tag);
    }

    @Override
    public void onDestroy() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        super.onDestroy();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        // only display the action appropiate for the items current state
        menu.add(0, CONTEXT_CHECKIN_ID, 0, R.string.checkin);
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        WatchedBox watchedBox = (WatchedBox) info.targetView.findViewById(R.id.watchedBoxUpcoming);
        if (watchedBox.isChecked()) {
            menu.add(0, CONTEXT_FLAG_UNWATCHED_ID, 2, R.string.unmark_episode);
        } else {
            menu.add(0, CONTEXT_FLAG_WATCHED_ID, 1, R.string.mark_episode);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case CONTEXT_FLAG_WATCHED_ID: {
                onFlagEpisodeWatched(info, true);
                return true;
            }
            case CONTEXT_FLAG_UNWATCHED_ID: {
                onFlagEpisodeWatched(info, false);
                return true;
            }
            case CONTEXT_CHECKIN_ID: {
                onCheckinEpisode((int) info.id);
                return true;
            }
        }
        return super.onContextItemSelected(item);
    }

    private void onCheckinEpisode(int episodeTvdbId) {
        CheckInDialogFragment f = CheckInDialogFragment.newInstance(getActivity(), episodeTvdbId);
        f.show(getFragmentManager(), "checkin-dialog");
    }

    private void onFlagEpisodeWatched(AdapterContextMenuInfo info, boolean isWatched) {
        Cursor item = (Cursor) mAdapter.getItem(info.position);

        new FlagTask(getActivity(), item.getInt(UpcomingQuery.REF_SHOW_ID))
                .episodeWatched((int) info.id, item.getInt(UpcomingQuery.SEASON),
                        item.getInt(UpcomingQuery.NUMBER), isWatched)
                .execute();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int episodeId = (int) id;

        if (mDualPane) {
            // Check if fragment is shown, create new if needed.
            EpisodeDetailsFragment detailsFragment = (EpisodeDetailsFragment) getFragmentManager()
                    .findFragmentById(R.id.fragment_details);
            if (detailsFragment == null || detailsFragment.getEpisodeId() != episodeId) {
                // Make new fragment to show this selection.
                detailsFragment = EpisodeDetailsFragment.newInstance(episodeId, true, true);

                // Execute a transaction, replacing any existing
                // fragment with this one inside the frame.
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.setCustomAnimations(R.anim.fragment_slide_left_enter,
                        R.anim.fragment_slide_right_exit);
                ft.replace(R.id.fragment_details, detailsFragment, "fragmentDetails").commit();
            }
        } else {
            Intent intent = new Intent();
            intent.setClass(getActivity(), EpisodesActivity.class);
            intent.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, episodeId);

            startActivity(intent);
            getActivity().overridePendingTransition(R.anim.blow_up_enter, R.anim.blow_up_exit);
        }
    }

    public void onRequery() {
        getLoaderManager().restartLoader(getLoaderId(), null, this);
    }

    private int getLoaderId() {
        return getArguments().getInt("loaderid");
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String type = getArguments().getString(InitBundle.TYPE);
        boolean isInfiniteScrolling = ActivitySettings.isInfiniteScrolling(getActivity());

        // show headers if using a finite list
        mAdapter.setIsShowingHeaders(!isInfiniteScrolling);

        // infinite or 30 days activity stream
        String[][] queryArgs = DBUtils.buildActivityQuery(getActivity(), type,
                isInfiniteScrolling ? -1 : 30);

        return new CursorLoader(getActivity(), Episodes.CONTENT_URI_WITHSHOW,
                UpcomingQuery.PROJECTION, queryArgs[0][0], queryArgs[1], queryArgs[2][0]);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SeriesGuidePreferences.KEY_ONLYFAVORITES)
                || key.equals(SeriesGuidePreferences.KEY_ONLY_SEASON_EPISODES)
                || key.equals(SeriesGuidePreferences.KEY_NOWATCHED)
                || ActivitySettings.KEY_INFINITE_SCROLLING.equals(key)) {
            onRequery();
        }
    }

    public interface UpcomingQuery {
        String[] PROJECTION = new String[] {
                Tables.EPISODES + "." + Episodes._ID, Episodes.TITLE, Episodes.WATCHED,
                Episodes.NUMBER, Episodes.SEASON, Episodes.FIRSTAIREDMS, Shows.TITLE,
                Shows.AIRSTIME, Shows.NETWORK, Shows.POSTER, Shows.REF_SHOW_ID, Shows.IMDBID
        };

        String QUERY_UPCOMING = Episodes.FIRSTAIREDMS + ">=? AND " + Episodes.FIRSTAIREDMS
                + "<? AND " + Shows.HIDDEN + "=0";

        String QUERY_RECENT = Episodes.FIRSTAIREDMS + "<? AND " + Episodes.FIRSTAIREDMS + ">? AND "
                + Shows.HIDDEN + "=0";

        String SORTING_UPCOMING = Episodes.FIRSTAIREDMS + " ASC," + Shows.TITLE + " ASC,"
                + Episodes.NUMBER + " ASC";

        String SORTING_RECENT = Episodes.FIRSTAIREDMS + " DESC," + Shows.TITLE + " ASC,"
                + Episodes.NUMBER + " DESC";

        int _ID = 0;

        int TITLE = 1;

        int WATCHED = 2;

        int NUMBER = 3;

        int SEASON = 4;

        int FIRSTAIREDMS = 5;

        int SHOW_TITLE = 6;

        int SHOW_AIRSTIME = 7;

        int SHOW_NETWORK = 8;

        int SHOW_POSTER = 9;

        int REF_SHOW_ID = 10;

        int IMDBID = 11;

    }

    protected OnItemClickListener mCheckinButtonListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // TODO Auto-generated method stub

        }
    };

    private class SlowAdapter extends CursorAdapter implements StickyGridHeadersBaseAdapter {

        private final int LAYOUT = R.layout.upcoming_row;

        private final int LAYOUT_HEADER = R.layout.upcoming_header;

        private LayoutInflater mLayoutInflater;

        private SharedPreferences mPrefs;

        private List<HeaderData> mHeaders;

        private DataSetObserverExtension mHeaderChangeDataObserver;

        public SlowAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
            mLayoutInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        }

        /**
         * Whether to show episodes grouped by day with header. Disable headers
         * for larger data sets as calculating them is expensive.
         */
        public void setIsShowingHeaders(boolean isShowingHeaders) {
            if (isShowingHeaders) {
                mHeaders = generateHeaderList();
                if (mHeaderChangeDataObserver != null) {
                    mHeaderChangeDataObserver = new DataSetObserverExtension();
                }
                registerDataSetObserver(mHeaderChangeDataObserver);
            } else {
                mHeaders = null;
                if (mHeaderChangeDataObserver != null) {
                    unregisterDataSetObserver(mHeaderChangeDataObserver);
                }
            }
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
                viewHolder.episode = (TextView) convertView
                        .findViewById(R.id.textViewUpcomingEpisode);
                viewHolder.show = (TextView) convertView.findViewById(R.id.textViewUpcomingShow);
                viewHolder.watchedBox = (WatchedBox) convertView
                        .findViewById(R.id.watchedBoxUpcoming);
                viewHolder.meta = (TextView) convertView
                        .findViewById(R.id.textViewUpcomingMeta);
                viewHolder.poster = (ImageView) convertView.findViewById(R.id.poster);
                viewHolder.buttonCheckin = convertView.findViewById(R.id.imageViewUpcomingCheckIn);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            // watched box
            // save rowid to hand over to OnClick event listener
            final int showTvdbId = mCursor.getInt(UpcomingQuery.REF_SHOW_ID);
            final int season = mCursor.getInt(UpcomingQuery.SEASON);
            final int episodeTvdbId = mCursor.getInt(UpcomingQuery._ID);
            final int episode = mCursor.getInt(UpcomingQuery.NUMBER);
            viewHolder.watchedBox.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    WatchedBox checkBox = (WatchedBox) v;
                    checkBox.toggle();

                    new FlagTask(mContext, showTvdbId)
                            .episodeWatched(episodeTvdbId, season, episode, checkBox.isChecked())
                            .execute();
                }
            });
            viewHolder.watchedBox.setChecked(mCursor.getInt(UpcomingQuery.WATCHED) > 0);
            viewHolder.watchedBox.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast infoToast = Toast.makeText(getActivity(), ((WatchedBox) v)
                            .isChecked() ? R.string.unmark_episode : R.string.mark_episode,
                            Toast.LENGTH_SHORT);

                    // position toast near view
                    int[] location = new int[2];
                    v.getLocationOnScreen(location);
                    infoToast.setGravity(Gravity.TOP | Gravity.LEFT,
                            location[0] - v.getWidth() / 2,
                            location[1] - v.getHeight() - v.getHeight() / 2);

                    infoToast.show();
                    return true;
                }
            });

            // checkin button (not avail in all layouts)
            if (viewHolder.buttonCheckin != null) {
                viewHolder.buttonCheckin.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onCheckinEpisode(episodeTvdbId);
                    }
                });
                CheatSheet.setup(viewHolder.buttonCheckin, R.string.checkin);
            }

            // number and show
            final String number = Utils.getEpisodeNumber(mPrefs, season, episode);
            viewHolder.show.setText(number + " | " + mCursor.getString(UpcomingQuery.SHOW_TITLE));

            // title
            viewHolder.episode.setText(mCursor.getString(UpcomingQuery.TITLE));

            // meta data: time, day and network
            StringBuilder metaText = new StringBuilder();
            final long airtime = mCursor.getLong(UpcomingQuery.FIRSTAIREDMS);
            if (airtime != -1) {
                String[] timeAndDay = Utils.formatToTimeAndDay(airtime, mContext);
                // 10:00 | Fri in 3 days, 10:00 PM | Mon 23 Jul
                metaText.append(timeAndDay[0]).append(" | ").append(timeAndDay[1]).append(" ")
                        .append(timeAndDay[2]);
            }
            final String network = mCursor.getString(UpcomingQuery.SHOW_NETWORK);
            if (!TextUtils.isEmpty(network)) {
                metaText.append("\n").append(network);
            }
            viewHolder.meta.setText(metaText);

            // set poster
            final String imagePath = mCursor.getString(UpcomingQuery.SHOW_POSTER);
            ImageProvider.getInstance(mContext).loadPosterThumb(viewHolder.poster, imagePath);

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

        private long getHeaderId(int position) {
            Object obj = getItem(position);
            if (obj != null) {
                /*
                 * Maps all episodes airing the same day to the same id (which
                 * equals the time midnight of their air day).
                 */
                @SuppressWarnings("resource")
                Cursor item = (Cursor) obj;
                long airtime = item.getLong(UpcomingQuery.FIRSTAIREDMS);
                Calendar cal = Utils.getAirtimeCalendar(airtime, mPrefs);
                cal.set(Calendar.HOUR_OF_DAY, 1);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                long airDay = cal.getTimeInMillis();
                return airDay;
            }
            return 0;
        }

        @Override
        public int getCountForHeader(int position) {
            if (mHeaders != null) {
                return mHeaders.get(position).getCount();
            }
            return 0;
        }

        @Override
        public int getNumHeaders() {
            if (mHeaders != null) {
                return mHeaders.size();
            }
            return 0;
        }

        @Override
        public View getHeaderView(int position, View convertView, ViewGroup parent) {
            // get header position for item position
            position = mHeaders.get(position).getRefPosition();

            Object obj = getItem(position);
            if (obj == null) {
                return null;
            }

            HeaderViewHolder holder;
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(LAYOUT_HEADER, null);

                holder = new HeaderViewHolder();
                holder.day = (TextView) convertView.findViewById(R.id.textViewUpcomingHeader);

                convertView.setTag(holder);
            } else {
                holder = (HeaderViewHolder) convertView.getTag();
            }

            @SuppressWarnings("resource")
            Cursor item = (Cursor) obj;
            long airtime = item.getLong(UpcomingQuery.FIRSTAIREDMS);
            Calendar cal = Utils.getAirtimeCalendar(airtime, mPrefs);
            cal.set(Calendar.HOUR_OF_DAY, 1);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long airDay = cal.getTimeInMillis();

            String dayAndTime = Utils.formatToDayAndTimeWithoutOffsets(mContext, airDay);

            holder.day.setText(dayAndTime);

            return convertView;
        }

        protected List<HeaderData> generateHeaderList() {
            Map<Long, HeaderData> mapping = Maps.newHashMap();
            List<HeaderData> headers = Lists.newArrayList();

            for (int i = 0; i < getCount(); i++) {
                long headerId = getHeaderId(i);
                HeaderData headerData = mapping.get(headerId);
                if (headerData == null) {
                    headerData = new HeaderData(i);
                    headers.add(headerData);
                }
                headerData.incrementCount();
                mapping.put(headerId, headerData);
            }

            return headers;
        }

        private final class DataSetObserverExtension extends DataSetObserver {
            @Override
            public void onChanged() {
                mHeaders = generateHeaderList();
            }

            @Override
            public void onInvalidated() {
                mHeaders = generateHeaderList();
            }
        }

        private class HeaderData {
            private int mCount;
            private int mRefPosition;

            public HeaderData(int refPosition) {
                mRefPosition = refPosition;
                mCount = 0;
            }

            public int getCount() {
                return mCount;
            }

            public int getRefPosition() {
                return mRefPosition;
            }

            public void incrementCount() {
                mCount++;
            }
        }
    }

    static class ViewHolder {

        public TextView show;

        public TextView episode;

        public WatchedBox watchedBox;

        public View buttonCheckin;

        public TextView meta;

        public ImageView poster;
    }

    static class HeaderViewHolder {

        public TextView day;

    }
}
