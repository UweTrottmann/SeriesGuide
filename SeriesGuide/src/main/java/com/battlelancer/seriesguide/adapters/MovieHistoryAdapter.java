package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
import com.uwetrottmann.trakt5.entities.HistoryEntry;

/**
 * Creates a list of movies from a list of {@link HistoryEntry}.
 */
public class MovieHistoryAdapter extends SectionedHistoryAdapter {

    public interface OnItemClickListener {
        void onItemClick(View view, HistoryEntry item);
    }

    public static class ViewHolder {
        @BindView(R.id.textViewHistoryShow) TextView show;
        @BindView(R.id.textViewHistoryEpisode) TextView episode;
        @BindView(R.id.imageViewHistoryPoster) ImageView poster;
        @BindView(R.id.textViewHistoryInfo) TextView info;
        @BindView(R.id.imageViewHistoryAvatar) ImageView avatar;
        @BindView(R.id.imageViewHistoryType) ImageView type;
        HistoryEntry item;

        public ViewHolder(View itemView, final OnItemClickListener listener) {
            ButterKnife.bind(this, itemView);
            avatar.setVisibility(View.GONE);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onItemClick(v, item);
                    }
                }
            });
        }
    }

    private final OnItemClickListener itemClickListener;

    public MovieHistoryAdapter(Context context, OnItemClickListener itemClickListener) {
        super(context);
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        // A ViewHolder keeps references to child views to avoid
        // unnecessary calls to findViewById() on each row.
        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_history, parent, false);
            holder = new ViewHolder(convertView, itemClickListener);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        HistoryEntry item = getItem(position);
        if (item == null) {
            return convertView; // all bets are off!
        }
        holder.item = item;

        // movie title
        holder.show.setText(item.movie == null ? null : item.movie.title);

        String posterUrl;
        if (item.movie != null && item.movie.ids != null && item.movie.ids.tmdb != null) {
            // TMDb poster (resolved on demand as trakt does not have them)
            posterUrl = "movietmdb://" + item.movie.ids.tmdb;
        } else {
            posterUrl = null; // no poster
        }
        TvdbImageTools.loadShowPosterResizeSmallCrop(getContext(), holder.poster, posterUrl);

        // timestamp
        if (item.watched_at != null) {
            CharSequence timestamp = DateUtils.getRelativeTimeSpanString(
                    item.watched_at.toInstant().toEpochMilli(), System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL);
            holder.info.setText(timestamp);
        } else {
            holder.info.setText(null);
        }

        // action type indicator
        if ("watch".equals(item.action)) {
            // marked watched
            holder.type.setImageDrawable(getDrawableWatched());
        } else {
            // check-in, scrobble
            holder.type.setImageDrawable(getDrawableCheckin());
        }

        return convertView;
    }
}
