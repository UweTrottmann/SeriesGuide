package com.battlelancer.seriesguide.ui.shows;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Sectioned adapter displaying recently watched episodes and episodes recently watched by trakt
 * friends.
 */
public class NowAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    protected static final String TRAKT_ACTION_WATCH = "watch";

    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    protected static class HistoryViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.textViewHistoryShow) public TextView show;
        @BindView(R.id.textViewHistoryEpisode) public TextView episode;
        @BindView(R.id.imageViewHistoryPoster) public ImageView poster;
        @BindView(R.id.textViewHistoryInfo) public TextView info;
        @BindView(R.id.imageViewHistoryAvatar) public ImageView avatar;
        @BindView(R.id.imageViewHistoryType) public ImageView type;

        public HistoryViewHolder(View itemView, final ItemClickListener listener) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(v, position);
                }
            });
        }
    }

    static class MoreViewHolder extends RecyclerView.ViewHolder {
        public TextView title;

        public MoreViewHolder(View itemView, final ItemClickListener listener) {
            super(itemView);
            title = itemView.findViewById(R.id.textViewNowMoreText);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(v, position);
                }
            });
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public TextView title;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textViewGridHeader);
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ItemType.HISTORY, ItemType.FRIEND, ItemType.MORE_LINK, ItemType.HEADER})
    public @interface ItemType {
        int HISTORY = 1;
        int FRIEND = 2;
        int MORE_LINK = 3;
        int HEADER = 4;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ViewType.HISTORY, ViewType.MORE_LINK, ViewType.HEADER})
    public @interface ViewType {
        int HISTORY = 1;
        int MORE_LINK = 2;
        int HEADER = 3;
    }

    private final Context context;
    private final ItemClickListener listener;
    private final Drawable drawableWatched;
    private final Drawable drawableCheckin;

    private List<NowItem> dataset;
    private List<NowItem> recentlyWatched;
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
        this.drawableWatched = AppCompatResources.getDrawable(getContext(),
                R.drawable.ic_watch_16dp);
        this.drawableCheckin = AppCompatResources.getDrawable(getContext(),
                R.drawable.ic_checkin_16dp);
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
                .inflate(R.layout.item_history, viewGroup, false);
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
        } else if (viewHolder instanceof HistoryViewHolder) {
            HistoryViewHolder holder = (HistoryViewHolder) viewHolder;

            String time = TimeTools.formatToLocalRelativeTime(getContext(),
                    new Date(item.timestamp));
            if (item.type == ItemType.HISTORY) {
                // user history entry
                holder.avatar.setVisibility(View.GONE);
                holder.info.setText(time);
            } else {
                // friend history entry
                holder.avatar.setVisibility(View.VISIBLE);
                holder.info.setText(TextTools.dotSeparate(item.username, time));

                // trakt avatar
                ServiceUtils.loadWithPicasso(getContext(), item.avatar).into(holder.avatar);
            }

            // a TVDb or no poster
            TvdbImageTools.loadShowPosterResizeSmallCrop(getContext(), holder.poster,
                    item.tvdbPosterUrl);

            holder.show.setText(item.title);
            holder.episode.setText(item.description);

            // action type indicator (only if showing trakt history)
            if (TRAKT_ACTION_WATCH.equals(item.action)) {
                holder.type.setImageDrawable(getDrawableWatched());
                holder.type.setEnabled(false);
            } else if (item.action != null) {
                // check-in, scrobble
                holder.type.setImageDrawable(getDrawableCheckin());
                holder.type.setVisibility(View.VISIBLE);
            } else {
                holder.type.setVisibility(View.GONE);
            }
            // Set disabled for darker icon (non-interactive).
            holder.type.setEnabled(false);
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

    protected Drawable getDrawableWatched() {
        return drawableWatched;
    }

    protected Drawable getDrawableCheckin() {
        return drawableCheckin;
    }

    public NowItem getItem(int position) {
        return dataset.get(position);
    }

    public void setRecentlyWatched(List<NowItem> items) {
        int oldCount = recentlyWatched == null ? 0 : recentlyWatched.size();
        int newCount = items == null ? 0 : items.size();

        recentlyWatched = items;
        reloadData();
        notifyAboutChanges(0, oldCount, newCount);
    }

    public void setFriendsRecentlyWatched(List<NowItem> items) {
        int oldCount = friendsRecently == null ? 0 : friendsRecently.size();
        int newCount = items == null ? 0 : items.size();
        // items start after recently watched (if any)
        int startPosition = recentlyWatched == null ? 0 : recentlyWatched.size();

        friendsRecently = items;
        reloadData();
        notifyAboutChanges(startPosition, oldCount, newCount);
    }

    private void reloadData() {
        dataset.clear();
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
