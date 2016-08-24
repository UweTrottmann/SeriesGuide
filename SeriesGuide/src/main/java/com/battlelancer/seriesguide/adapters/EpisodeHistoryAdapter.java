package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.trakt5.entities.HistoryEntry;

/**
 * Creates a list of episodes from a list of trakt {@link com.uwetrottmann.trakt.v2.entities.HistoryEntry}.
 */
public class EpisodeHistoryAdapter extends SectionedHistoryAdapter {

    public EpisodeHistoryAdapter(Context context) {
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
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        HistoryEntry item = getItem(position);

        // show title
        holder.title.setText(item.show == null ? null : item.show.title);
        // show poster
        String poster =
                (item.show == null || item.show.images == null || item.show.images.poster == null)
                        ? null : item.show.images.poster.thumb;
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

        // episode
        if (item.episode != null && item.episode.season != null && item.episode.number != null) {
            holder.description.setText(TextTools.getNextEpisodeString(getContext(), item.episode.season,
                    item.episode.number, item.episode.title));
        } else {
            holder.description.setText(null);
        }

        return convertView;
    }
}
