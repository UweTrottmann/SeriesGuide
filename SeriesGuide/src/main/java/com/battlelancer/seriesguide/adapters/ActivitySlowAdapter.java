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
import com.battlelancer.seriesguide.ui.ActivityFragment;
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
 * Adapter for {@link com.battlelancer.seriesguide.ui.ActivityFragment} with optimizations for image
 * loading for smoother scrolling.
 */
public class ActivitySlowAdapter extends CursorAdapter implements StickyGridHeadersBaseAdapter {

    private final int LAYOUT = R.layout.grid_activity_row;

    private final int LAYOUT_HEADER = R.layout.grid_activity_header;

    private LayoutInflater mLayoutInflater;

    private List<HeaderData> mHeaders;
    private boolean mIsShowingHeaders;

    private Calendar mCalendar;

    public ActivitySlowAdapter(Context context, Cursor c, int flags) {
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
        final int showTvdbId = cursor.getInt(ActivityFragment.ActivityQuery.SHOW_ID);
        final int season = cursor.getInt(ActivityFragment.ActivityQuery.SEASON);
        final int episodeTvdbId = cursor.getInt(ActivityFragment.ActivityQuery._ID);
        final int episode = cursor.getInt(ActivityFragment.ActivityQuery.NUMBER);
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
        viewHolder.watchedBox.setEpisodeFlag(cursor.getInt(ActivityFragment.ActivityQuery.WATCHED));
        viewHolder.watchedBox.setEnabled(true);
        CheatSheet.setup(viewHolder.watchedBox,
                EpisodeTools.isWatched(viewHolder.watchedBox.getEpisodeFlag())
                        ? R.string.action_unwatched : R.string.action_watched
        );

        // number and show
        final String number = Utils.getEpisodeNumber(context, season, episode);
        viewHolder.show.setText(
                number + " | " + cursor.getString(ActivityFragment.ActivityQuery.SHOW_TITLE));

        // title
        viewHolder.episode.setText(cursor.getString(ActivityFragment.ActivityQuery.TITLE));

        // meta data: time, day and network
        StringBuilder metaText = new StringBuilder();
        long releaseTime = cursor.getLong(
                ActivityFragment.ActivityQuery.RELEASE_TIME_MS);
        if (releaseTime != -1) {
            Date actualRelease = TimeTools.applyUserOffset(context, releaseTime);
            // 10:00 | in 3 days, 10:00 PM | 23 Jul
            metaText.append(TimeTools.formatToLocalTime(context, actualRelease));
            metaText.append(" | ")
                    .append(TimeTools.formatToLocalRelativeTime(context, actualRelease));
        }
        final String network = cursor.getString(ActivityFragment.ActivityQuery.SHOW_NETWORK);
        if (!TextUtils.isEmpty(network)) {
            metaText.append("\n").append(network);
        }
        viewHolder.meta.setText(metaText);

        // collected indicator
        boolean isCollected = EpisodeTools.isCollected(
                cursor.getInt(ActivityFragment.ActivityQuery.COLLECTED));
        viewHolder.collected.setVisibility(isCollected ? View.VISIBLE : View.GONE);

        // set poster
        Utils.loadPosterThumbnail(context, viewHolder.poster,
                cursor.getString(ActivityFragment.ActivityQuery.SHOW_POSTER));
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = mLayoutInflater.inflate(LAYOUT, parent, false);

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
        long releaseTime = item.getLong(ActivityFragment.ActivityQuery.RELEASE_TIME_MS);
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
            convertView = mLayoutInflater.inflate(LAYOUT_HEADER, parent, false);

            holder = new HeaderViewHolder();
            holder.day = (TextView) convertView.findViewById(R.id.textViewUpcomingHeader);

            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }

        @SuppressWarnings("resource")
        Cursor item = (Cursor) obj;
        long headerTime = getHeaderTime(item);
        // display headers like "Mon in 3 days", also "today" when applicable
        holder.day.setText(TimeTools.formatToLocalDayAndRelativeTime(mContext, new Date(headerTime)));

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

    static class ViewHolder {

        public TextView show;

        public TextView episode;

        public View collected;

        public WatchedBox watchedBox;

        public TextView meta;

        public ImageView poster;

        public ViewHolder(View v) {
            show = (TextView) v.findViewById(R.id.textViewActivityShow);
            episode = (TextView) v.findViewById(R.id.textViewActivityEpisode);
            collected = v.findViewById(R.id.imageViewActivityCollected);
            watchedBox = (WatchedBox) v.findViewById(R.id.watchedBoxActivity);
            meta = (TextView) v.findViewById(R.id.textViewActivityMeta);
            poster = (ImageView) v.findViewById(R.id.imageViewActivityPoster);
        }
    }

    static class HeaderViewHolder {

        public TextView day;
    }
}
