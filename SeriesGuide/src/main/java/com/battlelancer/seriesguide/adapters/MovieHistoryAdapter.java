package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.uwetrottmann.trakt5.entities.HistoryEntry;

/**
 * Creates a list of movies from a list of {@link HistoryEntry}.
 */
public class MovieHistoryAdapter extends SectionedHistoryAdapter {

    public static class ViewHolder {

        TextView title;
        TextView timestamp;
        ImageView type;

        public ViewHolder(View view) {
            title = (TextView) view.findViewById(R.id.textViewHistoryTitle);
            timestamp = (TextView) view.findViewById(R.id.textViewHistoryTimestamp);
            type = (ImageView) view.findViewById(R.id.imageViewHistoryType);
        }
    }

    public MovieHistoryAdapter(Context context) {
        super(context);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        // A ViewHolder keeps references to child views to avoid
        // unnecessary calls to findViewById() on each row.
        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_history_movie, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        HistoryEntry item = getItem(position);
        if (item == null) {
            return convertView; // all bets are off!
        }

        // movie title
        holder.title.setText(item.movie == null ? null : item.movie.title);
        // movie poster
        // trakt has removed images: currently displaying no poster
//        Utils.loadSmallPoster(getContext(), holder.poster, poster);

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
