package com.battlelancer.seriesguide.ui.movies;

import android.content.Context;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
import com.battlelancer.seriesguide.ui.shows.NowAdapter;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.SgPicassoRequestHandler;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
import java.util.Date;

/**
 * An adapted version of {@link NowAdapter} with a special layout for movies.
 */
class MoviesNowAdapter extends NowAdapter {

    MoviesNowAdapter(Context context, ItemClickListener listener) {
        super(context, listener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof HistoryViewHolder) {
            NowItem item = getItem(position);
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

            // TMDb poster (resolved on demand as trakt does not have them)
            TvdbImageTools.loadShowPosterResizeSmallCrop(getContext(), holder.poster,
                    SgPicassoRequestHandler.SCHEME_MOVIE_TMDB + "://" + item.movieTmdbId);

            holder.show.setText(item.title);

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
            // Set disabled for darker icon (non-interactive).
            holder.type.setEnabled(false);
        } else {
            super.onBindViewHolder(viewHolder, position);
        }
    }
}
