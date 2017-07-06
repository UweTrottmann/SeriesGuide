package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TimeTools;
import java.util.Date;

/**
 * An adapted version of {@link NowAdapter} with a special layout for movies.
 */
public class MoviesNowAdapter extends NowAdapter {

    private static class MovieHistoryViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public TextView timestamp;
        public TextView username;
        public ImageView avatar;
        public ImageView type;

        MovieHistoryViewHolder(View itemView, final ItemClickListener listener) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.textViewNowHistoryTitle);
            timestamp = (TextView) itemView.findViewById(R.id.textViewNowHistoryTimestamp);
            username = (TextView) itemView.findViewById(R.id.textViewNowHistoryUsername);
            avatar = (ImageView) itemView.findViewById(R.id.imageViewNowHistoryAvatar);
            type = (ImageView) itemView.findViewById(R.id.imageViewNowHistoryActionType);

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

    public MoviesNowAdapter(Context context, ItemClickListener listener) {
        super(context, listener);
    }

    @NonNull
    @Override
    protected RecyclerView.ViewHolder getHistoryViewHolder(ViewGroup viewGroup,
            ItemClickListener itemClickListener) {
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_now_history_movie, viewGroup, false);
        return new MovieHistoryViewHolder(v, itemClickListener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof MovieHistoryViewHolder) {
            NowItem item = getItem(position);
            MovieHistoryViewHolder holder = (MovieHistoryViewHolder) viewHolder;

            if (item.type == ItemType.HISTORY) {
                // user history entry
                holder.username.setVisibility(View.GONE);
                holder.avatar.setVisibility(View.GONE);
            } else {
                // friend history entry
                holder.username.setVisibility(View.VISIBLE);
                holder.avatar.setVisibility(View.VISIBLE);

                holder.username.setText(item.username);

                // trakt avatar
                ServiceUtils.loadWithPicasso(getContext(), item.avatar).into(holder.avatar);
            }

            holder.title.setText(item.title);
            holder.timestamp.setText(
                    TimeTools.formatToLocalRelativeTime(getContext(), new Date(item.timestamp)));

            // action type indicator (only if showing trakt history)
            if (TRAKT_ACTION_WATCH.equals(item.action)) {
                holder.type.setImageDrawable(getDrawableWatched());
                holder.type.setVisibility(View.VISIBLE);
            } else if (item.action != null) {
                // check-in, scrobble
                holder.type.setImageDrawable(getDrawableCheckin());
                holder.type.setVisibility(View.VISIBLE);
            } else {
                holder.type.setVisibility(View.GONE);
            }
        } else {
            super.onBindViewHolder(viewHolder, position);
        }
    }
}
