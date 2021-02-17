package com.battlelancer.seriesguide.ui.shows;

import android.content.Context;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.model.SgActivity;
import com.battlelancer.seriesguide.provider.SgEpisode2Helper;
import com.battlelancer.seriesguide.provider.SgEpisode2WithShow;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.util.ImageTools;
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
        SgRoomDatabase database = SgRoomDatabase.getInstance(getContext());
        List<SgActivity> activityByLatest = database
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
            SgEpisode2Helper helper = database.sgEpisode2Helper();
            long episodeId = helper.getEpisodeId(episodeTvdbId);
            SgEpisode2WithShow episode = helper.getEpisodeWithShow(episodeId);
            if (episode == null) {
                continue;
            }

            // add items
            NowAdapter.NowItem item = new NowAdapter.NowItem()
                    .displayData(
                            timestamp,
                            episode.getSeriestitle(),
                            TextTools.getNextEpisodeString(getContext(),
                                    episode.getSeason(),
                                    episode.getEpisodenumber(),
                                    episode.getEpisodetitle()),
                            ImageTools.tmdbOrTvdbPosterUrl(episode.getSeries_poster_small(),
                                    getContext(), false))
                    .tvdbIds(episodeTvdbId, episode.getShowTvdbId()).recentlyWatchedLocal();
            items.add(item);
        }

        // add header
        if (items.size() > 0) {
            items.add(0, new NowAdapter.NowItem().header(
                    getContext().getString(R.string.recently_watched)));
        }

        return items;
    }
}
