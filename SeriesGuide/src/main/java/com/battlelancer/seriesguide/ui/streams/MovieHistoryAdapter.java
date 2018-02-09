package com.battlelancer.seriesguide.ui.streams;

import android.content.Context;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
import com.uwetrottmann.trakt5.entities.HistoryEntry;

/**
 * Creates a list of movies from a list of {@link HistoryEntry}.
 */
class MovieHistoryAdapter extends SectionedHistoryAdapter {

    MovieHistoryAdapter(Context context, OnItemClickListener itemClickListener) {
        super(context, itemClickListener);
    }

    @Override
    void bindViewHolder(ViewHolder holder, HistoryEntry item) {
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
    }
}
