package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
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

    public static final String TRAKT_ACTION_WATCH = "watch";

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

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        public TextView show;
        public TextView episode;
        public TextView timestamp;
        public ImageView poster;
        public TextView username;
        public ImageView avatar;
        public ImageView type;

        public HistoryViewHolder(View itemView, final ItemClickListener listener) {
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
                    int position = getAdapterPosition();
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
                    int position = getAdapterPosition();
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
    @IntDef({ ViewType.RELEASED, ViewType.HISTORY, ViewType.MORE_LINK, ViewType.HEADER })
    public @interface ViewType {
        int RELEASED = 0;
        int HISTORY = 1;
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
        public String tvdbPosterUrl;
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
                String tvdbPosterUrl) {
            this.timestamp = timestamp;
            this.title = title;
            this.description = description;
            this.tvdbPosterUrl = tvdbPosterUrl;
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
        if (viewType == ViewType.HEADER) {
            View v = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_grid_header, viewGroup, false);
            return new HeaderViewHolder(v);
        } else if (viewType == ViewType.MORE_LINK) {
            View v = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_now_more, viewGroup, false);
            return new MoreViewHolder(v, listener);
        } else if (viewType == ViewType.RELEASED) {
            View v = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_now_released, viewGroup, false);
            return new ReleasedViewHolder(v, listener);
        } else if (viewType == ViewType.HISTORY) {
            return getHistoryViewHolder(viewGroup, listener);
        } else {
            throw new IllegalArgumentException("Using unrecognized view type.");
        }
    }

    @NonNull
    protected RecyclerView.ViewHolder getHistoryViewHolder(ViewGroup viewGroup,
            ItemClickListener itemClickListener) {
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_now_history, viewGroup, false);
        return new HistoryViewHolder(v, itemClickListener);
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
            Date actualRelease = TimeTools.applyUserOffset(getContext(), item.timestamp);
            boolean displayExactDate = DisplaySettings.isDisplayExactDate(getContext());
            holder.timestamp.setText(displayExactDate ?
                    TimeTools.formatToLocalDateShort(getContext(), actualRelease)
                    : TimeTools.formatToLocalRelativeTime(getContext(), actualRelease));

            // absolute time and network
            StringBuilder releaseInfo = new StringBuilder();
            // "10:00 PM / Network", as left aligned, exactly mirrored from show list
            releaseInfo.append(TimeTools.formatToLocalTime(getContext(), actualRelease));
            if (!TextUtils.isEmpty(item.network)) {
                releaseInfo.append(" / ").append(item.network);
            }
            holder.info.setText(releaseInfo);

            // is a TVDb or no poster
            TvdbImageTools.loadShowPosterResizeSmallCrop(getContext(), holder.poster, item.tvdbPosterUrl);

            // set unique transition names
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                holder.poster.setTransitionName("nowAdapterPoster_" + position);
            }
        } else if (viewHolder instanceof HistoryViewHolder) {
            HistoryViewHolder holder = (HistoryViewHolder) viewHolder;

            if (item.type == ItemType.HISTORY) {
                // user history entry
                holder.username.setVisibility(View.GONE);
                holder.avatar.setVisibility(View.GONE);

                // a TVDb or no poster
                TvdbImageTools.loadShowPosterResizeSmallCrop(getContext(), holder.poster, item.tvdbPosterUrl);
            } else {
                // friend history entry
                holder.username.setVisibility(View.VISIBLE);
                holder.avatar.setVisibility(View.VISIBLE);

                holder.username.setText(item.username);

                // a TVDb or no poster
                TvdbImageTools.loadShowPosterResizeSmallCrop(getContext(), holder.poster, item.tvdbPosterUrl);
                // trakt avatar
                ServiceUtils.loadWithPicasso(getContext(), item.avatar).into(holder.avatar);
            }

            holder.show.setText(item.title);
            holder.episode.setText(item.description);
            holder.timestamp.setText(
                    TimeTools.formatToLocalRelativeTime(getContext(), new Date(item.timestamp)));

            // action type indicator (only if showing trakt history)
            if (TRAKT_ACTION_WATCH.equals(item.action)) {
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
                return ViewType.HISTORY;
            case ItemType.MORE_LINK:
                return ViewType.MORE_LINK;
            case ItemType.HEADER:
                return ViewType.HEADER;
        }
        return 0;
    }

    protected Context getContext() {
        return context;
    }

    protected int getResIdDrawableWatched() {
        return resIdDrawableWatched;
    }

    protected int getResIdDrawableCheckin() {
        return resIdDrawableCheckin;
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
