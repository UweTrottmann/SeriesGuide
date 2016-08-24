package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.trakt5.entities.HistoryEntry;

/**
 * Creates a list of movies from a list of {@link com.uwetrottmann.trakt.v2.entities.HistoryEntry}.
 */
public class MovieHistoryAdapter extends SectionedHistoryAdapter {

    public MovieHistoryAdapter(Context context) {
        super(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // A ViewHolder keeps references to child views to avoid
        // unnecessary calls to findViewById() on each row.
        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_history, parent, false);

            holder = new ViewHolder(convertView);
            // tweak layout for movie
            holder.title.setTextAppearance(convertView.getContext(),
                    R.style.TextAppearance_Subhead);
            holder.title.setMaxLines(2);
            holder.description.setVisibility(View.GONE);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        HistoryEntry item = getItem(position);

        // movie title
        holder.title.setText(item.movie == null ? null : item.movie.title);
        // movie poster
        String poster =
                (item.movie == null || item.movie.images == null
                        || item.movie.images.poster == null)
                        ? null : item.movie.images.poster.thumb;
        Utils.loadSmallPoster(getContext(), holder.poster, poster);

        // timestamp
        if (item.watched_at != null) {
            CharSequence timestamp = DateUtils.getRelativeTimeSpanString(
                    item.watched_at.getMillis(), System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL);
            holder.timestamp.setText(timestamp);
        } else {
            holder.timestamp.setText(null);
        }

        // action type indicator
        if ("watch".equals(item.action)) {
            // marked watched
            holder.type.setImageResource(getResIdDrawableWatched());
        } else {
            // check-in, scrobble
            holder.type.setImageResource(getResIdDrawableCheckin());
        }

        return convertView;
    }
}
