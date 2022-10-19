package com.battlelancer.seriesguide.movies;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.battlelancer.seriesguide.shows.history.HistoryViewHolder;
import com.battlelancer.seriesguide.shows.history.NowAdapter;

/**
 * An adapted version of {@link NowAdapter} with a special layout for movies.
 */
class MoviesNowAdapter extends NowAdapter {

    MoviesNowAdapter(Context context, ItemClickListener listener) {
        super(context, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof HistoryViewHolder) {
            NowItem item = getItem(position);
            HistoryViewHolder holder = (HistoryViewHolder) viewHolder;
            holder.bindToMovie(getContext(), item, getDrawableWatched(), getDrawableCheckin());
        } else {
            super.onBindViewHolder(viewHolder, position);
        }
    }
}
