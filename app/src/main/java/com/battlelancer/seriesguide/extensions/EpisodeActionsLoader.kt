package com.battlelancer.seriesguide.extensions

import android.content.Context
import com.battlelancer.seriesguide.api.Action
import com.battlelancer.seriesguide.api.Episode
import com.battlelancer.seriesguide.provider.SgRoomDatabase.Companion.getInstance
import com.battlelancer.seriesguide.util.TextTools
import com.uwetrottmann.androidutils.GenericSimpleLoader
import java.util.ArrayList

/**
 * Tries returning existing actions for an episode. If no actions have been published, will ask
 * extensions to do so and returns an empty list.
 */
class EpisodeActionsLoader(
    context: Context,
    private val episodeId: Long
) : GenericSimpleLoader<MutableList<Action>>(context) {

    override fun loadInBackground(): MutableList<Action> {
        val database = getInstance(context)
        val episode = database.sgEpisode2Helper().getEpisode(episodeId)
            ?: return ArrayList()
        val episodeTmdbId = episode.tmdbId
            ?: return ArrayList()

        var actions = ExtensionManager.get(context)
            .getLatestEpisodeActions(context, episodeTmdbId)

        if (actions == null || actions.size == 0) {
            // no actions available yet, request extensions to publish them
            actions = ArrayList()

            val show = database.sgShow2Helper().getShow(episode.showId)
                ?: return actions
            if (show.tmdbId == null) {
                return actions
            }

            val number = episode.number
            val data = Episode.Builder()
                .tmdbId(episodeTmdbId)
                .tvdbId(episode.tvdbId ?: 0)
                .title(TextTools.getEpisodeTitle(context, episode.title, number))
                .number(number)
                .numberAbsolute(episode.absoluteNumber ?: 0)
                .season(episode.season)
                .imdbId(episode.imdbId)
                .showTmdbId(show.tmdbId)
                .showTvdbId(show.tvdbId ?: 0)
                .showTitle(show.title)
                .showImdbId(show.imdbId)
                .showFirstReleaseDate(show.firstRelease)
                .build()
            ExtensionManager.get(context).requestEpisodeActions(context, data)
        }
        return actions
    }
}