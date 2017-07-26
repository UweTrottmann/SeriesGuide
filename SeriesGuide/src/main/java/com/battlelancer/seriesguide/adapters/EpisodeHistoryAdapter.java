package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.support.v4.util.SparseArrayCompat;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.TextTools;
import com.uwetrottmann.trakt5.entities.HistoryEntry;
import java.util.List;

/**
 * Creates a list of episodes from a list of trakt {@link HistoryEntry} objects.
 */
public class EpisodeHistoryAdapter extends SectionedHistoryAdapter {

    private SparseArrayCompat<String> localShowPosters;

    public EpisodeHistoryAdapter(Context context, OnItemClickListener itemClickListener) {
        super(context, itemClickListener);
    }

    @Override
    public void setData(List<HistoryEntry> data) {
        super.setData(data);
        localShowPosters = ShowTools.getShowTvdbIdsAndPosters(getContext());
    }

    @Override
    void bindViewHolder(ViewHolder holder, HistoryEntry item) {
        // show title
        holder.show.setText(item.show == null ? null : item.show.title);
        // show poster, use a TVDB one
        String posterUrl;
        Integer showTvdbId = (item.show == null || item.show.ids == null)
                ? null : item.show.ids.tvdb;
        if (localShowPosters != null && showTvdbId != null) {
            // prefer poster of already added show, fall back to first uploaded poster
            posterUrl = TvdbImageTools.smallSizeOrResolveUrl(localShowPosters.get(showTvdbId),
                    showTvdbId);
        } else {
            posterUrl = null;
        }
        TvdbImageTools.loadShowPosterResizeSmallCrop(getContext(), holder.poster, posterUrl);

        // episode
        if (item.episode != null && item.episode.season != null && item.episode.number != null) {
            holder.episode.setText(TextTools.getNextEpisodeString(getContext(), item.episode.season,
                    item.episode.number, item.episode.title));
        } else {
            holder.episode.setText(null);
        }

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
