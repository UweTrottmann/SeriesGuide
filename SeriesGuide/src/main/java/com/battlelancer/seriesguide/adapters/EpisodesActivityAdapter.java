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
import android.database.DataSetObserver;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.model.HeaderData;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.ui.ActivityFragment;
import com.battlelancer.seriesguide.ui.streams.FriendsEpisodeStreamFragment;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.Utils;
import com.jakewharton.trakt.entities.ActivityItem;
import com.jakewharton.trakt.enumerations.ActivityAction;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersBaseAdapter;
import com.uwetrottmann.androidutils.Lists;
import com.uwetrottmann.androidutils.Maps;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Creates a list of episodes from a list of {@link com.jakewharton.trakt.entities.ActivityItem},
 * displaying user name and avatar.
 */
public class EpisodesActivityAdapter extends ArrayAdapter<ActivityItem> implements
        StickyGridHeadersBaseAdapter {

    private final LayoutInflater mInflater;
    private final DataSetObserverExtension mHeaderChangeDataObserver;

    private List<HeaderData> mHeaders;
    private Calendar mCalendar;

    public EpisodesActivityAdapter(Context context) {
        super(context, 0);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mCalendar = Calendar.getInstance();
        mHeaderChangeDataObserver = new DataSetObserverExtension();
        registerDataSetObserver(mHeaderChangeDataObserver);
    }

    public void setData(List<ActivityItem> data) {
        clear();
        if (data != null) {
            addAll(data);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // A ViewHolder keeps references to child views to avoid
        // unnecessary calls to findViewById() on each row.
        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.friend, parent, false);

            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.textViewFriendUsername);
            holder.show = (TextView) convertView.findViewById(R.id.textViewFriendShow);
            holder.episode = (TextView) convertView.findViewById(R.id.textViewFriendEpisode);
            holder.timestamp = (TextView) convertView.findViewById(
                    R.id.textViewFriendTimestamp);
            holder.poster = (ImageView) convertView.findViewById(R.id.imageViewFriendPoster);
            holder.avatar = (ImageView) convertView.findViewById(R.id.imageViewFriendAvatar);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Bind the data efficiently with the holder.
        ActivityItem activity = getItem(position);

        // show poster
        if (activity.show.images != null && !TextUtils.isEmpty(activity.show.images.poster)) {
            String posterPath = activity.show.images.poster.replace(
                    TraktSettings.POSTER_SIZE_SPEC_DEFAULT, TraktSettings.POSTER_SIZE_SPEC_138);
            ServiceUtils.getPicasso(getContext()).load(posterPath).into(holder.poster);
        }

        holder.name.setText(activity.user.username);
        ServiceUtils.getPicasso(getContext()).load(activity.user.avatar).into(holder.avatar);

        holder.timestamp.setTextAppearance(getContext(), R.style.TextAppearance_Small_Dim);

        CharSequence timestamp;
        // friend is watching something right now?
        if (activity.action == ActivityAction.Watching) {
            timestamp = getContext().getString(R.string.now);
            holder.timestamp.setTextAppearance(getContext(),
                    R.style.TextAppearance_Small_Highlight_Red);
        } else {
            timestamp = DateUtils.getRelativeTimeSpanString(
                    activity.timestamp.getTime(), System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL);
        }

        holder.show.setText(activity.show.title);
        holder.episode.setText(Utils.getNextEpisodeString(getContext(), activity.episode.season,
                activity.episode.number, activity.episode.title));
        holder.timestamp.setText(timestamp);

        return convertView;
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

        ActivityItem item = getItem(position);
        if (item == null) {
            return null;
        }

        HeaderViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.grid_activity_header, parent, false);

            holder = new HeaderViewHolder();
            holder.day = (TextView) convertView.findViewById(R.id.textViewUpcomingHeader);

            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }

        long headerTime = getHeaderTime(item);
        // display headers like "Mon in 3 days", also "today" when applicable
        holder.day.setText(
                TimeTools.formatToDayAndRelativeTime(getContext(), new Date(headerTime)));

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

    /**
     * Maps all activities of the same day in the device time zone to the same id (which
     * equals the time in ms close to midnight of that day).
     */
    private long getHeaderId(int position) {
        ActivityItem item = getItem(position);
        if (item != null) {
            return getHeaderTime(item);
        }
        return 0;
    }

    /**
     * Extracts the activity timestamp and "rounds" it down to shortly after midnight in the
     * current device time zone.
     */
    private long getHeaderTime(ActivityItem item) {
        mCalendar.setTime(item.timestamp);
        //
        mCalendar.set(Calendar.HOUR_OF_DAY, 0);
        mCalendar.set(Calendar.MINUTE, 0);
        mCalendar.set(Calendar.SECOND, 0);
        mCalendar.set(Calendar.MILLISECOND, 1);

        return mCalendar.getTimeInMillis();
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

    static class ViewHolder {

        TextView name;

        TextView show;

        TextView episode;

        TextView timestamp;

        ImageView poster;

        ImageView avatar;
    }

    static class HeaderViewHolder {

        public TextView day;
    }
}
