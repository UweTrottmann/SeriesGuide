package com.battlelancer.seriesguide.ui.shows;

import android.content.Context;
import android.database.Cursor;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Activity;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
import com.battlelancer.seriesguide.util.TextTools;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads a list of recently watched episodes.
 */
class RecentlyWatchedLoader extends GenericSimpleLoader<List<NowAdapter.NowItem>> {

    RecentlyWatchedLoader(Context context) {
        super(context);
    }

    @Override
    public List<NowAdapter.NowItem> loadInBackground() {
        // get all activity with the latest one first
        Cursor query = getContext().getContentResolver()
                .query(Activity.CONTENT_URI,
                        new String[] { Activity.TIMESTAMP_MS, Activity.EPISODE_TVDB_ID },
                        null, null, Activity.SORT_LATEST);
        if (query == null) {
            return null;
        }

        List<NowAdapter.NowItem> items = new ArrayList<>();
        while (query.moveToNext()) {
            if (items.size() == 50) {
                break; // take at most 50 items
            }

            long timestamp = query.getLong(0);
            int episodeTvdbId = query.getInt(1);

            // get episode details
            Cursor episodeQuery = getContext().getContentResolver().query(
                    Episodes.buildEpisodeWithShowUri(episodeTvdbId),
                    new String[] {
                            SeriesGuideDatabase.Tables.EPISODES + "."
                                    + Episodes._ID, // 0
                            Episodes.TITLE,
                            Episodes.NUMBER,
                            Episodes.SEASON, // 3
                            Episodes.FIRSTAIREDMS,
                            Shows.REF_SHOW_ID,
                            Shows.TITLE,
                            Shows.POSTER // 7
                    }, null, null, null);
            if (episodeQuery == null) {
                continue;
            }

            // add items
            if (episodeQuery.moveToFirst()) {
                NowAdapter.NowItem item = new NowAdapter.NowItem().displayData(
                        timestamp,
                        episodeQuery.getString(6),
                        TextTools.getNextEpisodeString(getContext(), episodeQuery.getInt(3),
                                episodeQuery.getInt(2), episodeQuery.getString(1)),
                        TvdbImageTools.smallSizeUrl(episodeQuery.getString(7))
                ).tvdbIds(episodeTvdbId, episodeQuery.getInt(5)).recentlyWatchedLocal();
                items.add(item);
            }

            episodeQuery.close();
        }

        query.close();

        // add header
        if (items.size() > 0) {
            items.add(0, new NowAdapter.NowItem().header(
                    getContext().getString(R.string.recently_watched)));
        }

        return items;
    }
}
