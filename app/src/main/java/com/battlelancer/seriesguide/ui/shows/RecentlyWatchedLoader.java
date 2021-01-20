package com.battlelancer.seriesguide.ui.shows;

import android.content.Context;
import android.database.Cursor;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.model.SgActivity;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
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
        List<SgActivity> activityByLatest = SgRoomDatabase.getInstance(getContext())
                .sgActivityHelper()
                .getActivityByLatest();

        List<NowAdapter.NowItem> items = new ArrayList<>();
        for (SgActivity activity : activityByLatest) {
            if (items.size() == 50) {
                break; // take at most 50 items
            }

            long timestamp = activity.getTimestampMs();
            int episodeTvdbId = Integer.parseInt(activity.getEpisodeTvdbOrTmdbId());

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
                            Shows.POSTER_SMALL // 7
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
                        TvdbImageTools.artworkUrl(episodeQuery.getString(7))
                ).tvdbIds(episodeTvdbId, episodeQuery.getInt(5)).recentlyWatchedLocal();
                items.add(item);
            }

            episodeQuery.close();
        }

        // add header
        if (items.size() > 0) {
            items.add(0, new NowAdapter.NowItem().header(
                    getContext().getString(R.string.recently_watched)));
        }

        return items;
    }
}
