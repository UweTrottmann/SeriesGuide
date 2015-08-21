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
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.Utils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Sectioned adapter displaying recently watched episodes, episodes released today and episodes
 * recently watched by trakt friends.
 */
public class NowAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    static class ReleasedViewHolder extends RecyclerView.ViewHolder {
        TextView show;
        TextView episode;
        TextView timestamp;
        TextView info;
        ImageView poster;

        public ReleasedViewHolder(View itemView, final ItemClickListener listener) {
            super(itemView);
            show = (TextView) itemView.findViewById(R.id.textViewReleasedShow);
            episode = (TextView) itemView.findViewById(R.id.textViewReleasedEpisode);
            timestamp = (TextView) itemView.findViewById(R.id.textViewReleasedTimestamp);
            info = (TextView) itemView.findViewById(R.id.textViewReleasedInfo);
            poster = (ImageView) itemView.findViewById(R.id.imageViewReleasedPoster);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        listener.onItemClick(v, position);
                    }
                }
            });
        }
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        public TextView show;
        public TextView episode;
        public TextView timestamp;
        public ImageView poster;
        public TextView username;
        public ImageView avatar;
        public ImageView type;

        public FriendViewHolder(View itemView, final ItemClickListener listener) {
            super(itemView);
            show = (TextView) itemView.findViewById(R.id.textViewFriendShow);
            episode = (TextView) itemView.findViewById(R.id.textViewFriendEpisode);
            timestamp = (TextView) itemView.findViewById(R.id.textViewFriendTimestamp);
            poster = (ImageView) itemView.findViewById(R.id.imageViewFriendPoster);
            username = (TextView) itemView.findViewById(R.id.textViewFriendUsername);
            avatar = (ImageView) itemView.findViewById(R.id.imageViewFriendAvatar);
            type = (ImageView) itemView.findViewById(R.id.imageViewFriendActionType);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        listener.onItemClick(v, position);
                    }
                }
            });
        }
    }

    static class MoreViewHolder extends RecyclerView.ViewHolder {
        public TextView title;

        public MoreViewHolder(View itemView, final ItemClickListener listener) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.textViewNowMoreText);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        listener.onItemClick(v, position);
                    }
                }
            });
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public TextView title;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.textViewGridHeader);
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ ItemType.RELEASED, ItemType.HISTORY, ItemType.FRIEND, ItemType.MORE_LINK,
            ItemType.HEADER })
    public @interface ItemType {
        int RELEASED = 0;
        int HISTORY = 1;
        int FRIEND = 2;
        int MORE_LINK = 3;
        int HEADER = 4;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ ViewType.RELEASED, ViewType.FRIEND, ViewType.MORE_LINK, ViewType.HEADER })
    public @interface ViewType {
        int RELEASED = 0;
        int FRIEND = 1;
        int MORE_LINK = 2;
        int HEADER = 3;
    }

    private final Context context;
    private final ItemClickListener listener;
    private final int resIdDrawableCheckin;
    private final int resIdDrawableWatched;

    private List<NowItem> dataset;
    private List<NowItem> recentlyWatched;
    private List<NowItem> releasedToday;
    private List<NowItem> friendsRecently;

    public static class NowItem {
        public Integer episodeTvdbId;
        public Integer showTvdbId;
        public Integer movieTmdbId;
        public long timestamp;
        public String title;
        public String description;
        public String network;
        public String poster;
        public String username;
        public String avatar;
        public String action;
        @ItemType public int type;

        public NowItem releasedToday(String network) {
            this.network = network;
            this.type = ItemType.RELEASED;
            return this;
        }

        public NowItem recentlyWatchedLocal() {
            this.type = ItemType.HISTORY;
            return this;
        }

        public NowItem recentlyWatchedTrakt(@Nullable String action) {
            this.action = action;
            this.type = ItemType.HISTORY;
            return this;
        }

        public NowItem friend(String username, String avatar, String action) {
            this.username = username;
            this.avatar = avatar;
            this.action = action;
            this.type = ItemType.FRIEND;
            return this;
        }

        public NowItem tvdbIds(Integer episodeTvdbId, Integer showTvdbId) {
            this.episodeTvdbId = episodeTvdbId;
            this.showTvdbId = showTvdbId;
            return this;
        }

        public NowItem tmdbId(Integer movieTmdbId) {
            this.movieTmdbId = movieTmdbId;
            return this;
        }

        public NowItem displayData(long timestamp, String title, String description,
                String poster) {
            this.timestamp = timestamp;
            this.title = title;
            this.description = description;
            this.poster = poster;
            return this;
        }

        public NowItem moreLink(String title) {
            this.type = ItemType.MORE_LINK;
            this.title = title;
            return this;
        }

        public NowItem header(String title) {
            this.type = ItemType.HEADER;
            this.title = title;
            return this;
        }
    }

    public NowAdapter(Context context, ItemClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.dataset = new ArrayList<>();
        this.resIdDrawableCheckin = Utils.resolveAttributeToResourceId(context.getTheme(),
                R.attr.drawableCheckin);
        this.resIdDrawableWatched = Utils.resolveAttributeToResourceId(context.getTheme(),
                R.attr.drawableWatch);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType == ViewType.RELEASED) {
            View v = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_now_released, viewGroup, false);
            return new ReleasedViewHolder(v, listener);
        } else if (viewType == ViewType.FRIEND) {
            View v = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_now_friend, viewGroup, false);
            return new FriendViewHolder(v, listener);
        } else if (viewType == ViewType.MORE_LINK) {
            View v = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_now_more, viewGroup, false);
            return new MoreViewHolder(v, listener);
        } else if (viewType == ViewType.HEADER) {
            View v = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_grid_header, viewGroup, false);
            return new HeaderViewHolder(v);
        } else {
            throw new IllegalArgumentException("Using unrecognized view type.");
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        NowItem item = getItem(position);

        if (viewHolder instanceof HeaderViewHolder) {
            HeaderViewHolder holder = (HeaderViewHolder) viewHolder;

            holder.title.setText(item.title);
        } else if (viewHolder instanceof MoreViewHolder) {
            MoreViewHolder holder = (MoreViewHolder) viewHolder;

            holder.title.setText(item.title);
        } else if (viewHolder instanceof ReleasedViewHolder) {
            ReleasedViewHolder holder = (ReleasedViewHolder) viewHolder;

            holder.show.setText(item.title);
            holder.episode.setText(item.description);

            // timestamp
            Date actualRelease = TimeTools.applyUserOffset(context, item.timestamp);
            holder.timestamp.setText(TimeTools.formatToLocalRelativeTime(context, actualRelease));

            // absolute time and network
            StringBuilder releaseInfo = new StringBuilder();
            // "10:00 PM / Network", as left aligned, exactly mirrored from show list
            releaseInfo.append(TimeTools.formatToLocalTime(context, actualRelease));
            if (!TextUtils.isEmpty(item.network)) {
                releaseInfo.append(" / ").append(item.network);
            }
            holder.info.setText(releaseInfo);

            // is a TVDb or no poster
            Utils.loadSmallTvdbShowPoster(getContext(), holder.poster, item.poster);
        } else if (viewHolder instanceof FriendViewHolder) {
            FriendViewHolder holder = (FriendViewHolder) viewHolder;

            if (item.type == ItemType.HISTORY) {
                // user history entry
                holder.username.setVisibility(View.GONE);
                holder.avatar.setVisibility(View.GONE);

                if (item.poster != null && item.poster.startsWith("http")) {
                    // is a trakt poster
                    Utils.loadSmallPoster(getContext(), holder.poster, item.poster);
                } else {
                    // is a TVDb (only path then, so build URL) or no poster
                    Utils.loadSmallTvdbShowPoster(getContext(), holder.poster, item.poster);
                }
            } else {
                // friend history entry
                holder.username.setVisibility(View.VISIBLE);
                holder.avatar.setVisibility(View.VISIBLE);

                holder.username.setText(item.username);

                // trakt poster and avatar
                Utils.loadSmallPoster(getContext(), holder.poster, item.poster);
                ServiceUtils.loadWithPicasso(getContext(), item.avatar).into(holder.avatar);
            }

            holder.show.setText(item.title);
            holder.episode.setText(item.description);
            holder.timestamp.setText(
                    TimeTools.formatToLocalRelativeTime(getContext(), new Date(item.timestamp)));

            // action type indicator (only if showing trakt history)
            if ("watch".equals(item.action)) {
                holder.type.setImageResource(resIdDrawableWatched);
                holder.type.setVisibility(View.VISIBLE);
            } else if (item.action != null) {
                // check-in, scrobble
                holder.type.setImageResource(resIdDrawableCheckin);
                holder.type.setVisibility(View.VISIBLE);
            } else {
                holder.type.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

    @Override
    public int getItemViewType(int position) {
        int itemType = getItem(position).type;
        switch (itemType) {
            case ItemType.RELEASED:
                return ViewType.RELEASED;
            case ItemType.HISTORY:
            case ItemType.FRIEND:
                return ViewType.FRIEND;
            case ItemType.MORE_LINK:
                return ViewType.MORE_LINK;
            case ItemType.HEADER:
                return ViewType.HEADER;
        }
        return 0;
    }

    private Context getContext() {
        return context;
    }

    public NowItem getItem(int position) {
        return dataset.get(position);
    }

    public void setReleasedTodayData(List<NowItem> items) {
        int oldCount = releasedToday == null ? 0 : releasedToday.size();
        int newCount = items == null ? 0 : items.size();

        releasedToday = items;
        reloadData();
        notifyAboutChanges(0, oldCount, newCount);
    }

    public void setRecentlyWatched(List<NowItem> items) {
        int oldCount = recentlyWatched == null ? 0 : recentlyWatched.size();
        int newCount = items == null ? 0 : items.size();
        // items start after released today (if any)
        int startPosition = releasedToday == null ? 0 : releasedToday.size();

        recentlyWatched = items;
        reloadData();
        notifyAboutChanges(startPosition, oldCount, newCount);
    }

    public void setFriendsRecentlyWatched(List<NowItem> items) {
        int oldCount = friendsRecently == null ? 0 : friendsRecently.size();
        int newCount = items == null ? 0 : items.size();
        // items start after released today and recently watched (if any)
        int startPosition = (releasedToday == null ? 0 : releasedToday.size())
                + (recentlyWatched == null ? 0 : recentlyWatched.size());

        friendsRecently = items;
        reloadData();
        notifyAboutChanges(startPosition, oldCount, newCount);
    }

    private void reloadData() {
        dataset.clear();
        if (releasedToday != null) {
            dataset.addAll(releasedToday);
        }
        if (recentlyWatched != null) {
            dataset.addAll(recentlyWatched);
        }
        if (friendsRecently != null) {
            dataset.addAll(friendsRecently);
        }
    }

    private void notifyAboutChanges(int startPosition, int oldItemCount, int newItemCount) {
        if (newItemCount == 0 && oldItemCount == 0) {
            return;
        }

        if (newItemCount == oldItemCount) {
            // identical number of items
            notifyItemRangeChanged(startPosition, oldItemCount);
        } else if (newItemCount > oldItemCount) {
            // more items than before
            if (oldItemCount > 0) {
                notifyItemRangeChanged(startPosition, oldItemCount);
            }
            notifyItemRangeInserted(startPosition + oldItemCount,
                    newItemCount - oldItemCount);
        } else {
            // less items than before
            if (newItemCount > 0) {
                notifyItemRangeChanged(startPosition, newItemCount);
            }
            notifyItemRangeRemoved(startPosition + newItemCount,
                    oldItemCount - newItemCount);
        }
    }
}
