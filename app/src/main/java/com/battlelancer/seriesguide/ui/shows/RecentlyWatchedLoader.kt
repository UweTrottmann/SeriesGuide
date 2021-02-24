package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.model.ActivityType
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.ui.shows.NowAdapter.NowItem
import com.battlelancer.seriesguide.util.ImageTools.tmdbOrTvdbPosterUrl
import com.battlelancer.seriesguide.util.TextTools
import com.uwetrottmann.androidutils.GenericSimpleLoader
import timber.log.Timber

/**
 * Loads a list of recently watched episodes from the local activity table.
 */
class RecentlyWatchedLoader(
    context: Context
) : GenericSimpleLoader<MutableList<NowItem>>(context) {

    override fun loadInBackground(): MutableList<NowItem> {
        // get all activity with the latest one first
        val database = SgRoomDatabase.getInstance(context)
        val activityByLatest = database
            .sgActivityHelper()
            .getActivityByLatest()

        val items = mutableListOf<NowItem>()
        for ((_, episodeStableId, _, timestamp, type) in activityByLatest) {
            if (items.size == 50) {
                break // take at most 50 items
            }

            val episodeTmdbOrTvdbId = episodeStableId.toInt()

            // get episode details
            val helper = database.sgEpisode2Helper()
            val episodeId = if (type == ActivityType.TMDB_ID) {
                helper.getEpisodeIdByTmdbId(episodeTmdbOrTvdbId)
            } else if (type == ActivityType.TVDB_ID) {
                helper.getEpisodeIdByTvdbId(episodeTmdbOrTvdbId)
            } else {
                Timber.e("Unknown activity type %d", type)
                continue
            }
            val episode = helper.getEpisodeWithShow(episodeId)
                ?: continue

            // add items
            val item = NowItem()
                .displayData(
                    timestamp,
                    episode.seriestitle,
                    TextTools.getNextEpisodeString(
                        context,
                        episode.season,
                        episode.episodenumber,
                        episode.episodetitle
                    ),
                    tmdbOrTvdbPosterUrl(
                        episode.series_poster_small,
                        context, false
                    )
                )
                // No need to add ID for adding show, query only returns if show is added.
                .episodeIds(episodeId, 0).recentlyWatchedLocal()
            items.add(item)
        }

        // add header
        if (items.size > 0) {
            items.add(0, NowItem().header(context.getString(R.string.recently_watched)))
        }

        return items
    }
}