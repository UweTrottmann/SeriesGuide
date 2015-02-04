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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.model.HeaderData;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.Utils;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersBaseAdapter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sectioned adapter displaying recently watched episodes, episodes released today and episodes
 * recently watched by trakt friends.
 */
public class NowAdapter extends ArrayAdapter<NowAdapter.NowItem>
        implements StickyGridHeadersBaseAdapter {

    private static final int VIEW_TYPE_DEFAULT = 0;
    private static final int VIEW_TYPE_FRIEND = 1;

    private final int resIdDrawableCheckin;
    private final int resIdDrawableWatched;

    private List<HeaderData> headers;
    private List<NowItem> recentlyWatched;
    private List<NowItem> releasedToday;
    private List<NowItem> friendsRecently;

    public enum NowType {
        RECENTLY_WATCHED,
        RELEASED_TODAY,
        FRIENDS
    }

    public static class NowItem {
        public Integer episodeTvdbId;
        public long timestamp;
        public String title;
        public String description;
        public String poster;
        public String username;
        public String avatar;
        public String action;
        public NowType type;

        public NowItem recentlyWatched(int episodeTvdbId, long timestamp, String show,
                String episode, String poster) {
            setCommonValues(timestamp, show, episode, poster);
            this.episodeTvdbId = episodeTvdbId;
            this.type = NowType.RECENTLY_WATCHED;
            return this;
        }

        public NowItem releasedToday(int episodeTvdbId, long timestamp, String show,
                String episode, String poster) {
            setCommonValues(timestamp, show, episode, poster);
            this.episodeTvdbId = episodeTvdbId;
            this.type = NowType.RELEASED_TODAY;
            return this;
        }

        public NowItem friend(long timestamp, String show, String episode, String poster,
                String username, String avatar, String action) {
            setCommonValues(timestamp, show, episode, poster);
            this.username = username;
            this.avatar = avatar;
            this.action = action;
            this.type = NowType.FRIENDS;
            return this;
        }

        private void setCommonValues(long timestamp, String show, String episode, String poster) {
            this.timestamp = timestamp;
            this.title = show;
            this.description = episode;
            this.poster = poster;
        }
    }

    public NowAdapter(Context context) {
        super(context, 0);

        resIdDrawableCheckin = Utils.resolveAttributeToResourceId(getContext().getTheme(),
                R.attr.drawableCheckin);
        resIdDrawableWatched = Utils.resolveAttributeToResourceId(getContext().getTheme(),
                R.attr.drawableWatch);
    }

    static class HeaderViewHolder {
        public TextView title;

        public HeaderViewHolder(View itemView) {
            title = (TextView) itemView.findViewById(R.id.textViewNowHeader);
        }
    }

    static class FriendViewHolder {
        public TextView show;
        public TextView episode;
        public TextView timestamp;
        public ImageView poster;
        public TextView username;
        public ImageView avatar;
        public ImageView type;

        public FriendViewHolder(View itemView) {
            show = (TextView) itemView.findViewById(R.id.textViewFriendShow);
            episode = (TextView) itemView.findViewById(R.id.textViewFriendEpisode);
            timestamp = (TextView) itemView.findViewById(R.id.textViewFriendTimestamp);
            poster = (ImageView) itemView.findViewById(R.id.imageViewFriendPoster);
            username = (TextView) itemView.findViewById(R.id.textViewFriendUsername);
            avatar = (ImageView) itemView.findViewById(R.id.imageViewFriendAvatar);
            type = (ImageView) itemView.findViewById(R.id.imageViewFriendActionType);
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position).type == NowType.FRIENDS) {
            return VIEW_TYPE_FRIEND;
        }
        return VIEW_TYPE_DEFAULT;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int viewType = getItemViewType(position);
        if (viewType == VIEW_TYPE_FRIEND) {
            FriendViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_friend, parent, false);
                holder = new FriendViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (FriendViewHolder) convertView.getTag();
            }

            NowItem item = getItem(position);
            holder.show.setText(item.title);
            holder.episode.setText(item.description);
            holder.timestamp.setText(
                    TimeTools.formatToLocalRelativeTime(getContext(), new Date(item.timestamp)));
            holder.username.setText(item.username);
            // trakt poster urls
            ServiceUtils.getPicasso(getContext()).load(item.poster).into(holder.poster);
            ServiceUtils.getPicasso(getContext()).load(item.avatar).into(holder.avatar);

            // action type indicator
            if ("watch".equals(item.action)) {
                // marked watched
                holder.type.setImageResource(resIdDrawableWatched);
            } else {
                // check-in, scrobble
                holder.type.setImageResource(resIdDrawableCheckin);
            }
        } else if (viewType == VIEW_TYPE_DEFAULT) {
            SectionedHistoryAdapter.ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_history, parent, false);

                holder = new SectionedHistoryAdapter.ViewHolder(convertView);
                holder.type.setVisibility(View.GONE);

                convertView.setTag(holder);
            } else {
                holder = (SectionedHistoryAdapter.ViewHolder) convertView.getTag();
            }

            NowItem item = getItem(position);
            holder.title.setText(item.title);
            holder.description.setText(item.description);
            holder.timestamp.setText(
                    TimeTools.formatToLocalRelativeTime(getContext(), new Date(item.timestamp)));
            // TheTVDB poster path
            Utils.loadPosterThumbnail(getContext(), holder.poster, item.poster);
        } else {
            throw new IllegalArgumentException("Using unrecognized view type.");
        }

        return convertView;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        // get header position for item position
        position = headers.get(position).getRefPosition();

        NowItem item = getItem(position);
        if (item == null) {
            return null;
        }

        HeaderViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_now_header, parent, false);
            holder = new HeaderViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder) convertView.getTag();
        }

        int titleResId;
        if (item.type == NowType.RECENTLY_WATCHED) {
            titleResId = R.string.recently_watched;
        } else if (item.type == NowType.RELEASED_TODAY) {
            titleResId = R.string.released_today;
        } else {
            titleResId = R.string.friends_recently;
        }
        holder.title.setText(titleResId);

        return convertView;
    }

    @Override
    public int getCountForHeader(int position) {
        if (headers != null) {
            return headers.get(position).getCount();
        }
        return 0;
    }

    @Override
    public int getNumHeaders() {
        if (headers != null) {
            return headers.size();
        }
        return 0;
    }

    @Override
    public void notifyDataSetChanged() {
        // re-create headers before letting notifyDataSetChanged reach the AdapterView
        headers = generateHeaderList();
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        // remove headers before letting notifyDataSetChanged reach the AdapterView
        headers = null;
        super.notifyDataSetInvalidated();
    }

    public synchronized void setRecentlyWatched(List<NowItem> items) {
        recentlyWatched = items;
        reloadData();
    }

    public synchronized void setReleasedTodayData(List<NowItem> items) {
        releasedToday = items;
        reloadData();
    }

    public synchronized void setFriendsRecentlyWatched(List<NowItem> items) {
        friendsRecently = items;
        reloadData();
    }

    private void reloadData() {
        clear();
        if (releasedToday != null) {
            addAll(releasedToday);
        }
        if (recentlyWatched != null) {
            addAll(recentlyWatched);
        }
        if (friendsRecently != null) {
            addAll(friendsRecently);
        }
    }

    private List<HeaderData> generateHeaderList() {
        if (getCount() == 0) {
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

    /**
     * Maps items to their section.
     */
    private long getHeaderId(int position) {
        NowItem item = getItem(position);
        if (item != null) {
            return item.type.ordinal();
        }
        return 0;
    }
}
