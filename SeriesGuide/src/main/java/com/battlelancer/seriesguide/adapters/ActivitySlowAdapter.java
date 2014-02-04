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

import com.battlelancer.seriesguide.WatchedBox;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.ui.ActivityFragment;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.FlagTask;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.Utils;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersBaseAdapter;
import com.uwetrottmann.androidutils.CheatSheet;
import com.uwetrottmann.androidutils.Lists;
import com.uwetrottmann.androidutils.Maps;
import com.uwetrottmann.seriesguide.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.preference.PreferenceManager;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Adapter for {@link com.battlelancer.seriesguide.ui.ActivityFragment} with optimizations for image
 * loading for smoother scrolling.
 */
public class ActivitySlowAdapter extends CursorAdapter implements StickyGridHeadersBaseAdapter {

    private final int LAYOUT = R.layout.grid_activity_row;

    private final int LAYOUT_HEADER = R.layout.grid_activity_header;

    private final CheckInListener mCheckInListener;

    private LayoutInflater mLayoutInflater;

    private SharedPreferences mPrefs;

    private List<HeaderData> mHeaders;

    private DataSetObserverExtension mHeaderChangeDataObserver;

    public interface CheckInListener {

        public void onCheckinEpisode(int episodeTvdbId);

    }

    public ActivitySlowAdapter(Context context, Cursor c, int flags,
            CheckInListener checkInListener) {
        super(context, c, flags);
        mLayoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mCheckInListener = checkInListener;
    }

    /**
     * Whether to show episodes grouped by day with header. Disable headers for larger data sets as
     * calculating them is expensive.
     */
    public void setIsShowingHeaders(boolean isShowingHeaders) {
        if (isShowingHeaders) {
            if (mHeaderChangeDataObserver == null) {
                mHeaderChangeDataObserver = new DataSetObserverExtension();
                registerDataSetObserver(mHeaderChangeDataObserver);
            }
        } else {
            if (mHeaderChangeDataObserver != null) {
                unregisterDataSetObserver(mHeaderChangeDataObserver);
            }
            mHeaders = null;
            mHeaderChangeDataObserver = null;
        }
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        // watched box
        // save rowid to hand over to OnClick event listener
        final int showTvdbId = cursor.getInt(ActivityFragment.ActivityQuery.REF_SHOW_ID);
        final int season = cursor.getInt(ActivityFragment.ActivityQuery.SEASON);
        final int episodeTvdbId = cursor.getInt(ActivityFragment.ActivityQuery._ID);
        final int episode = cursor.getInt(ActivityFragment.ActivityQuery.NUMBER);
        viewHolder.watchedBox.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                WatchedBox box = (WatchedBox) v;
                new FlagTask(context, showTvdbId)
                        .episodeWatched(episodeTvdbId, season, episode,
                                EpisodeTools.isWatched(box.getEpisodeFlag())
                                        ? EpisodeFlags.UNWATCHED : EpisodeFlags.WATCHED)
                        .execute();
            }
        });
        viewHolder.watchedBox.setEpisodeFlag(cursor.getInt(ActivityFragment.ActivityQuery.WATCHED));
        CheatSheet.setup(viewHolder.watchedBox,
                EpisodeTools.isWatched(viewHolder.watchedBox.getEpisodeFlag())
                        ? R.string.unmark_episode : R.string.mark_episode);

        // number and show
        final String number = Utils.getEpisodeNumber(context, season, episode);
        viewHolder.show.setText(number + " | " + cursor.getString(ActivityFragment.ActivityQuery.SHOW_TITLE));

        // title
        viewHolder.episode.setText(cursor.getString(ActivityFragment.ActivityQuery.TITLE));

        // meta data: time, day and network
        StringBuilder metaText = new StringBuilder();
        long releaseTime = cursor.getLong(
                ActivityFragment.ActivityQuery.EPISODE_FIRST_RELEASE_MS);
        if (releaseTime != -1) {
            Date actualRelease = TimeTools.getEpisodeReleaseTime(context, releaseTime);
            // 10:00 | Fri in 3 days, 10:00 PM | Mon 23 Jul
            metaText.append(TimeTools.formatToLocalReleaseTime(context, actualRelease))
                    .append(" | ")
                    .append(TimeTools.formatToLocalReleaseDay(actualRelease))
                    .append(" ")
                    .append(TimeTools.formatToRelativeLocalReleaseTime(actualRelease));
        }
        final String network = cursor.getString(ActivityFragment.ActivityQuery.SHOW_NETWORK);
        if (!TextUtils.isEmpty(network)) {
            metaText.append("\n").append(network);
        }
        viewHolder.meta.setText(metaText);

        // set poster
        final String imagePath = cursor.getString(ActivityFragment.ActivityQuery.SHOW_POSTER);
        ImageProvider.getInstance(context).loadPosterThumb(viewHolder.poster, imagePath);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = mLayoutInflater.inflate(LAYOUT, parent, false);

        ViewHolder viewHolder = new ViewHolder();
        viewHolder.episode = (TextView) v.findViewById(R.id.textViewUpcomingEpisode);
        viewHolder.show = (TextView) v.findViewById(R.id.textViewUpcomingShow);
        viewHolder.watchedBox = (WatchedBox) v.findViewById(R.id.watchedBoxUpcoming);
        viewHolder.meta = (TextView) v.findViewById(R.id.textViewUpcomingMeta);
        viewHolder.poster = (ImageView) v.findViewById(R.id.poster);

        v.setTag(viewHolder);

        return v;
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
            long airtime = item.getLong(ActivityFragment.ActivityQuery.EPISODE_FIRST_RELEASE_MS);
            Calendar cal = Utils.getAirtimeCalendar(airtime, mPrefs);
            cal.set(Calendar.HOUR_OF_DAY, 1);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
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
        long airtime = item.getLong(ActivityFragment.ActivityQuery.EPISODE_FIRST_RELEASE_MS);
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

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        mHeaders = null;
        return super.swapCursor(newCursor);
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

    static class ViewHolder {

        public TextView show;

        public TextView episode;

        public WatchedBox watchedBox;

        public TextView meta;

        public ImageView poster;
    }

    static class HeaderViewHolder {

        public TextView day;

    }
}
