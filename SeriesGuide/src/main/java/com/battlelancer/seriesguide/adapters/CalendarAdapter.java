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

package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.model.HeaderData;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;
import com.battlelancer.seriesguide.ui.CalendarFragment;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.widgets.WatchedBox;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersBaseAdapter;
import com.uwetrottmann.androidutils.CheatSheet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for {@link CalendarFragment} with optimizations for image loading for smoother
 * scrolling.
 */
public class CalendarAdapter extends CursorAdapter implements StickyGridHeadersBaseAdapter {

    private LayoutInflater mLayoutInflater;

    private List<HeaderData> mHeaders;
    private boolean mIsShowingHeaders;

    private Calendar mCalendar;

    public CalendarAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mLayoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mCalendar = Calendar.getInstance();
    }

    /**
     * Whether to show episodes grouped by day with header. Disable headers for larger data sets as
     * calculating them is expensive. Make sure to reload the data afterwards.
     */
    public void setIsShowingHeaders(boolean isShowingHeaders) {
        mIsShowingHeaders = isShowingHeaders;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        // watched box
        // save rowid to hand over to OnClick event listener
        final int showTvdbId = cursor.getInt(Query.SHOW_ID);
        final int season = cursor.getInt(Query.SEASON);
        final int episodeTvdbId = cursor.getInt(Query._ID);
        final int episode = cursor.getInt(Query.NUMBER);
        viewHolder.watchedBox.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                WatchedBox box = (WatchedBox) v;
                // disable button, will be re-enabled on data reload once action completes
                box.setEnabled(false);
                EpisodeTools.episodeWatched(context, showTvdbId, episodeTvdbId, season, episode,
                        EpisodeTools.isWatched(box.getEpisodeFlag()) ? EpisodeFlags.UNWATCHED
                                : EpisodeFlags.WATCHED
                );
            }
        });
        viewHolder.watchedBox.setEpisodeFlag(cursor.getInt(Query.WATCHED));
        viewHolder.watchedBox.setEnabled(true);
        CheatSheet.setup(viewHolder.watchedBox,
                EpisodeTools.isWatched(viewHolder.watchedBox.getEpisodeFlag())
                        ? R.string.action_unwatched : R.string.action_watched
        );

        // show title
        viewHolder.show.setText(cursor.getString(Query.SHOW_TITLE));

        // episode number and title
        viewHolder.episode.setText(Utils.getNextEpisodeString(context, season, episode,
                cursor.getString(Query.TITLE)));

        // timestamp, absolute time and network
        StringBuilder releaseInfo = new StringBuilder();
        long releaseTime = cursor.getLong(Query.RELEASE_TIME_MS);
        if (releaseTime != -1) {
            Date actualRelease = TimeTools.applyUserOffset(context, releaseTime);
            // timestamp
            viewHolder.timestamp.setText(
                    TimeTools.formatToLocalRelativeTime(context, actualRelease));

            // "10:00 PM / Network", as left aligned, exactly mirrored from show list
            releaseInfo.append(TimeTools.formatToLocalTime(context, actualRelease));
        } else {
            viewHolder.timestamp.setText(null);
        }
        final String network = cursor.getString(Query.SHOW_NETWORK);
        if (!TextUtils.isEmpty(network)) {
            releaseInfo.append(" / ").append(network);
        }
        viewHolder.info.setText(releaseInfo);

        // collected indicator
        boolean isCollected = EpisodeTools.isCollected(
                cursor.getInt(Query.COLLECTED));
        viewHolder.collected.setVisibility(isCollected ? View.VISIBLE : View.GONE);

        // set poster
        Utils.loadSmallTvdbShowPoster(context, viewHolder.poster,
                cursor.getString(Query.SHOW_POSTER));
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = mLayoutInflater.inflate(R.layout.grid_activity_row, parent, false);

        ViewHolder viewHolder = new ViewHolder(v);
        v.setTag(viewHolder);

        return v;
    }

    private long getHeaderId(int position) {
        Object obj = getItem(position);
        if (obj != null) {
            /*
             * Map all episodes releasing the same day to the same id (which
             * equals the time midnight of their release day).
             */
            @SuppressWarnings("resource")
            Cursor item = (Cursor) obj;
            return getHeaderTime(item);
        }
        return 0;
    }

    private long getHeaderTime(Cursor item) {
        long releaseTime = item.getLong(Query.RELEASE_TIME_MS);
        Date actualRelease = TimeTools.applyUserOffset(mContext, releaseTime);

        mCalendar.setTime(actualRelease);
        // not midnight because upcoming->recent is delayed 1 hour
        // so header would display wrong relative time close to midnight
        mCalendar.set(Calendar.HOUR_OF_DAY, 1);
        mCalendar.set(Calendar.MINUTE, 0);
        mCalendar.set(Calendar.SECOND, 0);
        mCalendar.set(Calendar.MILLISECOND, 0);

        return mCalendar.getTimeInMillis();
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
            convertView = mLayoutInflater.inflate(R.layout.item_grid_header, parent, false);

            holder = new HeaderViewHolder();
            holder.day = (TextView) convertView.findViewById(R.id.textViewGridHeader);

            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }

        @SuppressWarnings("resource")
        Cursor item = (Cursor) obj;
        long headerTime = getHeaderTime(item);
        // display headers like "Mon in 3 days", also "today" when applicable
        holder.day.setText(
                TimeTools.formatToLocalDayAndRelativeTime(mContext, new Date(headerTime)));

        return convertView;
    }

    @Override
    public void notifyDataSetChanged() {
        // re-create headers before letting notifyDataSetChanged reach the AdapterView
        mHeaders = generateHeaderList();
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        // remove headers before letting notifyDataSetChanged reach the AdapterView
        mHeaders = null;
        super.notifyDataSetInvalidated();
    }

    protected List<HeaderData> generateHeaderList() {
        if (getCount() == 0 || !mIsShowingHeaders) {
            return null;
        }

        Map<Long, HeaderData> mapping = new HashMap<>();
        List<HeaderData> headers = new ArrayList<>();

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

    public interface Query {

        String[] PROJECTION = new String[] {
                SeriesGuideDatabase.Tables.EPISODES + "." + SeriesGuideContract.Episodes._ID,
                SeriesGuideContract.Episodes.TITLE,
                SeriesGuideContract.Episodes.NUMBER,
                SeriesGuideContract.Episodes.SEASON,
                SeriesGuideContract.Episodes.FIRSTAIREDMS,
                SeriesGuideContract.Episodes.WATCHED,
                SeriesGuideContract.Episodes.COLLECTED,
                SeriesGuideContract.Shows.REF_SHOW_ID,
                SeriesGuideContract.Shows.TITLE,
                SeriesGuideContract.Shows.NETWORK,
                SeriesGuideContract.Shows.POSTER
        };

        String QUERY_UPCOMING = SeriesGuideContract.Episodes.FIRSTAIREDMS + ">=? AND "
                + SeriesGuideContract.Episodes.FIRSTAIREDMS
                + "<? AND " + SeriesGuideContract.Shows.SELECTION_NO_HIDDEN;

        String QUERY_RECENT = SeriesGuideContract.Episodes.FIRSTAIREDMS + "<? AND "
                + SeriesGuideContract.Episodes.FIRSTAIREDMS + ">? AND "
                + SeriesGuideContract.Shows.SELECTION_NO_HIDDEN;

        String SORTING_UPCOMING = SeriesGuideContract.Episodes.FIRSTAIREDMS + " ASC,"
                + SeriesGuideContract.Shows.TITLE + " ASC,"
                + SeriesGuideContract.Episodes.NUMBER + " ASC";

        String SORTING_RECENT = SeriesGuideContract.Episodes.FIRSTAIREDMS + " DESC,"
                + SeriesGuideContract.Shows.TITLE + " ASC,"
                + SeriesGuideContract.Episodes.NUMBER + " DESC";

        int _ID = 0;
        int TITLE = 1;
        int NUMBER = 2;
        int SEASON = 3;
        int RELEASE_TIME_MS = 4;
        int WATCHED = 5;
        int COLLECTED = 6;
        int SHOW_ID = 7;
        int SHOW_TITLE = 8;
        int SHOW_NETWORK = 9;
        int SHOW_POSTER = 10;
    }

    static class ViewHolder {

        public TextView show;
        public TextView episode;
        public View collected;
        public WatchedBox watchedBox;
        public TextView info;
        public TextView timestamp;
        public ImageView poster;

        public ViewHolder(View v) {
            show = (TextView) v.findViewById(R.id.textViewActivityShow);
            episode = (TextView) v.findViewById(R.id.textViewActivityEpisode);
            collected = v.findViewById(R.id.imageViewActivityCollected);
            watchedBox = (WatchedBox) v.findViewById(R.id.watchedBoxActivity);
            info = (TextView) v.findViewById(R.id.textViewActivityInfo);
            timestamp = (TextView) v.findViewById(R.id.textViewActivityTimestamp);
            poster = (ImageView) v.findViewById(R.id.imageViewActivityPoster);
        }
    }

    static class HeaderViewHolder {

        public TextView day;
    }
}
